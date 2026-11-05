import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Select, Table, Tag, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { PageHeader } from '@/components/PageHeader'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { useMenus } from '@/layouts/MenuContext'
import { formatDateTime } from '@/utils/format'
import {
    DISPENSING_STATUSES,
    optionsOf,
    pickCode,
    pickDate,
    pickId,
    type DispensingStatus,
} from '@/features/pharmacy/pharmacyFields'
import { codeTag, DateRangeFilter } from '@/features/pharmacy/pharmacyParts'
import { DispensingDetailDrawer } from './DispensingDetailDrawer'
import { DispensingFormModal } from './DispensingFormModal'
import type { DispensingSummaryResponse } from './dispensingFields'

/** URL 쿼리스트링에 두는 검색 조건. prescriptionId는 처방 상세의 "조제 내역"에서 넘어온다 */
const FILTER_KEYS = ['keyword', 'status', 'from', 'to', 'prescriptionId'] as const

const STATUS_OPTIONS = optionsOf(DISPENSING_STATUSES)

/**
 * 조제 목록 화면: 검색 · 목록 · 조제 등록.
 *
 * 처방 → **조제** → 검수 → 불출 → 반납. 행을 누르면 상세 드로어가 열리고, 거기서 상태에 맞는 다음 단계
 * (검수 · 불출 · 재조제)로 이어진다. 화면 구조는 처방 목록(PrescriptionListPage)과 같다.
 */
export function DispensingListPage() {
    const { can } = useMenus()
    const canCreate = can('/dispensings', 'create')
    const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)

    const status = pickCode<DispensingStatus>(filters.status, DISPENSING_STATUSES)
    const from = pickDate(filters.from)
    const to = pickDate(filters.to)
    const prescriptionId = pickId(filters.prescriptionId)

    const listQuery = $api.useQuery(
        'get',
        '/api/dispensings',
        { params: { query: { keyword: filters.keyword, status, from, to, prescriptionId, page: page - 1, size } } },
        { placeholderData: keepPreviousData },
    )

    const updateFilter = (patch: Partial<typeof filters>) => setFilters({ ...filters, ...patch })
    const [detailId, setDetailId] = useState<number | null>(null)
    const [formOpen, setFormOpen] = useState(false)

    const columns: TableColumnsType<DispensingSummaryResponse> = [
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
            title: '상태',
            dataIndex: 'status',
            width: 110,
            align: 'center',
            render: (value?: DispensingStatus) => codeTag(DISPENSING_STATUSES, value),
        },
        { title: '조제자', dataIndex: 'dispensedBy', width: 110 },
        { title: '조제 일시', dataIndex: 'dispensedAt', width: 170, render: (value?: string) => formatDateTime(value) },
    ]

    return (
        <>
            <PageHeader
                title="조제"
                extra={
                    canCreate && (
                        <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormOpen(true)}>
                            조제 등록
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
                        value={status}
                        options={STATUS_OPTIONS}
                        placeholder="상태 전체"
                        allowClear
                        className="filter-select-narrow"
                        onChange={(value?: string) => updateFilter({ status: value })}
                    />
                    <DateRangeFilter label="조제" from={from} to={to} onChange={updateFilter} />
                    {prescriptionId !== undefined && (
                        <Tag closable onClose={() => updateFilter({ prescriptionId: undefined })}>
                            처방 ID {prescriptionId}의 조제만
                        </Tag>
                    )}
                    <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
                        초기화
                    </Button>
                </div>

                {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}
                <Table<DispensingSummaryResponse>
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

                <DispensingDetailDrawer dispensingId={detailId} onClose={() => setDetailId(null)} />
                <DispensingFormModal open={formOpen} prescriptionId={null} onClose={() => setFormOpen(false)} />
            </Card>
        </>
    )
}
