import { Alert, Descriptions, Drawer, Skeleton, Table, Typography, type TableColumnsType } from 'antd'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { formatDateTime } from '@/utils/format'
import { INSPECTION_RESULTS, formatQuantity, type InspectionResult } from '@/features/pharmacy/pharmacyFields'
import { codeTag } from '@/features/pharmacy/pharmacyParts'
import type { InspectionItemResponse } from './inspectionFields'

interface InspectionDetailDrawerProps {
    /** 보여줄 검수 ID. null이면 닫힌다 */
    inspectionId: number | null
    onClose: () => void
}

const ITEM_COLUMNS: TableColumnsType<InspectionItemResponse> = [
    { title: '순번', dataIndex: 'lineNo', width: 60, align: 'center' },
    { title: '약품코드', dataIndex: 'drugCode', width: 110, render: (value: string) => <span className="mono">{value}</span> },
    { title: '약품명', dataIndex: 'drugName', ellipsis: true },
    { title: '조제량', key: 'quantity', width: 90, align: 'right', render: (_, item) => formatQuantity(item.quantity) },
    { title: '제조번호', dataIndex: 'lotNo', width: 120, render: (value?: string) => (value ? <span className="mono">{value}</span> : '-') },
    {
        title: '결과',
        dataIndex: 'result',
        width: 80,
        align: 'center',
        render: (value?: InspectionResult) => codeTag(INSPECTION_RESULTS, value),
    },
    { title: '불량 사유', dataIndex: 'defectReason', width: 180, ellipsis: true, render: (value?: string) => value || '-' },
]

/**
 * 검수 상세와 품목별 결과. 검수는 등록 후 바뀌지 않으므로 버튼이 없다.
 * 다음 단계(합격 → 불출, 불합격 → 재조제)는 조제 상세에서 이어 간다.
 */
export function InspectionDetailDrawer({ inspectionId, onClose }: InspectionDetailDrawerProps) {
    const open = inspectionId !== null
    const detailQuery = $api.useQuery(
        'get',
        '/api/inspections/{id}',
        { params: { path: { id: inspectionId ?? 0 } } },
        { enabled: open },
    )
    const inspection = detailQuery.data

    return (
        <Drawer
            open={open}
            onClose={onClose}
            size="large"
            title={inspection ? `검수 · 처방 ${inspection.prescriptionNo}` : '검수 상세'}
        >
            {detailQuery.isPending && open ? (
                <Skeleton active />
            ) : detailQuery.isError ? (
                <Alert type="error" showIcon title={errorMessage(detailQuery.error)} />
            ) : (
                inspection && (
                    <>
                        <Descriptions column={2} bordered size="small">
                            <Descriptions.Item label="처방전번호">
                                <span className="mono">{inspection.prescriptionNo}</span>
                            </Descriptions.Item>
                            <Descriptions.Item label="결과">{codeTag(INSPECTION_RESULTS, inspection.result)}</Descriptions.Item>
                            <Descriptions.Item label="환자">
                                {inspection.patientName} ({inspection.patientNo})
                            </Descriptions.Item>
                            <Descriptions.Item label="검수자">{inspection.inspectedBy}</Descriptions.Item>
                            <Descriptions.Item label="검수 일시" span={2}>
                                {formatDateTime(inspection.inspectedAt)}
                            </Descriptions.Item>
                            <Descriptions.Item label="비고" span={2}>
                                {inspection.remark || '-'}
                            </Descriptions.Item>
                        </Descriptions>

                        <Alert
                            type={inspection.result === 'PASS' ? 'success' : 'warning'}
                            showIcon
                            className="page-notice"
                            title={
                                inspection.result === 'PASS'
                                    ? '전 품목 합격 — 조제 화면에서 불출할 수 있습니다.'
                                    : '불합격 품목이 있습니다 — 조제 화면에서 같은 처방으로 재조제합니다.'
                            }
                        />

                        <Typography.Title level={5} className="section-title">
                            검수 품목 ({inspection.items?.length ?? 0}건)
                        </Typography.Title>
                        <Table<InspectionItemResponse>
                            rowKey="id"
                            size="small"
                            columns={ITEM_COLUMNS}
                            dataSource={inspection.items}
                            pagination={false}
                            scroll={{ x: 760 }}
                        />
                    </>
                )
            )}
        </Drawer>
    )
}
