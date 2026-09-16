import { ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Select, Space, Table, Tag, Tooltip, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { PageHeader } from '@/components/PageHeader'
import { RequestDetailDrawer } from '@/features/requests/RequestDetailDrawer'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { formatDateTime } from '@/utils/format'
import { HistoryChangeDetail } from './HistoryChangeDetail'
import {
  HISTORY_CHANGE_TYPES,
  HISTORY_FIELD_LABELS,
  HISTORY_TARGET_TYPES,
  summarizeHistoryTarget,
  type ChangeHistoryResponse,
  type HistoryChangeType,
  type HistoryTargetType,
} from './historyFields'

/** URL 쿼리스트링에 두는 검색 조건 이름. 다른 화면이 ?requestId=, ?targetType=&targetId=로 연결한다 */
const FILTER_KEYS = ['targetType', 'targetId', 'changeType', 'changedBy', 'requestId', 'from', 'to'] as const

const TARGET_TYPE_OPTIONS = Object.entries(HISTORY_TARGET_TYPES).map(([value, { label }]) => ({ value, label }))
const CHANGE_TYPE_OPTIONS = Object.entries(HISTORY_CHANGE_TYPES).map(([value, { label }]) => ({ value, label }))
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/
/** 대상 이름이 물리명·코드ID라 고정폭 글꼴로 보여주는 대상 */
const MONO_TARGET_TYPES: HistoryTargetType[] = ['TABLE', 'COLUMN', 'CODE_GROUP', 'CODE_VALUE']

function parseId(value: string | undefined): number | undefined {
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : undefined
}

/** 주소의 문자열을 API 파라미터로 바꾼다. 잘못된 값은 조건에서 뺀다. */
function parseFilters(filters: Record<(typeof FILTER_KEYS)[number], string | undefined>) {
  const targetType = filters.targetType && filters.targetType in HISTORY_TARGET_TYPES ? (filters.targetType as HistoryTargetType) : undefined
  return {
    targetType,
    // 대상 ID는 종류와 함께일 때만 의미가 있다 (서버도 둘을 함께 지정하도록 안내한다).
    targetId: targetType ? parseId(filters.targetId) : undefined,
    changeType: filters.changeType && filters.changeType in HISTORY_CHANGE_TYPES ? (filters.changeType as HistoryChangeType) : undefined,
    changedBy: filters.changedBy,
    requestId: parseId(filters.requestId),
    from: filters.from && DATE_PATTERN.test(filters.from) ? filters.from : undefined,
    to: filters.to && DATE_PATTERN.test(filters.to) ? filters.to : undefined,
  }
}

/**
 * 변경 이력 화면: 표준 데이터와 테이블 정의의 변경 전후 값을 조회한다 (최신순).
 *
 * - 대상 이름은 스냅샷에서 꺼내므로 삭제된 대상도 이름으로 보인다. 이름을 누르면 그 대상의 이력만 거른다.
 * - 행을 펼치면 변경 내용(수정은 바뀐 항목, 등록·삭제는 전체 값)을 보여준다.
 * - 신청 승인으로 반영된 변경은 신청 번호를 누르면 신청 상세를 연다.
 */
export function HistoryListPage() {
  const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)
  const query = parseFilters(filters)
  const [requestDetailId, setRequestDetailId] = useState<number | null>(null)

  const listQuery = $api.useQuery(
    'get',
    '/api/histories',
    { params: { query: { ...query, page: page - 1, size } } },
    { placeholderData: keepPreviousData },
  )

  /** 검색 조건 하나만 바꾸고 나머지는 유지한다. */
  const updateFilter = (patch: Partial<typeof filters>) => setFilters({ ...filters, ...patch })

  const columns: TableColumnsType<ChangeHistoryResponse> = [
    { title: '일시', dataIndex: 'changedAt', width: 170, render: (value: string) => formatDateTime(value) },
    {
      title: '대상',
      dataIndex: 'targetType',
      width: 90,
      render: (type: HistoryTargetType) => <Tag color={HISTORY_TARGET_TYPES[type].color}>{HISTORY_TARGET_TYPES[type].label}</Tag>,
    },
    {
      title: '이름',
      key: 'target',
      width: 240,
      render: (_, history) => (
        <Space size={4}>
          <Tooltip title="이 대상의 이력만 보기">
            <Typography.Link
              className={history.targetType && MONO_TARGET_TYPES.includes(history.targetType) ? 'mono' : undefined}
              onClick={() => updateFilter({ targetType: history.targetType, targetId: String(history.targetId) })}
            >
              {summarizeHistoryTarget(history)}
            </Typography.Link>
          </Tooltip>
          <Typography.Text type="secondary">#{history.targetId}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '변경',
      dataIndex: 'changeType',
      width: 80,
      render: (type: HistoryChangeType) => <Tag color={HISTORY_CHANGE_TYPES[type].color}>{HISTORY_CHANGE_TYPES[type].label}</Tag>,
    },
    {
      title: '변경 항목',
      key: 'changedFields',
      ellipsis: true,
      render: (_, history) =>
        history.changeType === 'UPDATE' ? (
          (history.changedFields ?? []).map((field) => HISTORY_FIELD_LABELS[history.targetType ?? 'WORD'][field] ?? field).join(', ')
        ) : (
          <Typography.Text type="secondary">-</Typography.Text>
        ),
    },
    {
      title: '변경자',
      key: 'changedBy',
      width: 160,
      render: (_, history) =>
        history.changedByUsername ? (
          <span>
            {history.changedByName} <Typography.Text type="secondary">({history.changedByUsername})</Typography.Text>
          </span>
        ) : (
          <Typography.Text type="secondary">시스템</Typography.Text>
        ),
    },
    {
      title: '신청',
      dataIndex: 'requestId',
      width: 80,
      render: (requestId?: number) =>
        requestId ? (
          <Typography.Link onClick={() => setRequestDetailId(requestId)}>#{requestId}</Typography.Link>
        ) : (
          <Typography.Text type="secondary">-</Typography.Text>
        ),
    },
  ]

  return (
    <>
      <PageHeader title="변경 이력" />

      <Card>
        <div className="list-toolbar">
          <Select
            value={query.targetType}
            options={TARGET_TYPE_OPTIONS}
            placeholder="대상 전체"
            allowClear
            className="filter-select-narrow"
            // 종류를 바꾸면 이전 종류의 대상 ID 조건은 의미가 없어진다.
            onChange={(value?: string) => updateFilter({ targetType: value, targetId: undefined })}
          />
          <Select
            value={query.changeType}
            options={CHANGE_TYPE_OPTIONS}
            placeholder="변경 전체"
            allowClear
            className="filter-select-narrow"
            onChange={(value?: string) => updateFilter({ changeType: value })}
          />
          <Input.Search
            key={filters.changedBy ?? ''}
            defaultValue={filters.changedBy}
            placeholder="변경자 아이디"
            allowClear
            className="filter-select"
            onSearch={(changedBy) => updateFilter({ changedBy })}
          />
          <Space size={4}>
            <Input
              type="date"
              aria-label="시작일"
              className="date-input"
              value={query.from ?? ''}
              max={query.to}
              onChange={(event) => updateFilter({ from: event.target.value || undefined })}
            />
            <Typography.Text type="secondary">~</Typography.Text>
            <Input
              type="date"
              aria-label="종료일"
              className="date-input"
              value={query.to ?? ''}
              min={query.from}
              onChange={(event) => updateFilter({ to: event.target.value || undefined })}
            />
          </Space>
          <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
            초기화
          </Button>
        </div>

        {(query.targetId !== undefined || query.requestId !== undefined) && (
          <Space size={4} wrap className="active-filters">
            {query.targetType && query.targetId !== undefined && (
              <Tag closable onClose={() => updateFilter({ targetId: undefined })}>
                {HISTORY_TARGET_TYPES[query.targetType].label} #{query.targetId}의 이력
              </Tag>
            )}
            {query.requestId !== undefined && (
              <Tag closable onClose={() => updateFilter({ requestId: undefined })}>
                신청 #{query.requestId}으로 반영된 변경
              </Tag>
            )}
          </Space>
        )}

        {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}

        <Table<ChangeHistoryResponse>
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={listQuery.data?.content}
          loading={listQuery.isFetching}
          scroll={{ x: 1100 }}
          locale={{ emptyText: '변경 이력이 없습니다' }}
          expandable={{ expandedRowRender: (history) => <HistoryChangeDetail history={history} /> }}
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

      <RequestDetailDrawer requestId={requestDetailId} onClose={() => setRequestDetailId(null)} />
    </>
  )
}
