import { ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData, useQueries } from '@tanstack/react-query'
import { Alert, Button, Card, Radio, Table, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { PageHeader } from '@/components/PageHeader'
import { TableSelect } from '@/components/TableSelect'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { formatDataType } from '@/utils/dataType'
import { ColumnDataType, ColumnIssueTags, ColumnPhysicalName } from './columnCells'
import { ColumnFormModal } from './ColumnFormModal'
import { COLUMN_ISSUE_LABELS, type ColumnIssue, type MetaColumnResponse } from './tableFields'

/** URL 쿼리스트링에 두는 검색 조건 이름 */
const FILTER_KEYS = ['tableId', 'issue'] as const

/** 사유 필터 버튼 순서. 첫 값(undefined)은 전체 */
const ISSUE_FILTERS: (ColumnIssue | undefined)[] = [undefined, 'NO_TERM', 'NAME_MISMATCH', 'TYPE_MISMATCH', 'LENGTH_MISMATCH']

/** 주소의 문자열을 API 파라미터로 바꾼다. 잘못된 값은 조건에서 뺀다. */
function parseFilters(filters: Record<(typeof FILTER_KEYS)[number], string | undefined>) {
  const tableId = Number(filters.tableId)
  return {
    tableId: Number.isInteger(tableId) && tableId > 0 ? tableId : undefined,
    issue: filters.issue && filters.issue in COLUMN_ISSUE_LABELS ? (filters.issue as ColumnIssue) : undefined,
  }
}

/**
 * 비표준 컬럼 화면: 모든 테이블에서 표준을 지키지 않는 컬럼을 사유별로 찾고 바로 고친다.
 *
 * - 서버가 조회할 때마다 현재 용어·도메인 기준으로 검출하므로, 컬럼·용어·도메인을 고치면 목록에서 바로 빠진다.
 * - 사유 버튼에는 현재 테이블 조건의 사유별 건수를 붙인다 (size=1 조회의 totalElements).
 * - 컬럼 수정은 테이블 상세와 같은 모달을 쓴다. 테이블 정의는 USER도 바로 반영한다.
 */
export function NonStandardColumnListPage() {
  const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)
  const query = parseFilters(filters)

  const listQuery = $api.useQuery(
    'get',
    '/api/columns/non-standard',
    { params: { query: { ...query, page: page - 1, size } } },
    { placeholderData: keepPreviousData },
  )

  // 사유별 건수. 목록과 같은 경로라 컬럼을 고친 뒤 '/api/columns/non-standard' 무효화로 함께 갱신된다.
  const countQueries = useQueries({
    queries: ISSUE_FILTERS.map((issue) =>
      $api.queryOptions('get', '/api/columns/non-standard', { params: { query: { tableId: query.tableId, issue, size: 1 } } }),
    ),
  })

  const [editingColumn, setEditingColumn] = useState<MetaColumnResponse | null>(null)

  const issueOptions = ISSUE_FILTERS.map((issue, index) => {
    const count = countQueries[index].data?.totalElements
    const label = issue ? COLUMN_ISSUE_LABELS[issue] : '전체'
    return { value: issue ?? '', label: count === undefined ? label : `${label} ${count.toLocaleString()}` }
  })

  const columns: TableColumnsType<MetaColumnResponse> = [
    {
      title: '테이블',
      dataIndex: 'tablePhysicalName',
      width: 200,
      render: (name: string, column) => (
        <Link to={`/tables/${column.tableId}`} className="mono">
          {name}
        </Link>
      ),
    },
    { title: '순서', dataIndex: 'seq', width: 70, align: 'center' },
    { title: '컬럼 물리명', key: 'physicalName', width: 200, render: (_, column) => <ColumnPhysicalName column={column} /> },
    { title: '논리명', dataIndex: 'logicalName', width: 160 },
    { title: '데이터 타입', key: 'dataType', width: 150, render: (_, column) => <ColumnDataType column={column} /> },
    {
      title: '매핑 용어',
      dataIndex: 'termName',
      width: 150,
      render: (value?: string) => value ?? <Typography.Text type="secondary">-</Typography.Text>,
    },
    {
      title: '표준 값',
      key: 'expected',
      width: 240,
      render: (_, { expected }) =>
        expected ? (
          <span className="mono">
            {expected.physicalName} · {formatDataType(expected.dataType, expected.dataLength, expected.dataScale)}
          </span>
        ) : (
          <Typography.Text type="secondary">용어를 매핑하세요</Typography.Text>
        ),
    },
    { title: '사유', key: 'issues', width: 200, render: (_, column) => <ColumnIssueTags column={column} /> },
    {
      title: '작업',
      key: 'actions',
      width: 80,
      fixed: 'right',
      render: (_, column) => (
        <Button type="link" size="small" onClick={() => setEditingColumn(column)}>
          수정
        </Button>
      ),
    },
  ]

  return (
    <>
      <PageHeader title="비표준 컬럼" />

      <Alert
        type="info"
        showIcon
        className="page-notice"
        title="조회할 때마다 현재 용어·도메인 기준으로 검출합니다. 컬럼을 고치거나 용어·도메인을 바꾸면 바로 반영됩니다."
      />

      <Card>
        <div className="list-toolbar">
          <TableSelect
            value={query.tableId}
            onChange={(id) => setFilters({ tableId: id === undefined ? undefined : String(id), issue: filters.issue })}
            placeholder="테이블 전체"
            allowClear
            className="filter-select-wide"
          />
          <Radio.Group
            optionType="button"
            options={issueOptions}
            value={query.issue ?? ''}
            onChange={(event) => setFilters({ tableId: filters.tableId, issue: event.target.value || undefined })}
          />
          <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
            초기화
          </Button>
        </div>

        {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}

        <Table<MetaColumnResponse>
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={listQuery.data?.content}
          loading={listQuery.isFetching}
          scroll={{ x: 1450 }}
          locale={{ emptyText: '비표준 컬럼이 없습니다' }}
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

      <ColumnFormModal
        open={editingColumn !== null}
        tableId={editingColumn?.tableId ?? 0}
        column={editingColumn}
        // 수정만 하므로 쓰이지 않는다.
        nextSeq={editingColumn?.seq ?? 1}
        onClose={() => setEditingColumn(null)}
      />
    </>
  )
}
