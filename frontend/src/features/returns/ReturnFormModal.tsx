import { Alert, App, Col, Descriptions, Divider, Form, Input, InputNumber, Modal, Row, Select, Typography } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { IssueSelect } from '@/features/pharmacy/PharmacySelects'
import {
    RETURN_DISPOSITIONS,
    formatQuantity,
    optionsOf,
    subtractQuantity,
    trimToUndefined,
} from '@/features/pharmacy/pharmacyFields'
import type { DrugReturnRequest, ReturnFormValues } from './returnFields'

interface ReturnFormModalProps {
    open: boolean
    /** 반납할 불출. 불출 상세에서 열면 정해져 있고, 반납 목록에서 열면 null(모달에서 고른다) */
    issueId: number | null
    onClose: () => void
}

const DISPOSITION_OPTIONS = optionsOf(RETURN_DISPOSITIONS)

/**
 * 반납 등록 모달 (POST /api/returns).
 *
 * 불출을 고르면 불출 품목마다 한 줄을 만든다. 반납할 품목만 반납량을 입력하고(일부 품목만 가능),
 * 줄마다 처리 방법(재입고/폐기)을 고른다. 반납량은 남은 수량(불출량 − 이전 반납 합계)까지다.
 * 서버도 같은 한도를 검사한다(RETURN_QUANTITY_EXCEEDED). 같은 불출을 여러 번 나눠 반납할 수 있다.
 */
