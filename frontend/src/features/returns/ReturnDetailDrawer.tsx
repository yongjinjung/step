import { Alert, Descriptions, Drawer, Skeleton, Table, Typography, type TableColumnsType } from 'antd'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { formatDateTime } from '@/utils/format'
import { RETURN_DISPOSITIONS, formatQuantity, type ReturnDisposition } from '@/features/pharmacy/pharmacyFields'
import { codeTag } from '@/features/pharmacy/pharmacyParts'
import type { DrugReturnItemResponse } from './returnFields'

interface ReturnDetailDrawerProps {
    /** 보여줄 반납 ID. null이면 닫힌다 */
    returnId: number | null
    onClose: () => void
}

const ITEM_COLUMNS: TableColumnsType<DrugReturnItemResponse> = [
    { title: '순번', dataIndex: 'lineNo', width: 60, align: 'center' },
    { title: '약품코드', dataIndex: 'drugCode', width: 110, render: (value: string) => <span className="mono">{value}</span> },
    { title: '약품명', dataIndex: 'drugName', ellipsis: true },
    { title: '불출량', key: 'issuedQuantity', width: 90, align: 'right', render: (_, item) => formatQuantity(item.issuedQuantity) },
    { title: '반납량', key: 'quantity', width: 90, align: 'right', render: (_, item) => formatQuantity(item.quantity) },
    {
        title: '처리',
        dataIndex: 'disposition',
        width: 90,
        align: 'center',
        render: (value?: ReturnDisposition) => codeTag(RETURN_DISPOSITIONS, value),
    },
]

/** 반납 상세와 품목. 반납은 등록 후 바뀌지 않으므로 버튼이 없다. */
export function ReturnDetailDrawer({ returnId, onClose }: ReturnDetailDrawerProps) {
    const open = returnId !== null
    const detailQuery = $api.useQuery(
        'get',
        '/api/returns/{id}',
        { params: { path: { id: returnId ?? 0 } } },
        { enabled: open },
    )
    const drugReturn = detailQuery.data

    return (
        <Drawer
            open={open}
            onClose={onClose}
            size="large"
            title={drugReturn ? `반납 · 처방 ${drugReturn.prescriptionNo}` : '반납 상세'}
        >
            {detailQuery.isPending && open ? (
                <Skeleton active />
            ) : detailQuery.isError ? (
                <Alert type="error" showIcon title={errorMessage(detailQuery.error)} />
            ) : (
                drugReturn && (
                    <>
                        <Descriptions column={2} bordered size="small">
                            <Descriptions.Item label="처방전번호">
                                <span className="mono">{drugReturn.prescriptionNo}</span>
                            </Descriptions.Item>
                            <Descriptions.Item label="환자">
                                {drugReturn.patientName} ({drugReturn.patientNo})
                            </Descriptions.Item>
                            <Descriptions.Item label="불출처">{drugReturn.destinationName}</Descriptions.Item>
                            <Descriptions.Item label="반납자">{drugReturn.returnedBy}</Descriptions.Item>
                            <Descriptions.Item label="반납 일시" span={2}>
                                {formatDateTime(drugReturn.returnedAt)}
                            </Descriptions.Item>
                            <Descriptions.Item label="반납 사유" span={2}>
                                {drugReturn.reason}
                            </Descriptions.Item>
                            <Descriptions.Item label="비고" span={2}>
                                {drugReturn.remark || '-'}
                            </Descriptions.Item>
                        </Descriptions>

                        <Typography.Title level={5} className="section-title">
                            반납 품목 ({drugReturn.items?.length ?? 0}건)
                        </Typography.Title>
                        <Table<DrugReturnItemResponse>
                            rowKey="id"
                            size="small"
                            columns={ITEM_COLUMNS}
                            dataSource={drugReturn.items}
                            pagination={false}
                            scroll={{ x: 640 }}
                        />
                    </>
                )
            )}
        </Drawer>
    )
}
