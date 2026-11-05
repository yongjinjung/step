import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Table, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { PageHeader } from '@/components/PageHeader'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { useMenus } from '@/layouts/MenuContext'
import { formatDateTime } from '@/utils/format'
import { pickDate } from '@/features/pharmacy/pharmacyFields'
import { DateRangeFilter } from '@/features/pharmacy/pharmacyParts'
import { IssueDetailDrawer } from './IssueDetailDrawer'
import { IssueFormModal } from './IssueFormModal'
import type { IssueSummaryResponse } from './issueFields'

const FILTER_KEYS = ['keyword', 'destinationCode', 'from', 'to'] as const

/**
 * 불출 목록 화면: 검색 · 목록 · 불출 등록.
 *
 * 처방 → 조제 → 검수 → **불출** → 반납. 불출처 코드는 정확히 일치로 찾는다(서버가 대문자로 맞춘다).
 */
export function IssueListPage() {
    const { can } = useMenus()
    const canCreate = can('/issues', 'create')
    const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)

    const from = pickDate(filters.from)
    const to = pickDate(filters.to)

    const listQuery = $api.useQuery(
        'get',
        '/api/issues',
        {
            params: {
                query: { keyword: filters.keyword, destinationCode: filters.destinationCode, from, to, page: page - 1, size },
            },
        },
        { placeholderData: keepPreviousData },
    )

    const updateFilter = (patch: Partial<typeof filters>) => setFilters({ ...filters, ...patch })
    const [detailId, setDetailId] = useState<number | null>(null)
    const [formOpen, setFormOpen] = useState(false)

    const columns: TableColumnsType<IssueSummaryResponse> = [
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
        {
            title: '불출처',
            key: 'destination',
            width: 160,
            render: (_, record) => (
                <>
                    {record.destinationName} <span className="mono">({record.destinationCode})</span>
                </>
            ),
        },
        { title: '수령자', dataIndex: 'receiverName', width: 110 },
        { title: '불출자', dataIndex: 'issuedBy', width: 110 },
        { title: '불출 일시', dataIndex: 'issuedAt', width: 170, render: (value?: string) => formatDateTime(value) },
    ]

    return (
        <>
            <PageHeader
                title="불출"
                extra={
                    canCreate && (
                        <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormOpen(true)}>
                            불출 등록
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
                    <Input.Search
                        key={`destination-${filters.destinationCode ?? ''}`}
                        defaultValue={filters.destinationCode}
                        placeholder="불출처 코드"
                        allowClear
                        className="filter-select-narrow"
                        onSearch={(destinationCode) => updateFilter({ destinationCode })}
                    />
                    <DateRangeFilter label="불출" from={from} to={to} onChange={updateFilter} />
                    <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
                        초기화
                    </Button>
                </div>

                {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}
                <Table<IssueSummaryResponse>
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

                <IssueDetailDrawer issueId={detailId} onClose={() => setDetailId(null)} />
                <IssueFormModal open={formOpen} dispensingId={null} onClose={() => setFormOpen(false)} />
            </Card>
        </>
    )
}
