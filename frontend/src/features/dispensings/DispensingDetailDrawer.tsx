import { Alert, Button, Descriptions, Drawer, Skeleton, Space, Table, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useMenus } from '@/layouts/MenuContext'
import { formatDateTime } from '@/utils/format'
import { InspectionFormModal } from '@/features/inspections/InspectionFormModal'
import { IssueFormModal } from '@/features/issues/IssueFormModal'
import { DISPENSING_STATUSES, formatQuantity } from '@/features/pharmacy/pharmacyFields'
import { codeTag } from '@/features/pharmacy/pharmacyParts'
import { DispensingFormModal } from './DispensingFormModal'
import type { DispensingItemResponse } from './dispensingFields'

interface DispensingDetailDrawerProps {
    /** 보여줄 조제 ID. null이면 닫힌다 */
    dispensingId: number | null
    onClose: () => void
}

/** 조제 품목 표의 열 */
const ITEM_COLUMNS: TableColumnsType<DispensingItemResponse> = [
    { title: '순번', dataIndex: 'lineNo', width: 60, align: 'center' },
    { title: '약품코드', dataIndex: 'drugCode', width: 110, render: (value: string) => <span className="mono">{value}</span> },
    { title: '약품명', dataIndex: 'drugName', ellipsis: true },
    { title: '처방량', key: 'prescribedQuantity', width: 90, align: 'right', render: (_, item) => formatQuantity(item.prescribedQuantity) },
    { title: '조제량', key: 'quantity', width: 90, align: 'right', render: (_, item) => formatQuantity(item.quantity) },
    { title: '제조번호', dataIndex: 'lotNo', width: 120, render: (value?: string) => (value ? <span className="mono">{value}</span> : '-') },
    { title: '유효기간', dataIndex: 'expiryDate', width: 110, render: (value?: string) => value ?? '-' },
]

/** 다음 단계 모달: 상태에 따라 검수 → 불출, 불합격이면 재조제 */
type NextStep = 'inspect' | 'issue' | 'redispense' | null

/**
 * 조제 상세와 품목. 상태에 맞는 다음 단계 버튼을 보여준다.
 *
 * | 조제 상태 | 버튼 | 권한 |
 * |---|---|---|
 * | 검수 대기(DISPENSED) | 검수 | 검수 등록 |
 * | 검수 합격(INSPECTED) | 불출 | 불출 등록 |
 * | 검수 불합격(REJECTED) | 재조제 | 조제 등록 |
 * | 불출됨(ISSUED) | 없음 | |
 */
export function DispensingDetailDrawer({ dispensingId, onClose }: DispensingDetailDrawerProps) {
    const open = dispensingId !== null
    const detailQuery = $api.useQuery(
        'get',
        '/api/dispensings/{id}',
        { params: { path: { id: dispensingId ?? 0 } } },
        { enabled: open },
    )
    const dispensing = detailQuery.data

    const { can } = useMenus()
    const [nextStep, setNextStep] = useState<NextStep>(null)

    const status = dispensing?.status
    const canInspect = status === 'DISPENSED' && can('/inspections', 'create')
    const canIssue = status === 'INSPECTED' && can('/issues', 'create')
    const canRedispense = status === 'REJECTED' && can('/dispensings', 'create')

    return (
        <Drawer
            open={open}
            onClose={onClose}
            size="large"
            title={dispensing ? `조제 · 처방 ${dispensing.prescriptionNo}` : '조제 상세'}
            extra={
                <Space>
                    {canInspect && (
                        <Button type="primary" onClick={() => setNextStep('inspect')}>
                            검수
                        </Button>
                    )}
                    {canIssue && (
                        <Button type="primary" onClick={() => setNextStep('issue')}>
                            불출
                        </Button>
                    )}
                    {canRedispense && <Button onClick={() => setNextStep('redispense')}>재조제</Button>}
                </Space>
            }
        >
            {detailQuery.isPending && open ? (
                <Skeleton active />
            ) : detailQuery.isError ? (
                <Alert type="error" showIcon title={errorMessage(detailQuery.error)} />
            ) : (
                dispensing && (
                    <>
                        <Descriptions column={2} bordered size="small">
                            <Descriptions.Item label="처방전번호">
                                <span className="mono">{dispensing.prescriptionNo}</span>
                            </Descriptions.Item>
                            <Descriptions.Item label="상태">{codeTag(DISPENSING_STATUSES, dispensing.status)}</Descriptions.Item>
                            <Descriptions.Item label="환자">
                                {dispensing.patientName} ({dispensing.patientNo})
                            </Descriptions.Item>
                            <Descriptions.Item label="조제자">{dispensing.dispensedBy}</Descriptions.Item>
                            <Descriptions.Item label="조제 일시">{formatDateTime(dispensing.dispensedAt)}</Descriptions.Item>
                            <Descriptions.Item label="수정 일시">{formatDateTime(dispensing.updatedAt)}</Descriptions.Item>
                            <Descriptions.Item label="비고" span={2}>
                                {dispensing.remark || '-'}
                            </Descriptions.Item>
                        </Descriptions>

                        {dispensing.status === 'REJECTED' && (
                            <Alert
                                type="warning"
                                showIcon
                                className="page-notice"
                                title="검수에서 불합격한 조제입니다."
                                description="같은 처방으로 다시 조제해야 합니다(재조제). 불합격 사유는 검수 화면에서 확인합니다."
                            />
                        )}

                        <Typography.Title level={5} className="section-title">
                            조제 품목 ({dispensing.items?.length ?? 0}건)
                        </Typography.Title>
                        <Table<DispensingItemResponse>
                            rowKey="id"
                            size="small"
                            columns={ITEM_COLUMNS}
                            dataSource={dispensing.items}
                            pagination={false}
                            scroll={{ x: 760 }}
                        />
                    </>
                )
            )}

            {/* 다음 단계 모달. 한 번에 하나만 열리므로 state 하나(nextStep)로 정한다. */}
            <InspectionFormModal
                open={nextStep === 'inspect'}
                dispensingId={dispensingId}
                onClose={() => setNextStep(null)}
            />
            <IssueFormModal open={nextStep === 'issue'} dispensingId={dispensingId} onClose={() => setNextStep(null)} />
            <DispensingFormModal
                open={nextStep === 'redispense'}
                prescriptionId={dispensing?.prescriptionId ?? null}
                onClose={() => setNextStep(null)}
            />
        </Drawer>
    )
}
