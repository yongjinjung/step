import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Table, Tag, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { PageHeader } from '@/components/PageHeader'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { useMenus } from '@/layouts/MenuContext'
import { formatDateTime } from '@/utils/format'
import { pickDate, pickId } from '@/features/pharmacy/pharmacyFields'
import { DateRangeFilter } from '@/features/pharmacy/pharmacyParts'
import { ReturnDetailDrawer } from './ReturnDetailDrawer'
import { ReturnFormModal } from './ReturnFormModal'
import type { DrugReturnSummaryResponse } from './returnFields'

/** issueId는 불출 상세의 "반납 내역"에서 넘어온다 */
const FILTER_KEYS = ['keyword', 'from', 'to', 'issueId'] as const

/**
 * 반납 목록 화면: 검색 · 목록 · 반납 등록.
 *
 * 처방 → 조제 → 검수 → 불출 → **반납**. 불출 하나를 여러 번 나눠, 일부 품목만 반납할 수 있다.
 */
export function ReturnListPage() {
    const { can } = useMenus()
    const canCreate = can('/returns', 'create')
    const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)

    const from = pickDate(filters.from)
    const to = pickDate(filters.to)
    const issueId = pickId(filters.issueId)

    const listQuery = $api.useQuery(
        'get',
        '/api/returns',
        { params: { query: { keyword: filters.keyword, from, to, issueId, page: page - 1, size } } },
        { placeholderData: keepPreviousData },
    )

    const updateFilter = (patch: Partial<typeof filters>) => setFilters({ ...filters, ...patch })
    const [detailId, setDetailId] = useState<number | null>(null)
    const [formOpen, setFormOpen] = useState(false)

    const columns: TableColumnsType<DrugReturnSummaryResponse> = [
        {
            title: '처방전번호',
            dataIndex: 'prescriptionNo',
            width: 160,
            render: (value: string, record) => (
                <Typography.Link className="mono" onClick={() => record.id !== undefined && setDetailId(record.id)}>
                    {value}
                </Typography.Link>
            ),
        },
        { title: '환자번호', dataIndex: 'patientNo', width: 120 },
        { title: '환자명', dataIndex: 'patientName', width: 110 },
        { title: '불출처', dataIndex: 'destinationName', width: 120 },
        { title: '반납 사유', dataIndex: 'reason', ellipsis: true },
        { title: '반납자', dataIndex: 'returnedBy', width: 110 },
        { title: '반납 일시', dataIndex: 'returnedAt', width: 170, render: (value?: string) => formatDateTime(value) },
    ]

    return (
        <>
            <PageHeader
                title="반납"
                extra={
                    canCreate && (
                        <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormOpen(true)}>
                            반납 등록
                        </Button>
                    )
                }
            />
            <Card>
                <div className="list-toolbar">
                    <Input.Search
                        key={filters.keyword ?? ''}
                        defaultValue={filters.keyword}
                        placeholder="처방전번호 · 환자번호 · 환자명 (앞부분)"
                        allowClear
                        enterButton
                        className="search-input"
                        onSearch={(keyword) => updateFilter({ keyword })}
                    />
                    <DateRangeFilter label="반납" from={from} to={to} onChange={updateFilter} />
                    {issueId !== undefined && (
                        <Tag closable onClose={() => updateFilter({ issueId: undefined })}>
                            불출 ID {issueId}의 반납만
                        </Tag>
                    )}
                    <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
                        초기화
                    </Button>
                </div>

                {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}
                <Table<DrugReturnSummaryResponse>
                    rowKey="id"
                    size="middle"
                    columns={columns}
                    dataSource={listQuery.data?.content}
                    loading={listQuery.isFetching}
                    scroll={{ x: 960 }}
                    pagination={{
                        current: page,
                        pageSize: size,
                        total: listQuery.data?.totalElements ?? 0,
                        showSizeChanger: true,
                        pageSizeOptions: [20, 50, 100],
                        showTotal: (total) => `총 ${total.toLocaleString()}건`,
                        onChange: setPaging,
                    }}
                />

                <ReturnDetailDrawer returnId={detailId} onClose={() => setDetailId(null)} />
                <ReturnFormModal open={formOpen} issueId={null} onClose={() => setFormOpen(false)} />
            </Card>
        </>
    )
}
