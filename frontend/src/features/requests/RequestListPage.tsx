import { ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData, useQueries } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Radio, Select, Table, Tag, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useAuth } from '@/auth/AuthContext'
import { PageHeader } from '@/components/PageHeader'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { formatDateTime } from '@/utils/format'
import { RequestDetailDrawer } from './RequestDetailDrawer'
import {
  REQUEST_STATUSES,
  REQUEST_TARGET_TYPES,
  REQUEST_TYPES,
  summarizeTarget,
  type ChangeRequestResponse,
  type RequestStatus,
  type RequestTargetType,
  type RequestType,
} from './requestFields'

/** URL 쿼리스트링에 두는 검색 조건 이름 */
const FILTER_KEYS = ['status', 'targetType', 'requestType', 'requester'] as const

/** 상태 필터 버튼 순서. 첫 값(undefined)은 전체 */
const STATUS_FILTERS: (RequestStatus | undefined)[] = [undefined, 'REQUESTED', 'APPROVED', 'REJECTED', 'CANCELED']

const TARGET_TYPE_OPTIONS = Object.entries(REQUEST_TARGET_TYPES).map(([value, { label }]) => ({ value, label }))
const REQUEST_TYPE_OPTIONS = Object.entries(REQUEST_TYPES).map(([value, { label }]) => ({ value, label }))

/** 주소의 문자열을 API 파라미터로 바꾼다. 잘못된 값은 조건에서 뺀다. */
function parseFilters(filters: Record<(typeof FILTER_KEYS)[number], string | undefined>) {
  return {
    status: filters.status && filters.status in REQUEST_STATUSES ? (filters.status as RequestStatus) : undefined,
    targetType: filters.targetType && filters.targetType in REQUEST_TARGET_TYPES ? (filters.targetType as RequestTargetType) : undefined,
    requestType: filters.requestType && filters.requestType in REQUEST_TYPES ? (filters.requestType as RequestType) : undefined,
    requester: filters.requester,
  }
}

/**
 * 신청/승인 화면: 표준 데이터(단어·도메인·용어) 변경 신청 목록과 처리.
 *
 * - USER: 서버가 자기 신청만 돌려준다. 대기 중인 신청을 취소할 수 있다.
 * - ADMIN: 모든 신청을 보고 신청자로 거를 수 있다. 상세에서 승인·반려한다.
 * - 상태 버튼에는 다른 조건을 적용한 상태별 건수를 붙인다 (size=1 조회의 totalElements).
 */
