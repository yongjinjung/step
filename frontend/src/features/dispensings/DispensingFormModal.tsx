import { Alert, App, Col, Descriptions, Divider, Form, Input, InputNumber, Modal, Row, Typography } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyFieldErrors, errorMessage } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { PrescriptionSelect } from '@/features/pharmacy/PharmacySelects'
import { codeTag } from '@/features/pharmacy/pharmacyParts'
import { formatQuantity, trimToUndefined } from '@/features/pharmacy/pharmacyFields'
import { PRESCRIPTION_STATUSES, PRESCRIPTION_TYPES } from '@/features/prescriptions/prescriptionFields'
import type { DispensingRequest } from './dispensingFields'

interface DispensingFormModalProps {
    open: boolean
    /** 조제할 처방. 처방 상세·재조제에서 열면 정해져 있고, 조제 목록에서 열면 null(모달에서 고른다) */
    prescriptionId: number | null
    onClose: () => void
}

/**
 * 조제 등록 모달 (POST /api/dispensings).
 *
 * 처방을 고르면 그 처방의 품목을 불러와 품목마다 한 줄씩 만든다. 조제 품목은 처방 품목을 빠짐없이 한 번씩
 * 다뤄야 하므로(서버 INVALID_DISPENSING_ITEMS) 줄을 더하거나 뺄 수 없고, 수량·제조번호·유효기간만 입력한다.
 * 조제 수량은 처방 총량으로 채워 둔다. 조제하면 처방이 DISPENSED(조제됨)가 된다.
 */
