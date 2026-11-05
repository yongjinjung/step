import { Alert, App, Col, Descriptions, Divider, Form, Input, Modal, Radio, Row, Typography } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyFieldErrors, errorMessage } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { DispensingSelect } from '@/features/pharmacy/PharmacySelects'
import { INSPECTION_RESULTS, formatQuantity, optionsOf, trimToUndefined } from '@/features/pharmacy/pharmacyFields'
import type { InspectionRequest } from './inspectionFields'

interface InspectionFormModalProps {
    open: boolean
    /** 검수할 조제. 조제 상세에서 열면 정해져 있고, 검수 목록에서 열면 null(모달에서 고른다) */
    dispensingId: number | null
    onClose: () => void
}

const RESULT_OPTIONS = optionsOf(INSPECTION_RESULTS)

/**
 * 검수 등록 모달 (POST /api/inspections).
 *
 * 검수 대기(DISPENSED) 조제를 고르면 조제 품목마다 한 줄을 만들고 품목별로 합격/불합격을 고른다(처음 값은 합격).
 * 불합격 품목은 불량 사유가 필수다(서버 DEFECT_REASON_REQUIRED와 같은 규칙을 화면에서도 검사한다).
 * 전 품목 합격이면 조제가 검수 합격(INSPECTED, 불출 가능), 하나라도 불합격이면 검수 불합격(REJECTED, 재조제)이 된다.
 */
