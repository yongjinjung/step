import { Alert, Button, Descriptions, Drawer, Skeleton, Space, Table, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useMenus } from '@/layouts/MenuContext'
import { formatDateTime } from '@/utils/format'
import { ReturnFormModal } from '@/features/returns/ReturnFormModal'
import { formatQuantity, subtractQuantity } from '@/features/pharmacy/pharmacyFields'
import type { IssueItemResponse } from './issueFields'

interface IssueDetailDrawerProps {
    /** 보여줄 불출 ID. null이면 닫힌다 */
    issueId: number | null
    onClose: () => void
}

const ITEM_COLUMNS: TableColumnsType<IssueItemResponse> = [
    { title: '순번', dataIndex: 'lineNo', width: 60, align: 'center' },
    { title: '약품코드', dataIndex: 'drugCode', width: 110, render: (value: string) => <span className="mono">{value}</span> },
    { title: '약품명', dataIndex: 'drugName', ellipsis: true },
    { title: '제조번호', dataIndex: 'lotNo', width: 120, render: (value?: string) => (value ? <span className="mono">{value}</span> : '-') },
    { title: '불출량', key: 'quantity', width: 90, align: 'right', render: (_, item) => formatQuantity(item.quantity) },
    { title: '반납량', key: 'returnedQuantity', width: 90, align: 'right', render: (_, item) => formatQuantity(item.returnedQuantity) },
    {
        title: '반납 가능',
        key: 'remaining',
        width: 90,
        align: 'right',
        render: (_, item) => formatQuantity(subtractQuantity(item.quantity, item.returnedQuantity)),
    },
]

/**
 * 불출 상세와 품목. 품목마다 지금까지 반납한 합계(returnedQuantity)와 남은(반납 가능) 수량을 보여준다.
 * 남은 수량이 있으면 "반납" 버튼으로 반납 등록을 열고, "반납 내역"은 이 불출의 반납 목록으로 이동한다.
 */
export function IssueDetailDrawer({ issueId, onClose }: IssueDetailDrawerProps) {
    const open = issueId !== null
    const detailQuery = $api.useQuery(
        'get',
        '/api/issues/{id}',
        { params: { path: { id: issueId ?? 0 } } },
        { enabled: open },
    )
    const issue = detailQuery.data

    const { can } = useMenus()
    const navigate = useNavigate()
    const [returnOpen, setReturnOpen] = useState(false)

    // 하나라도 남은 수량이 있어야 반납할 수 있다(전부 반납했으면 서버도 RETURN_QUANTITY_EXCEEDED로 막는다).
    const returnable = (issue?.items ?? []).some((item) => subtractQuantity(item.quantity, item.returnedQuantity) > 0)
    const canReturn = returnable && can('/returns', 'create')

    return (
        <Drawer
            open={open}
            onClose={onClose}
            size="large"
            title={issue ? `불출 · 처방 ${issue.prescriptionNo}` : '불출 상세'}
            extra={
                <Space>
                    {issue && can('/returns', 'read') && (
                        <Button
                            onClick={() => {
                                onClose()
                                navigate(`/returns?issueId=${issueId}`)
                            }}
                        >
                            반납 내역
                        </Button>
                    )}
                    {canReturn && (
                        <Button type="primary" onClick={() => setReturnOpen(true)}>
                            반납
                        </Button>
                    )}
                </Space>
            }
        >
            {detailQuery.isPending && open ? (
                <Skeleton active />
            ) : detailQuery.isError ? (
                <Alert type="error" showIcon title={errorMessage(detailQuery.error)} />
            ) : (
                issue && (
                    <>
                        <Descriptions column={2} bordered size="small">
                            <Descriptions.Item label="처방전번호">
                                <span className="mono">{issue.prescriptionNo}</span>
                            </Descriptions.Item>
                            <Descriptions.Item label="환자">
                                {issue.patientName} ({issue.patientNo})
                            </Descriptions.Item>
                            <Descriptions.Item label="불출처">
                                {issue.destinationName} (<span className="mono">{issue.destinationCode}</span>)
                            </Descriptions.Item>
                            <Descriptions.Item label="수령자">{issue.receiverName}</Descriptions.Item>
                            <Descriptions.Item label="불출자">{issue.issuedBy}</Descriptions.Item>
                            <Descriptions.Item label="불출 일시">{formatDateTime(issue.issuedAt)}</Descriptions.Item>
                            <Descriptions.Item label="비고" span={2}>
                                {issue.remark || '-'}
                            </Descriptions.Item>
                        </Descriptions>

                        <Typography.Title level={5} className="section-title">
                            불출 품목 ({issue.items?.length ?? 0}건)
                        </Typography.Title>
                        <Table<IssueItemResponse>
                            rowKey="id"
                            size="small"
                            columns={ITEM_COLUMNS}
                            dataSource={issue.items}
                            pagination={false}
                            scroll={{ x: 760 }}
                        />
                    </>
                )
            )}

            <ReturnFormModal open={returnOpen} issueId={issueId} onClose={() => setReturnOpen(false)} />
        </Drawer>
    )
}