export function DispensingFormModal({ open, prescriptionId, onClose }: DispensingFormModalProps) {
    const { message } = App.useApp()
    const invalidate = useInvalidateApi()
    const [form] = Form.useForm<DispensingRequest>()
    const createDispensing = $api.useMutation('post', '/api/dispensings')

    // 폼의 처방 선택 값을 지켜본다. 바뀔 때마다 컴포넌트가 다시 렌더링되고 아래 조회의 키가 바뀐다.
    const selectedId = Form.useWatch('prescriptionId', form)
    const prescriptionQuery = $api.useQuery(
        'get',
        '/api/prescriptions/{id}',
        { params: { path: { id: selectedId ?? 0 } } },
        { enabled: open && selectedId !== undefined },
    )
    const prescription = prescriptionQuery.data
    const sourceItems = prescription?.items ?? []

    // 열 때마다 빈 폼으로 시작한다. 처방이 정해져 있으면 넣어 둔다.
    useEffect(() => {
        if (!open) {
            return
        }
        form.resetFields()
        form.setFieldsValue({ prescriptionId: prescriptionId ?? undefined })
    }, [open, prescriptionId, form])

    // 처방(품목)이 오면 품목마다 조제 줄을 만든다. 처방을 바꾸면 다시 만든다.
    useEffect(() => {
        if (!open || !prescription) {
            return
        }
        form.setFieldValue(
            'items',
            (prescription.items ?? []).map((item) => ({ prescriptionItemId: item.id, quantity: item.totalQuantity })),
        )
    }, [open, prescription, form])

    const handleFinish = (values: DispensingRequest) => {
        const body: DispensingRequest = {
            ...values,
            remark: trimToUndefined(values.remark),
            items: values.items.map((item) => ({
                ...item,
                lotNo: trimToUndefined(item.lotNo),
                expiryDate: item.expiryDate || undefined,
            })),
        }
        createDispensing.mutate(
            { body },
            {
                onSuccess: (created) => {
                    message.success(`처방 ${created.prescriptionNo}을(를) 조제했습니다.`)
                    // 조제 목록·상세와 처방 상태(ORDERED → DISPENSED)를 함께 새로 고친다.
                    invalidate('/api/dispensings', '/api/dispensings/{id}', '/api/prescriptions', '/api/prescriptions/{id}')
                    onClose()
                },
                onError: (error: unknown) => {
                    // 형식 오류는 해당 칸, 업무 오류(취소된 처방, 이미 조제됨 등)는 상단 메시지
                    if (!applyFieldErrors(form, error)) {
                        message.error(errorMessage(error))
                    }
                },
            },
        )
    }

    return (
        <Modal
            open={open}
            title="조제 등록"
            okText="조제"
            okButtonProps={{ disabled: !prescription || prescription.status === 'CANCELLED' }}
            confirmLoading={createDispensing.isPending}
            onOk={() => form.submit()}
            onCancel={onClose}
            forceRender
            width={960}
        >
            <Form form={form} layout="vertical" onFinish={handleFinish}>
                <Form.Item name="prescriptionId" label="처방" rules={[{ required: true, message: '처방을 선택하세요.' }]}>
                    <PrescriptionSelect disabled={prescriptionId !== null} />
                </Form.Item>

                {prescriptionQuery.isError && <Alert type="error" showIcon title={errorMessage(prescriptionQuery.error)} />}
                {prescription && (
                    <Descriptions column={3} bordered size="small" className="page-notice">
                        <Descriptions.Item label="환자">
                            {prescription.patientName} ({prescription.patientNo})
                        </Descriptions.Item>
                        <Descriptions.Item label="구분">{codeTag(PRESCRIPTION_TYPES, prescription.prescriptionType)}</Descriptions.Item>
                        <Descriptions.Item label="처방 상태">{codeTag(PRESCRIPTION_STATUSES, prescription.status)}</Descriptions.Item>
                    </Descriptions>
                )}
                {prescription?.status === 'CANCELLED' && (
                    <Alert type="warning" showIcon className="page-notice" title="취소된 처방은 조제할 수 없습니다." />
                )}

                <Form.Item name="remark" label="비고" rules={[{ max: 1000 }]}>
                    <Input.TextArea rows={2} />
                </Form.Item>

                <Divider titlePlacement="start" plain>
                    조제 품목 (처방 품목마다 한 줄)
                </Divider>
                <Form.List name="items">
                    {(fields) =>
                        fields.length === 0 ? (
                            <Typography.Text type="secondary">처방을 고르면 품목이 나옵니다.</Typography.Text>
                        ) : (
                            <>
                                <Row gutter={8} className="item-header">
                                    <Col flex="32px" />
                                    <Col span={7}>약품</Col>
                                    <Col span={3}>처방량</Col>
                                    <Col span={4}>조제량</Col>
                                    <Col span={5}>제조번호</Col>
                                    <Col flex="auto">유효기간</Col>
                                </Row>
                                {fields.map((field, index) => {
                                    // 줄은 처방 품목 순서대로 만들었으므로 같은 위치의 처방 품목이 그 줄의 대상이다.
                                    const source = sourceItems[index]
                                    return (
                                        <Row key={field.key} gutter={8} align="top">
                                            <Col flex="32px">
                                                <Typography.Text type="secondary" className="item-line-no">
                                                    {source?.lineNo ?? index + 1}
                                                </Typography.Text>
                                            </Col>
                                            <Col span={7}>
                                                {/* 보이지 않는 칸: 요청에 넣을 처방 품목 ID */}
                                                <Form.Item name={[field.name, 'prescriptionItemId']} hidden>
                                                    <Input />
                                                </Form.Item>
                                                <div className="item-source">
                                                    <span className="mono">{source?.drugCode}</span> {source?.drugName}
                                                </div>
                                            </Col>
                                            <Col span={3}>
                                                <div className="item-source">{formatQuantity(source?.totalQuantity)}</div>
                                            </Col>
                                            <Col span={4}>
                                                <Form.Item name={[field.name, 'quantity']} rules={[{ required: true, message: '조제량' }]}>
                                                    <InputNumber min={0.001} className="full-width" />
                                                </Form.Item>
                                            </Col>
                                            <Col span={5}>
                                                <Form.Item name={[field.name, 'lotNo']} rules={[{ max: 50 }]}>
                                                    <Input placeholder="제조번호 (선택)" className="mono" />
                                                </Form.Item>
                                            </Col>
                                            <Col flex="auto">
                                                <Form.Item name={[field.name, 'expiryDate']}>
                                                    <Input type="date" aria-label={`${index + 1}번 품목 유효기간`} />
                                                </Form.Item>
                                            </Col>
                                        </Row>
                                    )
                                })}
                            </>
                        )
                    }
                </Form.List>
            </Form>
        </Modal>
    )
}
