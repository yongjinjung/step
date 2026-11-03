import {Alert, Descriptions, Drawer, Skeleton, Table, Tag, Typography, type TableColumnsType} from 'antd'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { formatDateTime } from '@/utils/format'
import {
    PRESCRIPTION_STATUSES,
    PRESCRIPTION_TYPES,
    formatQuantity,
    type PrescriptionItemResponse,
} from './prescriptionFields'

interface PrescriptionDetailDrawerProps {
    /** 보여줄 처방 ID. null이면 닫힌다 */
    prescriptionId: number | null
    onClose: () => void
}

/** 품목 표의 열. props나 state를 쓰지 않으므로 컴포넌트 밖에 둔다. */
const ITEM_COLUMNS: TableColumnsType<PrescriptionItemResponse> = [
    { title: '순번', dataIndex: 'lineNo', width: 60, align: 'center' },
    { title: '약품코드', dataIndex: 'drugCode', width: 110, render: (value: string) => <span className="mono">{value}</span> },
    { title: '약품명', dataIndex: 'drugName', ellipsis: true },
    {
        title: '1회량',
        key: 'dose',
        width: 100,
        align: 'right',
        render: (_, item) => `${formatQuantity(item.dose)} ${item.doseUnit ?? ''}`,
    },
    { title: '경로', dataIndex: 'route', width: 70, align: 'center' },
    { title: '빈도', dataIndex: 'frequency', width: 70, align: 'center' },
    { title: '일수', dataIndex: 'days', width: 60, align: 'right' },
    { title: '총량', key: 'totalQuantity', width: 90, align: 'right', render: (_, item) => formatQuantity(item.totalQuantity) },
    { title: '용법', dataIndex: 'usageNote', width: 140, ellipsis: true },
]

/** 처방 상세와 품목. 목록의 행 데이터 대신 단건 API로 최신 값과 품목을 불러온다. */
export function PrescriptionDetailDrawer({ prescriptionId, onClose }: PrescriptionDetailDrawerProps) {
    const open = prescriptionId !== null

    const detailQuery = $api.useQuery(
        'get',
        '/api/prescriptions/{id}',
        { params: { path: { id: prescriptionId ?? 0 } } },
        { enabled: open },
    )
    const prescription = detailQuery.data

    return (
        <Drawer
            open={open}
            onClose={onClose}
            size="large"
            title={prescription ? `처방 ${prescription.prescriptionNo}` : '처방 상세'}
        >
            {detailQuery.isPending && open ? (
                <Skeleton active />
            ) : detailQuery.isError ? (
                <Alert type="error" showIcon title={errorMessage(detailQuery.error)} />
            ) : (
                prescription && (
                    <>
                        <Descriptions column={2} bordered size="small">
                            <Descriptions.Item label="처방전번호">
                                <span className="mono">{prescription.prescriptionNo}</span>
                            </Descriptions.Item>
                            <Descriptions.Item label="상태">
                                {prescription.status && (
                                    <Tag color={PRESCRIPTION_STATUSES[prescription.status].color}>
                                        {PRESCRIPTION_STATUSES[prescription.status].label}
                                    </Tag>
                                )}
                            </Descriptions.Item>
                            <Descriptions.Item label="환자">
                                {prescription.patientName} ({prescription.patientNo})
                            </Descriptions.Item>
                            <Descriptions.Item label="구분">
                                {prescription.prescriptionType && (
                                    <Tag color={PRESCRIPTION_TYPES[prescription.prescriptionType].color}>
                                        {PRESCRIPTION_TYPES[prescription.prescriptionType].label}
                                    </Tag>
                                )}
                            </Descriptions.Item>
                            <Descriptions.Item label="진료과">{prescription.departmentCode}</Descriptions.Item>
                            <Descriptions.Item label="처방의">{prescription.prescriberName}</Descriptions.Item>
                            <Descriptions.Item label="처방 일시">{formatDateTime(prescription.prescribedAt)}</Descriptions.Item>
                            <Descriptions.Item label="등록 일시">{formatDateTime(prescription.createdAt)}</Descriptions.Item>
                            <Descriptions.Item label="비고" span={2}>
                                {prescription.remark || '-'}
                            </Descriptions.Item>
                        </Descriptions>

                        {/* 취소된 처방만: 언제, 왜 취소했는지 */}
                        {prescription.status === 'CANCELLED' && (
                            <Alert
                                type="warning"
                                showIcon
                                className="page-notice"
                                title={`${formatDateTime(prescription.cancelledAt)}에 취소된 처방입니다.`}
                                description={`사유: ${prescription.cancelReason ?? '-'}`}
                            />
                        )}

                        <Typography.Title level={5} className="section-title">
                            처방 품목 ({prescription.items?.length ?? 0}건)
                        </Typography.Title>
                        <Table<PrescriptionItemResponse>
                            rowKey="id"
                            size="small"
                            columns={ITEM_COLUMNS}
                            dataSource={prescription.items}
                            pagination={false}
                            scroll={{ x: 760 }}
                        />
                    </>
                )
            )}
        </Drawer>
    )
}
