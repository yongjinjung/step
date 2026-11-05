import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Select, Table, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { PageHeader } from '@/components/PageHeader'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { useMenus } from '@/layouts/MenuContext'
import { formatDateTime } from '@/utils/format'
import { INSPECTION_RESULTS, optionsOf, pickCode, pickDate, type InspectionResult } from '@/features/pharmacy/pharmacyFields'
import { codeTag, DateRangeFilter } from '@/features/pharmacy/pharmacyParts'
import { InspectionDetailDrawer } from './InspectionDetailDrawer'
import { InspectionFormModal } from './InspectionFormModal'
import type { InspectionSummaryResponse } from './inspectionFields'

const FILTER_KEYS = ['keyword', 'result', 'from', 'to'] as const

const RESULT_OPTIONS = optionsOf(INSPECTION_RESULTS)

/**
 * 검수 목록 화면: 검색 · 목록 · 검수 등록.
 *
 * 처방 → 조제 → **검수** → 불출 → 반납. 검수 등록은 검수 대기(DISPENSED) 조제를 골라 품목별 합격/불합격을 입력한다.
 */
export function InspectionListPage() {
    const { can } = useMenus()
    const canCreate = can('/inspections', 'create')
    const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)

    const result = pickCode<InspectionResult>(filters.result, INSPECTION_RESULTS)
    const from = pickDate(filters.from)
    const to = pickDate(filters.to)

    const listQuery = $api.useQuery(
        'get',
        '/api/inspections',
        { params: { query: { keyword: filters.keyword, result, from, to, page: page - 1, size } } },
        { placeholderData: keepPreviousData },
    )

    const updateFilter = (patch: Partial<typeof filters>) => setFilters({ ...filters, ...patch })
    const [detailId, setDetailId] = useState<number | null>(null)
    const [formOpen, setFormOpen] = useState(false)

    const columns: TableColumnsType<InspectionSummaryResponse> = [
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
            title: '결과',
            dataIndex: 'result',
            width: 90,
            align: 'center',
            render: (value?: InspectionResult) => codeTag(INSPECTION_RESULTS, value),
        },
        { title: '검수자', dataIndex: 'inspectedBy', width: 110 },
        { title: '검수 일시', dataIndex: 'inspectedAt', width: 170, render: (value?: string) => formatDateTime(value) },
    ]

    return (
        <>
            <PageHeader
                title="검수"
                extra={
                    canCreate && (
                        <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormOpen(true)}>
                            검수 등록
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
                    <Select
                        value={result}
                        options={RESULT_OPTIONS}
                        placeholder="결과 전체"
                        allowClear
                        className="filter-select-narrow"
                        onChange={(value?: string) => updateFilter({ result: value })}
                    />
                    <DateRangeFilter label="검수" from={from} to={to} onChange={updateFilter} />
                    <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
                        초기화
                    </Button>
                </div>

                {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}
                <Table<InspectionSummaryResponse>
                    rowKey="id"
                    size="middle"
                    columns={columns}
                    dataSource={listQuery.data?.content}
                    loading={listQuery.isFetching}
                    scroll={{ x: 800 }}
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

                <InspectionDetailDrawer inspectionId={detailId} onClose={() => setDetailId(null)} />
                <InspectionFormModal open={formOpen} dispensingId={null} onClose={() => setFormOpen(false)} />
            </Card>
        </>
    )
}
