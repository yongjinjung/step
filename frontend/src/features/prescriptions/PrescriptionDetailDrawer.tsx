import { Alert, App, Button, Descriptions, Drawer, Skeleton, Table, Tag, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage, isApiError } from '@/api/errors'
import { ReasonModal } from '@/components/ReasonModal'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { useMenus } from '@/layouts/MenuContext'
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

    const { message } = App.useApp()
    const { can } = useMenus()
    const invalidate = useInvalidateApi()
    const [cancelOpen, setCancelOpen] = useState(false)
    const cancelPrescription = $api.useMutation('post', '/api/prescriptions/{id}/cancel')

    // 버튼 표시 조건: 수정 권한 + 조제 전(ORDERED). 최종 검사는 서버가 한다.
    const cancellable = can('/prescriptions', 'update') && prescription?.status === 'ORDERED'

    const handleCancel = (reason: string | undefined) => {
        if (prescriptionId === null || !reason) {
            return
        }
        cancelPrescription.mutate(
            { params: { path: { id: prescriptionId } }, body: { reason } },
            {
                onSuccess: () => {
                    message.success('처방을 취소했습니다.')
                    setCancelOpen(false)
                    // 목록(상태 열)과 이 상세(취소 안내)를 함께 새로 고친다.
                    invalidate('/api/prescriptions', '/api/prescriptions/{id}')
                },
                onError: (error: unknown) => {
                    message.error(errorMessage(error))
                    // 그사이 조제된 경우: 화면의 상태가 낡았으므로 새로 불러온다.
                    if (isApiError(error) && error.code === 'PRESCRIPTION_NOT_CANCELLABLE') {
                        setCancelOpen(false)
                        invalidate('/api/prescriptions', '/api/prescriptions/{id}')
                    }
                },
            },
        )
    }

    return (
        <Drawer
            open={open}
            onClose={onClose}
            size="large"
            title={prescription ? `처방 ${prescription.prescriptionNo}` : '처방 상세'}
            extra={
                cancellable && (
                    <Button danger onClick={() => setCancelOpen(true)}>
                        처방 취소
                    </Button>
                )
            }
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
            <ReasonModal
                open={cancelOpen}
                title="처방 취소"
                description={
                    <Typography.Paragraph type="secondary">
                        {prescription?.prescriptionNo} ({prescription?.patientName}) 처방을 취소합니다. 취소한 처방은 되돌릴 수 없고 조제할 수 없습니다.
                    </Typography.Paragraph>
                }
                okText="취소 처리"
                label="취소 사유"
                placeholder="예: 처방 변경으로 재처방"
                required
                danger
                confirmLoading={cancelPrescription.isPending}
                onSubmit={handleCancel}
                onCancel={() => setCancelOpen(false)}
            />
        </Drawer>
    )
}
