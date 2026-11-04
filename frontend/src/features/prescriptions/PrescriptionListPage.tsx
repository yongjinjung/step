import { useState } from 'react'
import { PrescriptionDetailDrawer } from './PrescriptionDetailDrawer'
import { PrescriptionFormModal } from './PrescriptionFormModal'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons' // ⑤ ReloadOutlined
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Select, Space, Table, Tag, Typography, type TableColumnsType } from 'antd' // ⑤ Input·Select·Space·Typography
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { PageHeader } from '@/components/PageHeader'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { useMenus } from '@/layouts/MenuContext'
import { formatDateTime } from '@/utils/format'
import {
    PRESCRIPTION_STATUSES,
    PRESCRIPTION_TYPES,
    type PrescriptionStatus,
    type PrescriptionSummaryResponse,
    type PrescriptionType,
} from './prescriptionFields'

/** URL 쿼리스트링에 두는 검색 조건 이름 (컴포넌트 밖 상수여야 한다: useListSearchParams 참고) */
const FILTER_KEYS = ['keyword', 'status', 'prescriptionType', 'from', 'to'] as const

/** ⑤ 선택 박스 항목. ①의 표에서 만들어 표와 항상 같다. */
const STATUS_OPTIONS = Object.entries(PRESCRIPTION_STATUSES).map(([value, { label }]) => ({ value, label }))
const TYPE_OPTIONS = Object.entries(PRESCRIPTION_TYPES).map(([value, { label }]) => ({ value, label }))

/** ⑤ 날짜 입력 값 형식 (yyyy-MM-dd) */
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

/** 주소의 문자열이 표의 코드값이면 그 값을, 아니면 undefined(조건 없음)를 돌려준다. */
function pickCode<T extends string>(value: string | undefined, table: Record<T, unknown>): T | undefined {
    return value !== undefined && Object.hasOwn(table, value) ? (value as T) : undefined
}

/** ⑤ 주소의 문자열이 yyyy-MM-dd 형식이면 그 값을, 아니면 undefined를 돌려준다. */
function pickDate(value: string | undefined): string | undefined {
    return value !== undefined && DATE_PATTERN.test(value) ? value : undefined
}

/**
 * 처방 목록 화면: 검색 · 목록.
 *
 * 처방 → 조제 → 검수 → 불출 → 반납으로 이어지는 약제 업무의 첫 화면이다.
 * 등록 버튼은 처방 메뉴의 등록 권한이 있을 때만 보인다(서버도 같은 권한으로 막는다).
 * 검색 조건과 페이지는 주소(?keyword=&status=&page=)에 남아 새로고침해도 유지된다.
 */
export function PrescriptionListPage() {
    const { can } = useMenus()
    const canCreate = can('/prescriptions', 'create')
    const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS) // ⑤ setFilters

    // ⑤ 주소 값을 검사한 검색 조건. 조회와 검색 줄이 같은 값을 쓴다.
    const status = pickCode<PrescriptionStatus>(filters.status, PRESCRIPTION_STATUSES)
    const prescriptionType = pickCode<PrescriptionType>(filters.prescriptionType, PRESCRIPTION_TYPES)
    const from = pickDate(filters.from)
    const to = pickDate(filters.to)

    const listQuery = $api.useQuery(
        'get',
        '/api/prescriptions',
        // ⑤ 위에서 검사한 값을 그대로 넘긴다 (같은 이름이라 줄여 쓸 수 있다)
        { params: { query: { keyword: filters.keyword, status, prescriptionType, from, to, page: page - 1, size } } },
        // 페이지를 넘길 때 이전 목록을 유지해 표가 깜빡이지 않게 한다.
        { placeholderData: keepPreviousData },
    )

    /** ⑤ 조건 하나만 바꾸고 나머지는 유지한다 (setFilters는 넘기지 않은 조건을 지운다). */
    const updateFilter = (patch: Partial<typeof filters>) => setFilters({ ...filters, ...patch })
    /** 상세 드로어에 보여줄 처방 ID. null이면 닫힘 */
    const [detailId, setDetailId] = useState<number | null>(null)
    const [formOpen, setFormOpen] = useState(false)
    const columns: TableColumnsType<PrescriptionSummaryResponse> = [
        {
            title: '처방전번호',
            dataIndex: 'prescriptionNo',
            width: 160,
            render: (value: string, record) => (
                <Typography.Link className="mono" onClick={() => record.id !== undefined && setDetailId(record.id)}>
                    {value}
                </Typography.Link>
            )
        },
        { title: '환자번호', dataIndex: 'patientNo', width: 120 },
        { title: '환자명', dataIndex: 'patientName', width: 110 },
        { title: '진료과', dataIndex: 'departmentCode', width: 90 },
        { title: '처방의', dataIndex: 'prescriberName', width: 110 },
        {
            title: '구분',
            dataIndex: 'prescriptionType',
            width: 80,
            align: 'center',
            render: (value?: PrescriptionType) =>
                value ? <Tag color={PRESCRIPTION_TYPES[value].color}>{PRESCRIPTION_TYPES[value].label}</Tag> : '-',
        },
        {
            title: '상태',
            dataIndex: 'status',
            width: 90,
            align: 'center',
            render: (value?: PrescriptionStatus) =>
                value ? <Tag color={PRESCRIPTION_STATUSES[value].color}>{PRESCRIPTION_STATUSES[value].label}</Tag> : '-',
        },
        { title: '처방 일시', dataIndex: 'prescribedAt', width: 170, render: (value?: string) => formatDateTime(value) },
    ]

    return (
        <>
            <PageHeader
                title="처방"
                extra={
                    canCreate && (
                        <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormOpen(true)}>
                            처방 등록
                        </Button>
                    )
                }
            />
            <Card>
                {/* ⑤ 검색 줄 */}
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
                    <Select
                        value={prescriptionType}
                        options={TYPE_OPTIONS}
                        placeholder="구분 전체"
                        allowClear
                        className="filter-select-narrow"
                        onChange={(value?: string) => updateFilter({ prescriptionType: value })}
                    />
                    <Space size={4}>
                        <Input
                            type="date"
                            aria-label="처방 시작일"
                            className="date-input"
                            value={from ?? ''}
                            max={to}
                            onChange={(event) => updateFilter({ from: event.target.value || undefined })}
                        />
                        <Typography.Text type="secondary">~</Typography.Text>
                        <Input
                            type="date"
                            aria-label="처방 종료일"
                            className="date-input"
                            value={to ?? ''}
                            min={from}
                            onChange={(event) => updateFilter({ to: event.target.value || undefined })}
                        />
                    </Space>
                    <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
                        초기화
                    </Button>
                </div>

                {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}
                <Table<PrescriptionSummaryResponse>
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

                <PrescriptionDetailDrawer prescriptionId={detailId} onClose={() => setDetailId(null)} />
                <PrescriptionFormModal open={formOpen} onClose={() => setFormOpen(false)} />
            </Card>
        </>
    )
}