export function ReturnFormModal({ open, issueId, onClose }: ReturnFormModalProps) {
    const { message } = App.useApp()
    const invalidate = useInvalidateApi()
    const [form] = Form.useForm<ReturnFormValues>()
    const createReturn = $api.useMutation('post', '/api/returns')

    const selectedId = Form.useWatch('issueId', form)
    const issueQuery = $api.useQuery(
        'get',
        '/api/issues/{id}',
        { params: { path: { id: selectedId ?? 0 } } },
        { enabled: open && selectedId !== undefined },
    )
    const issue = issueQuery.data
    const sourceItems = issue?.items ?? []

    useEffect(() => {
        if (!open) {
            return
        }
        form.resetFields()
        form.setFieldsValue({ issueId: issueId ?? undefined })
    }, [open, issueId, form])

    // 불출이 오면 불출 품목마다 반납 줄을 만든다(반납량은 비워 두고, 처리 방법은 재입고).
    useEffect(() => {
        if (!open || !issue) {
            return
        }
        form.setFieldValue(
            'items',
            (issue.items ?? []).map((item) => ({ issueItemId: item.id, quantity: null, disposition: 'RESTOCK' })),
        )
    }, [open, issue, form])

    const handleFinish = (values: ReturnFormValues) => {
        const body: DrugReturnRequest = {
            issueId: values.issueId,
            reason: values.reason.trim(),
            remark: trimToUndefined(values.remark),
            // 반납량을 입력한 줄만 보낸다.
            items: values.items
                .filter((item) => (item.quantity ?? 0) > 0)
                .map((item) => ({ issueItemId: item.issueItemId, quantity: item.quantity as number, disposition: item.disposition })),
        }
        createReturn.mutate(
            { body },
            {
                onSuccess: (created) => {
                    message.success(`처방 ${created.prescriptionNo}의 ${created.items?.length ?? 0}개 품목을 반납했습니다.`)
                    // 반납 목록·상세와 불출 상세의 반납량(returnedQuantity)을 함께 새로 고친다.
                    invalidate('/api/returns', '/api/returns/{id}', '/api/issues', '/api/issues/{id}')
                    onClose()
                },
                onError: (error: unknown) => {
                    // 필드 오류 경로는 요청 기준(items[0] = 보낸 첫 줄)이라 화면 줄 위치와 다를 수 있다.
                    // 그래서 applyFieldErrors로 칸에 붙이지 않고 상단 메시지로만 보여준다(화면 규칙이 같은 검사를 먼저 한다).
                    message.error(errorMessage(error))
                },
            },
        )
    }

    return (
        <Modal
            open={open}
            title="반납 등록"
            okText="반납"
            okButtonProps={{ disabled: !issue }}
            confirmLoading={createReturn.isPending}
            onOk={() => form.submit()}
            onCancel={onClose}
            forceRender
            width={960}
        >
            <Form form={form} layout="vertical" onFinish={handleFinish}>
                <Form.Item name="issueId" label="불출" rules={[{ required: true, message: '불출을 선택하세요.' }]}>
                    <IssueSelect disabled={issueId !== null} />
                </Form.Item>

                {issueQuery.isError && <Alert type="error" showIcon title={errorMessage(issueQuery.error)} />}
                {issue && (
                    <Descriptions column={3} bordered size="small" className="page-notice">
                        <Descriptions.Item label="환자">
                            {issue.patientName} ({issue.patientNo})
                        </Descriptions.Item>
                        <Descriptions.Item label="불출처">{issue.destinationName}</Descriptions.Item>
                        <Descriptions.Item label="수령자">{issue.receiverName}</Descriptions.Item>
                    </Descriptions>
                )}

                <Form.Item
                    name="reason"
                    label="반납 사유"
                    rules={[{ required: true, whitespace: true, message: '반납 사유를 입력하세요.' }, { max: 500 }]}
                >
                    <Input placeholder="예: 퇴원으로 투약 중단" />
                </Form.Item>
                <Form.Item name="remark" label="비고" rules={[{ max: 1000 }]}>
                    <Input.TextArea rows={2} />
                </Form.Item>

                <Divider titlePlacement="start" plain>
                    반납 품목 (반납할 품목만 수량 입력)
                </Divider>
                <Form.List
                    name="items"
                    rules={[
                        {
                            // 줄은 불출 품목 전부라 길이가 아니라 "반납량을 넣은 줄"이 있는지 본다.
                            validator: async (_, items?: ReturnFormValues['items']) => {
                                if (!(items ?? []).some((item) => (item?.quantity ?? 0) > 0)) {
                                    throw new Error('반납할 품목의 수량을 하나 이상 입력하세요.')
                                }
                            },
                        },
                    ]}
                >
                    {(fields, _operations, { errors }) =>
                        fields.length === 0 ? (
                            <Typography.Text type="secondary">불출을 고르면 품목이 나옵니다.</Typography.Text>
                        ) : (
                            <>
                                <Row gutter={8} className="item-header">
                                    <Col flex="32px" />
                                    <Col span={7}>약품</Col>
                                    <Col span={3}>불출량</Col>
                                    <Col span={3}>반납된</Col>
                                    <Col span={4}>이번 반납량</Col>
                                    <Col flex="auto">처리</Col>
                                </Row>
                                {fields.map((field, index) => {
                                    const source = sourceItems[index]
                                    const remaining = subtractQuantity(source?.quantity, source?.returnedQuantity)
                                    return (
                                        <Row key={field.key} gutter={8} align="top">
                                            <Col flex="32px">
                                                <Typography.Text type="secondary" className="item-line-no">
                                                    {source?.lineNo ?? index + 1}
                                                </Typography.Text>
                                            </Col>
                                            <Col span={7}>
                                                <Form.Item name={[field.name, 'issueItemId']} hidden>
                                                    <Input />
                                                </Form.Item>
                                                <div className="item-source">
                                                    <span className="mono">{source?.drugCode}</span> {source?.drugName}
                                                </div>
                                            </Col>
                                            <Col span={3}>
                                                <div className="item-source">{formatQuantity(source?.quantity)}</div>
                                            </Col>
                                            <Col span={3}>
                                                <div className="item-source">{formatQuantity(source?.returnedQuantity)}</div>
                                            </Col>
                                            <Col span={4}>
                                                <Form.Item
                                                    name={[field.name, 'quantity']}
                                                    rules={[
                                                        {
                                                            validator: async (_, value?: number | null) => {
                                                                if (value != null && value > remaining) {
                                                                    throw new Error(`남은 ${formatQuantity(remaining)}까지`)
                                                                }
                                                            },
                                                        },
                                                    ]}
                                                >
                                                    <InputNumber
                                                        min={0}
                                                        max={remaining}
                                                        disabled={remaining <= 0}
                                                        placeholder={remaining > 0 ? `최대 ${formatQuantity(remaining)}` : '모두 반납됨'}
                                                        className="full-width"
                                                    />
                                                </Form.Item>
                                            </Col>
                                            <Col flex="auto">
                                                <Form.Item name={[field.name, 'disposition']} rules={[{ required: true }]}>
                                                    <Select options={DISPOSITION_OPTIONS} disabled={remaining <= 0} />
                                                </Form.Item>
                                            </Col>
                                        </Row>
                                    )
                                })}
                                <Form.ErrorList errors={errors} />
                            </>
                        )
                    }
                </Form.List>
            </Form>
        </Modal>
    )
}
