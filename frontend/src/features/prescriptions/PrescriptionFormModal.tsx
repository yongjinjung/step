import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { App, Button, Col, Divider, Form, Input, InputNumber, Modal, Row, Select, Typography } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyCodeError, applyFieldErrors, errorMessage } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import {
    PRESCRIPTION_ERROR_FIELDS,
    PRESCRIPTION_TYPE_OPTIONS,
    nowForDateTimeInput,
    type PrescriptionItemRequest,
    type PrescriptionRequest,
} from './prescriptionFields'

interface PrescriptionFormModalProps {
    open: boolean
    onClose: () => void
}

/** 품목 한 줄의 처음 값. 1회량 일수는 자주 쓰는 값으로 채워 둔다. */
const NEW_ITEM: Partial<PrescriptionItemRequest> = {dose:1, days:1};

/**
 * 처방 등록 모달. 처방 정보와 품목(1~100줄)을 한 번에 저장한다. (POST /api/prescriptions).
 *
 * 처방은 수정하지 않는다.(바꾸려면 취소하고 새로 처방). 그래서 등록 전용이다.
 * 코드성 값(진료과 • 약품코드 • 단위 • 경로 • 은 서버가 대문자로 저장하므로 화면에서는 그대로 보낸다.
 */

export function PrescriptionFormModal({ open, onClose }: PrescriptionFormModalProps) {
    const { message } = App.useApp()
    const invalidate = useInvalidateApi()
    const [form] = Form.useForm<PrescriptionRequest>()
    const createPrescription = $api.useMutation('post', '/api/prescriptions')

    // 열 때마다 빈 폼 + 기본값(외래, 지금 시각, 품목 한 줄)으로 시작한다.
    useEffect(() => {
        if (!open) {
            return
        }
        form.resetFields()
        form.setFieldsValue({
            prescriptionType: 'OUTPATIENT',
            prescribedAt: nowForDateTimeInput(),
            items: [NEW_ITEM as PrescriptionItemRequest],
        })
    }, [open, form])


    const handleFinish = (values: PrescriptionRequest) => {
        const body: PrescriptionRequest = {
            ...values,
            // datetime-local은 초가 없다(2026-11-04T09:30). 서버 LocalDateTime 형식에 맞춰 초를 붙인다.
            prescribedAt: values.prescribedAt.length === 16 ? `${values.prescribedAt}:00` : values.prescribedAt,
            remark: values.remark?.trim() || undefined,
            items: values.items.map((item) => ({ ...item, usageNote: item.usageNote?.trim() || undefined })),
        }
        createPrescription.mutate(
            { body },
            {
                onSuccess: (created) => {
                    message.success(`처방 ${created.prescriptionNo}을(를) 등록했습니다.`)
                    invalidate('/api/prescriptions')
                    onClose()
                },
                onError: (error: unknown) => {
                    // 1) 중복 같은 업무 오류 → 해당 칸, 2) 형식 오류(fieldErrors) → 해당 칸, 3) 나머지 → 상단 메시지
                    if (applyCodeError(form, error, PRESCRIPTION_ERROR_FIELDS) || applyFieldErrors(form, error)) {
                        return
                    }
                    message.error(errorMessage(error))
                },
            },
        )
    }

    return (
        <Modal
        open={open}
        title="처방 등록"
        okText="저장"
        confirmLoading={createPrescription.isPending}
        onOk={() => form.submit()}
        onCancel={onClose}
        forceRender
        width={960}
        >
            <Form form={form} layout="vertical" onFinish={handleFinish}>
                <Row gutter={12}>
                <Col span={8}>
                    <Form.Item
                        name="prescriptionNo"
                        label="처방전번호"
                        rules={[
                            { required: true, whitespace: true, message: '처방전번호를 입력하세요.' },
                            { max: 30, message: '30자 이하로 입력하세요.' },
                        ]}
                    >
                        <Input placeholder="예: RX20261104-0001" className="mono" />
                    </Form.Item>
                </Col>
                <Col span={8}>
                    <Form.Item name="prescriptionType" label="구분" rules={[{ required: true }]}>
                        <Select options={PRESCRIPTION_TYPE_OPTIONS} />
                    </Form.Item>
                </Col>
                <Col span={8}>
                    <Form.Item name="prescribedAt" label="처방 일시" rules={[{ required: true, message: '처방 일시를 입력하세요.' }]}>
                        <Input type="datetime-local" />
                    </Form.Item>
                </Col>
            </Row>
                <Row gutter={12}>
                    <Col span={6}>
                        <Form.Item
                            name="patientNo"
                            label="환자번호"
                            rules={[{ required: true, whitespace: true, message: '환자번호를 입력하세요.' }, { max: 20 }]}
                        >
                            <Input />
                        </Form.Item>
                    </Col>
                    <Col span={6}>
                        <Form.Item
                            name="patientName"
                            label="환자명"
                            rules={[{ required: true, whitespace: true, message: '환자명을 입력하세요.' }, { max: 100 }]}
                        >
                            <Input />
                        </Form.Item>
                    </Col>
                    <Col span={6}>
                        <Form.Item
                            name="departmentCode"
                            label="진료과 코드"
                            rules={[{ required: true, whitespace: true, message: '진료과를 입력하세요.' }, { max: 20 }]}
                        >
                            <Input placeholder="예: IM" />
                        </Form.Item>
                    </Col>
                    <Col span={6}>
                        <Form.Item
                            name="prescriberName"
                            label="처방의"
                            rules={[{ required: true, whitespace: true, message: '처방의를 입력하세요.' }, { max: 100 }]}
                        >
                            <Input />
                        </Form.Item>
                    </Col>
                </Row>
            <Form.Item name="remark" label="비고" rules={[{ max: 1000 }]}>
                <Input.TextArea rows={2} />
            </Form.Item>
                <Divider titlePlacement="start" plain>
                    처방 품목
                </Divider>
                <Form.List
                    name="items"
                    rules={[
                        {
                            validator: async (_, items?: unknown[]) => {
                                if (!items?.length) {
                                    throw new Error('품목을 한 줄 이상 추가하세요.')
                                }
                                if (items.length > 100) {
                                    throw new Error('품목은 100줄까지 넣을 수 있습니다.')
                                }
                            },
                        },
                    ]}
                >
                    {(fields, { add, remove }, { errors }) => (
                        <>
                            {fields.map((field, index) => (
                                <Row key={field.key} gutter={8} align="top">
                                    <Col flex="32px">
                                        <Typography.Text type="secondary" className="item-line-no">
                                            {index + 1}
                                        </Typography.Text>
                                    </Col>
                                    <Col span={3}>
                                        <Form.Item name={[field.name, 'drugCode']} rules={[{ required: true, whitespace: true, message: '약품코드' }, { max: 30 }]}>
                                            <Input placeholder="약품코드" className="mono" />
                                        </Form.Item>
                                    </Col>
                                    <Col span={5}>
                                        <Form.Item name={[field.name, 'drugName']} rules={[{ required: true, whitespace: true, message: '약품명' }, { max: 200 }]}>
                                            <Input placeholder="약품명" />
                                        </Form.Item>
                                    </Col>
                                    <Col span={2}>
                                        <Form.Item name={[field.name, 'dose']} rules={[{ required: true, message: '1회량' }]}>
                                            <InputNumber placeholder="1회량" min={0.001} step={0.5} className="full-width" />
                                        </Form.Item>
                                    </Col>
                                    <Col span={2}>
                                        <Form.Item name={[field.name, 'doseUnit']} rules={[{ required: true, whitespace: true, message: '단위' }, { max: 20 }]}>
                                            <Input placeholder="단위" />
                                        </Form.Item>
                                    </Col>
                                    <Col span={2}>
                                        <Form.Item name={[field.name, 'route']} rules={[{ required: true, whitespace: true, message: '경로' }, { max: 20 }]}>
                                            <Input placeholder="경로" />
                                        </Form.Item>
                                    </Col>
                                    <Col span={2}>
                                        <Form.Item name={[field.name, 'frequency']} rules={[{ required: true, whitespace: true, message: '빈도' }, { max: 20 }]}>
                                            <Input placeholder="빈도" />
                                        </Form.Item>
                                    </Col>
                                    <Col span={2}>
                                        <Form.Item name={[field.name, 'days']} rules={[{ required: true, message: '일수' }]}>
                                            <InputNumber placeholder="일수" min={1} max={365} precision={0} className="full-width" />
                                        </Form.Item>
                                    </Col>
                                    <Col span={2}>
                                        <Form.Item name={[field.name, 'totalQuantity']} rules={[{ required: true, message: '총량' }]}>
                                            <InputNumber placeholder="총량" min={0.001} className="full-width" />
                                        </Form.Item>
                                    </Col>
                                    <Col flex="auto">
                                        <Form.Item name={[field.name, 'usageNote']} rules={[{ max: 500 }]}>
                                            <Input placeholder="용법 (선택)" />
                                        </Form.Item>
                                    </Col>
                                    <Col flex="32px">
                                        <Button
                                            type="text"
                                            danger
                                            icon={<MinusCircleOutlined />}
                                            aria-label={`${index + 1}번 품목 삭제`}
                                            disabled={fields.length === 1}
                                            onClick={() => remove(field.name)}
                                        />
                                    </Col>
                                </Row>
                            ))}
                            <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add({ ...NEW_ITEM })}>
                                품목 추가
                            </Button>
                            <Form.ErrorList errors={errors} />
                        </>
                    )}
                </Form.List>
            </Form>
        </Modal>
    )
}