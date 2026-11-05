import { Alert, App, Col, Descriptions, Divider, Form, Input, Modal, Row, Table, type TableColumnsType } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyFieldErrors, errorMessage } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import type { DispensingItemResponse } from '@/features/dispensings/dispensingFields'
import { DispensingSelect } from '@/features/pharmacy/PharmacySelects'
import { formatQuantity, trimToUndefined } from '@/features/pharmacy/pharmacyFields'
import type { IssueRequest } from './issueFields'

interface IssueFormModalProps {
    open: boolean
    /** 불출할 조제. 조제 상세에서 열면 정해져 있고, 불출 목록에서 열면 null(모달에서 고른다) */
    dispensingId: number | null
    onClose: () => void
}

/** 불출될 품목 미리보기 열 (입력 없음: 조제 품목이 수량 그대로 불출된다) */
const PREVIEW_COLUMNS: TableColumnsType<DispensingItemResponse> = [
    { title: '순번', dataIndex: 'lineNo', width: 60, align: 'center' },
    { title: '약품코드', dataIndex: 'drugCode', width: 110, render: (value: string) => <span className="mono">{value}</span> },
    { title: '약품명', dataIndex: 'drugName', ellipsis: true },
    { title: '제조번호', dataIndex: 'lotNo', width: 120, render: (value?: string) => value ?? '-' },
    { title: '불출량', key: 'quantity', width: 90, align: 'right', render: (_, item) => formatQuantity(item.quantity) },
]

/**
 * 불출 등록 모달 (POST /api/issues).
 *
 * 검수 합격(INSPECTED) 조제를 고르고 불출처·수령자만 입력한다. 품목은 요청으로 보내지 않고 서버가 조제 품목을
 * 수량 그대로 옮기므로, 화면은 불출될 품목을 표로 미리 보여주기만 한다. 불출하면 조제가 불출됨(ISSUED)이 된다.
 * 불출처 코드는 서버가 대문자로 저장한다.
 */
export function IssueFormModal({ open, dispensingId, onClose }: IssueFormModalProps) {
    const { message } = App.useApp()
    const invalidate = useInvalidateApi()
    const [form] = Form.useForm<IssueRequest>()
    const createIssue = $api.useMutation('post', '/api/issues')

    const selectedId = Form.useWatch('dispensingId', form)
    const dispensingQuery = $api.useQuery(
        'get',
        '/api/dispensings/{id}',
        { params: { path: { id: selectedId ?? 0 } } },
        { enabled: open && selectedId !== undefined },
    )
    const dispensing = dispensingQuery.data
    const issuable = dispensing?.status === 'INSPECTED'

    useEffect(() => {
        if (!open) {
            return
        }
        form.resetFields()
        form.setFieldsValue({ dispensingId: dispensingId ?? undefined })
    }, [open, dispensingId, form])

    const handleFinish = (values: IssueRequest) => {
        createIssue.mutate(
            { body: { ...values, remark: trimToUndefined(values.remark) } },
            {
                onSuccess: (created) => {
                    message.success(`처방 ${created.prescriptionNo}을(를) ${created.destinationName}(으)로 불출했습니다.`)
                    // 불출 목록·상세와 조제 상태(INSPECTED → ISSUED)를 함께 새로 고친다.
                    invalidate('/api/issues', '/api/issues/{id}', '/api/dispensings', '/api/dispensings/{id}')
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
            title="불출 등록"
            okText="불출"
            okButtonProps={{ disabled: !issuable }}
            confirmLoading={createIssue.isPending}
            onOk={() => form.submit()}
            onCancel={onClose}
            forceRender
            width={860}
        >
            <Form form={form} layout="vertical" onFinish={handleFinish}>
                <Form.Item name="dispensingId" label="조제 (검수 합격)" rules={[{ required: true, message: '조제를 선택하세요.' }]}>
                    <DispensingSelect status="INSPECTED" disabled={dispensingId !== null} />
                </Form.Item>

                {dispensingQuery.isError && <Alert type="error" showIcon title={errorMessage(dispensingQuery.error)} />}
                {dispensing && (
                    <Descriptions column={2} bordered size="small" className="page-notice">
                        <Descriptions.Item label="환자">
                            {dispensing.patientName} ({dispensing.patientNo})
                        </Descriptions.Item>
                        <Descriptions.Item label="조제자">{dispensing.dispensedBy}</Descriptions.Item>
                    </Descriptions>
                )}
                {dispensing && !issuable && (
                    <Alert type="warning" showIcon className="page-notice" title="검수에 합격한 조제만 불출할 수 있습니다." />
                )}

                <Row gutter={12}>
                    <Col span={6}>
                        <Form.Item
                            name="destinationCode"
                            label="불출처 코드"
                            rules={[{ required: true, whitespace: true, message: '불출처 코드를 입력하세요.' }, { max: 20 }]}
                        >
                            <Input placeholder="예: W71" className="mono" />
                        </Form.Item>
                    </Col>
                    <Col span={9}>
                        <Form.Item
                            name="destinationName"
                            label="불출처 이름"
                            rules={[{ required: true, whitespace: true, message: '불출처 이름을 입력하세요.' }, { max: 100 }]}
                        >
                            <Input placeholder="예: 71병동" />
                        </Form.Item>
                    </Col>
                    <Col span={9}>
                        <Form.Item
                            name="receiverName"
                            label="수령자"
                            rules={[{ required: true, whitespace: true, message: '수령자를 입력하세요.' }, { max: 100 }]}
                        >
                            <Input />
                        </Form.Item>
                    </Col>
                </Row>
                <Form.Item name="remark" label="비고" rules={[{ max: 1000 }]}>
                    <Input.TextArea rows={2} />
                </Form.Item>

                <Divider titlePlacement="start" plain>
                    불출될 품목 (조제 품목 그대로)
                </Divider>
                <Table<DispensingItemResponse>
                    rowKey="id"
                    size="small"
                    columns={PREVIEW_COLUMNS}
                    dataSource={dispensing?.items}
                    loading={dispensingQuery.isFetching}
                    pagination={false}
                    locale={{ emptyText: '조제를 고르면 품목이 나옵니다.' }}
                />
            </Form>
        </Modal>
    )
}