export function InspectionFormModal({ open, dispensingId, onClose }: InspectionFormModalProps) {
    const { message } = App.useApp()
    const invalidate = useInvalidateApi()
    const [form] = Form.useForm<InspectionRequest>()
    const createInspection = $api.useMutation('post', '/api/inspections')

    const selectedId = Form.useWatch('dispensingId', form)
    // 품목별 결과를 지켜본다: 불량 사유 칸 켜기/끄기와 전체 결과 미리보기에 쓴다.
    const watchedItems = Form.useWatch('items', form)

    const dispensingQuery = $api.useQuery(
        'get',
        '/api/dispensings/{id}',
        { params: { path: { id: selectedId ?? 0 } } },
        { enabled: open && selectedId !== undefined },
    )
    const dispensing = dispensingQuery.data
    const sourceItems = dispensing?.items ?? []
    const inspectable = dispensing?.status === 'DISPENSED'

    useEffect(() => {
        if (!open) {
            return
        }
        form.resetFields()
        form.setFieldsValue({ dispensingId: dispensingId ?? undefined })
    }, [open, dispensingId, form])

    // 조제가 오면 품목마다 검수 줄을 만든다(처음 값: 합격).
    useEffect(() => {
        if (!open || !dispensing) {
            return
        }
        form.setFieldValue(
            'items',
            (dispensing.items ?? []).map((item) => ({ dispensingItemId: item.id, result: 'PASS' })),
        )
    }, [open, dispensing, form])

    const failCount = (watchedItems ?? []).filter((item) => item?.result === 'FAIL').length

    const handleFinish = (values: InspectionRequest) => {
        const body: InspectionRequest = {
            ...values,
            remark: trimToUndefined(values.remark),
            // 합격 품목의 불량 사유는 보내지 않는다(불합격으로 골랐다가 되돌린 경우).
            items: values.items.map((item) => ({
                ...item,
                defectReason: item.result === 'FAIL' ? trimToUndefined(item.defectReason) : undefined,
            })),
        }
        createInspection.mutate(
            { body },
            {
                onSuccess: (created) => {
                    message.success(
                        created.result === 'PASS'
                            ? `처방 ${created.prescriptionNo} 검수 합격 — 불출할 수 있습니다.`
                            : `처방 ${created.prescriptionNo} 검수 불합격 — 재조제가 필요합니다.`,
                    )
                    // 검수 목록·상세와 조제 상태(DISPENSED → INSPECTED/REJECTED)를 함께 새로 고친다.
                    invalidate('/api/inspections', '/api/inspections/{id}', '/api/dispensings', '/api/dispensings/{id}')
                    onClose()
                },
                onError: (error: unknown) => {
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
            title="검수 등록"
            okText="검수 완료"
            okButtonProps={{ disabled: !inspectable }}
            confirmLoading={createInspection.isPending}
            onOk={() => form.submit()}
            onCancel={onClose}
            forceRender
            width={960}
        >
            <Form form={form} layout="vertical" onFinish={handleFinish}>
                <Form.Item name="dispensingId" label="조제 (검수 대기)" rules={[{ required: true, message: '조제를 선택하세요.' }]}>
                    <DispensingSelect status="DISPENSED" disabled={dispensingId !== null} />
                </Form.Item>

                {dispensingQuery.isError && <Alert type="error" showIcon title={errorMessage(dispensingQuery.error)} />}
                {dispensing && (
                    <Descriptions column={3} bordered size="small" className="page-notice">
                        <Descriptions.Item label="환자">
                            {dispensing.patientName} ({dispensing.patientNo})
                        </Descriptions.Item>
                        <Descriptions.Item label="조제자">{dispensing.dispensedBy}</Descriptions.Item>
                        <Descriptions.Item label="품목">{sourceItems.length}건</Descriptions.Item>
                    </Descriptions>
                )}
                {dispensing && !inspectable && (
                    <Alert type="warning" showIcon className="page-notice" title="검수를 기다리는 조제가 아닙니다." />
                )}

                <Form.Item name="remark" label="비고" rules={[{ max: 1000 }]}>
                    <Input.TextArea rows={2} />
                </Form.Item>

                <Divider titlePlacement="start" plain>
                    검수 품목 (조제 품목마다 한 줄)
                </Divider>
                <Form.List name="items">
                    {(fields) =>
                        fields.length === 0 ? (
                            <Typography.Text type="secondary">조제를 고르면 품목이 나옵니다.</Typography.Text>
                        ) : (
                            <>
                                <Row gutter={8} className="item-header">
                                    <Col flex="32px" />
                                    <Col span={7}>약품</Col>
                                    <Col span={3}>조제량</Col>
                                    <Col span={4}>제조번호</Col>
                                    <Col span={4}>결과</Col>
                                    <Col flex="auto">불량 사유</Col>
                                </Row>
                                {fields.map((field, index) => {
                                    const source = sourceItems[index]
                                    const failed = watchedItems?.[index]?.result === 'FAIL'
                                    return (
                                        <Row key={field.key} gutter={8} align="top">
                                            <Col flex="32px">
                                                <Typography.Text type="secondary" className="item-line-no">
                                                    {source?.lineNo ?? index + 1}
                                                </Typography.Text>
                                            </Col>
                                            <Col span={7}>
                                                <Form.Item name={[field.name, 'dispensingItemId']} hidden>
                                                    <Input />
                                                </Form.Item>
                                                <div className="item-source">
                                                    <span className="mono">{source?.drugCode}</span> {source?.drugName}
                                                </div>
                                            </Col>
                                            <Col span={3}>
                                                <div className="item-source">{formatQuantity(source?.quantity)}</div>
                                            </Col>
                                            <Col span={4}>
                                                <div className="item-source mono">{source?.lotNo ?? '-'}</div>
                                            </Col>
                                            <Col span={4}>
                                                <Form.Item name={[field.name, 'result']} rules={[{ required: true }]}>
                                                    <Radio.Group options={RESULT_OPTIONS} optionType="button" buttonStyle="solid" />
                                                </Form.Item>
                                            </Col>
                                            <Col flex="auto">
                                                <Form.Item
                                                    name={[field.name, 'defectReason']}
                                                    // 결과가 바뀌면 이 칸도 다시 검사한다(불합격 → 합격으로 바꾸면 오류가 사라진다).
                                                    dependencies={[['items', field.name, 'result']]}
                                                    rules={[
                                                        { max: 500 },
                                                        ({ getFieldValue }) => ({
                                                            validator: async (_, value?: string) => {
                                                                const result = getFieldValue(['items', field.name, 'result'])
                                                                if (result === 'FAIL' && !value?.trim()) {
                                                                    throw new Error('불합격 품목은 불량 사유를 입력하세요.')
                                                                }
                                                            },
                                                        }),
                                                    ]}
                                                >
                                                    <Input disabled={!failed} placeholder={failed ? '예: 수량 부족, 파손' : '합격 품목은 입력하지 않습니다'} />
                                                </Form.Item>
                                            </Col>
                                        </Row>
                                    )
                                })}
                                {failCount === 0 ? (
                                    <Alert type="success" showIcon title="전 품목 합격 → 검수 합격(불출 가능)으로 저장됩니다." />
                                ) : (
                                    <Alert
                                        type="error"
                                        showIcon
                                        title={`불합격 ${failCount}건 → 검수 불합격으로 저장되고, 같은 처방으로 재조제해야 합니다.`}
                                    />
                                )}
                            </>
                        )
                    }
                </Form.List>
            </Form>
        </Modal>
    )
}