export function RequestListPage() {
  const { user } = useAuth()
  const isAdmin = user?.isAdmin ?? false
  const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)
  const query = parseFilters(filters)
  // USER의 신청자 조건은 서버가 무시하므로 보내지 않는다.
  const baseQuery = { ...query, requester: isAdmin ? query.requester : undefined }

  const listQuery = $api.useQuery(
    'get',
    '/api/requests',
    { params: { query: { ...baseQuery, page: page - 1, size } } },
    { placeholderData: keepPreviousData },
  )

  // 상태별 건수. 처리 후 '/api/requests' 무효화로 목록과 함께 갱신된다.
  const countQueries = useQueries({
    queries: STATUS_FILTERS.map((status) =>
      $api.queryOptions('get', '/api/requests', { params: { query: { ...baseQuery, status, size: 1 } } }),
    ),
  })
  const statusOptions = STATUS_FILTERS.map((status, index) => {
    const count = countQueries[index].data?.totalElements
    const label = status ? REQUEST_STATUSES[status].label : '전체'
    return { value: status ?? '', label: count === undefined ? label : `${label} ${count.toLocaleString()}` }
  })

  const [detailId, setDetailId] = useState<number | null>(null)
  /** 검색 조건 하나만 바꾸고 나머지는 유지한다. */
  const updateFilter = (patch: Partial<typeof filters>) => setFilters({ ...filters, ...patch })

  const columns: TableColumnsType<ChangeRequestResponse> = [
    {
      title: '번호',
      dataIndex: 'id',
      width: 80,
      render: (id: number) => <Typography.Link onClick={() => setDetailId(id)}>#{id}</Typography.Link>,
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 80,
      render: (status: RequestStatus) => <Tag color={REQUEST_STATUSES[status].color}>{REQUEST_STATUSES[status].label}</Tag>,
    },
    {
      title: '대상',
      dataIndex: 'targetType',
      width: 90,
      render: (type: RequestTargetType) => <Tag color={REQUEST_TARGET_TYPES[type].color}>{REQUEST_TARGET_TYPES[type].label}</Tag>,
    },
    {
      title: '종류',
      dataIndex: 'requestType',
      width: 80,
      render: (type: RequestType) => <Tag color={REQUEST_TYPES[type].color}>{REQUEST_TYPES[type].label}</Tag>,
    },
    { title: '내용', key: 'summary', width: 200, ellipsis: true, render: (_, request) => summarizeTarget(request) },
    ...(isAdmin
      ? [
          {
            title: '신청자',
            key: 'requester',
            width: 140,
            render: (_: unknown, request: ChangeRequestResponse) => (
              <span>
                {request.requesterName} <Typography.Text type="secondary">({request.requesterUsername})</Typography.Text>
              </span>
            ),
          },
        ]
      : []),
    { title: '신청 사유', dataIndex: 'reason', ellipsis: true },
    { title: '신청 일시', dataIndex: 'createdAt', width: 170, render: (value: string) => formatDateTime(value) },
    {
      title: '처리 일시',
      dataIndex: 'reviewedAt',
      width: 170,
      render: (value?: string) => (value ? formatDateTime(value) : <Typography.Text type="secondary">-</Typography.Text>),
    },
  ]

  return (
    <>
      <PageHeader title={isAdmin ? '신청/승인' : '내 신청'} />

      <Alert
        type="info"
        showIcon
        className="page-notice"
        title={
          isAdmin
            ? '대기 중인 신청을 열어 현재 값과 신청 값을 비교한 뒤 승인하거나 반려하세요. 승인하면 바로 반영됩니다.'
            : '내가 올린 신청만 보입니다. 대기 중인 신청은 상세에서 취소할 수 있습니다.'
        }
      />

      <Card>
        <div className="list-toolbar">
          <Radio.Group
            optionType="button"
            options={statusOptions}
            value={query.status ?? ''}
            onChange={(event) => updateFilter({ status: event.target.value || undefined })}
          />
          <Select
            value={query.targetType}
            options={TARGET_TYPE_OPTIONS}
            placeholder="대상 전체"
            allowClear
            className="filter-select-narrow"
            onChange={(value?: string) => updateFilter({ targetType: value })}
          />
          <Select
            value={query.requestType}
            options={REQUEST_TYPE_OPTIONS}
            placeholder="종류 전체"
            allowClear
            className="filter-select-narrow"
            onChange={(value?: string) => updateFilter({ requestType: value })}
          />
          {isAdmin && (
            <Input.Search
              key={filters.requester ?? ''}
              defaultValue={filters.requester}
              placeholder="신청자 아이디"
              allowClear
              className="filter-select"
              onSearch={(requester) => updateFilter({ requester })}
            />
          )}
          <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
            초기화
          </Button>
        </div>

        {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}

        <Table<ChangeRequestResponse>
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={listQuery.data?.content}
          loading={listQuery.isFetching}
          scroll={{ x: 1100 }}
          locale={{ emptyText: '신청이 없습니다' }}
          onRow={(request) => ({ onClick: () => request.id !== undefined && setDetailId(request.id), className: 'clickable-row' })}
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
      </Card>

      <RequestDetailDrawer requestId={detailId} onClose={() => setDetailId(null)} />
    </>
  )
}
