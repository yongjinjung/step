import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, App, Button, Card, Checkbox, Descriptions, Drawer, Skeleton, Space, Table, Tag, Tooltip, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { ChangeHistoryTimeline } from '@/components/ChangeHistoryTimeline'
import { PageHeader } from '@/components/PageHeader'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { useTabTitle } from '@/layouts/WorkspaceTabsContext'
import { formatDateTime } from '@/utils/format'
import { ColumnDataType, ColumnIssueTags, ColumnPhysicalName } from './columnCells'
import { ColumnFormModal } from './ColumnFormModal'
import { TableFormModal } from './TableFormModal'
import { COLUMN_FIELD_LABELS, TABLE_FIELD_LABELS, type MetaColumnResponse } from './tableFields'
import { useDeleteTable } from './useDeleteTable'

/** 주소의 테이블 ID. 양의 정수가 아니면 undefined */
function parseId(value: string | undefined): number | undefined {
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : undefined
}

/**
 * 테이블 정의 상세: 테이블 정보 · 컬럼 목록(비표준 검출 결과) · 컬럼 추가/수정/삭제 · 변경 이력.
 *
 * 검출 결과는 서버가 조회할 때마다 현재 용어·도메인으로 계산해 준다(standard, issues, expected).
 * 불일치한 값은 빨간 글자로 표시하고, 마우스를 올리면 매핑 용어 기준 표준 값을 보여준다(columnCells).
 */
export function TableDetailPage() {
  const params = useParams()
  const tableId = parseId(params.id)
  const navigate = useNavigate()
  const { message, modal } = App.useApp()
  const invalidate = useInvalidateApi()
  const confirmDeleteTable = useDeleteTable()

  const tableQuery = $api.useQuery('get', '/api/tables/{id}', { params: { path: { id: tableId ?? 0 } } }, { enabled: tableId !== undefined })
  const columnsQuery = $api.useQuery(
    'get',
    '/api/tables/{tableId}/columns',
    { params: { path: { tableId: tableId ?? 0 } } },
    { enabled: tableId !== undefined },
  )
  const table = tableQuery.data
  const allColumns = columnsQuery.data ?? []
  const nonStandardCount = allColumns.filter((column) => !column.standard).length
  // 탭 이름을 테이블 물리명으로 바꾼다 (여러 테이블을 탭으로 열어 둘 때 구분하려고).
  useTabTitle(table?.physicalName)

  const [onlyNonStandard, setOnlyNonStandard] = useState(false)
  const [editingTable, setEditingTable] = useState(false)
  const [formColumn, setFormColumn] = useState<{ open: boolean; column: MetaColumnResponse | null }>({ open: false, column: null })
  const [historyColumn, setHistoryColumn] = useState<MetaColumnResponse | null>(null)

  const deleteColumn = $api.useMutation('delete', '/api/tables/{tableId}/columns/{columnId}')

  const visibleColumns = onlyNonStandard ? allColumns.filter((column) => !column.standard) : allColumns
  const nextSeq = allColumns.reduce((max, column) => Math.max(max, column.seq ?? 0), 0) + 1
  const backToList = () => navigate('/tables')

  const handleDeleteColumn = (column: MetaColumnResponse) => {
    if (tableId === undefined || column.id === undefined) {
      return
    }
    const columnId = column.id
    modal.confirm({
      title: '컬럼을 삭제할까요?',
      content: `${column.physicalName} (${column.logicalName})`,
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        deleteColumn
          .mutateAsync({ params: { path: { tableId, columnId } } })
          .then(() => {
            message.success('컬럼을 삭제했습니다.')
            invalidate('/api/tables/{tableId}/columns', '/api/columns/non-standard', '/api/code-groups/{id}/columns', '/api/histories')
          })
          .catch((error: unknown) => {
            message.error(errorMessage(error))
          }),
    })
  }

  const columns: TableColumnsType<MetaColumnResponse> = [
    { title: '순서', dataIndex: 'seq', width: 70, align: 'center' },
    { title: '물리명', key: 'physicalName', width: 200, render: (_, column) => <ColumnPhysicalName column={column} /> },
    { title: '논리명', dataIndex: 'logicalName', width: 180 },
    { title: '데이터 타입', key: 'dataType', width: 160, render: (_, column) => <ColumnDataType column={column} /> },
    {
      title: 'NULL',
      dataIndex: 'nullable',
      width: 70,
      align: 'center',
      render: (value: boolean) => (value ? 'Y' : <Typography.Text type="secondary">N</Typography.Text>),
    },
    {
      title: 'PK',
      dataIndex: 'primaryKey',
      width: 70,
      align: 'center',
      render: (value: boolean) => (value ? <Tag color="gold">PK</Tag> : null),
    },
    {
      title: '매핑 용어',
      dataIndex: 'termName',
      width: 160,
      render: (value?: string) => value ?? <Typography.Text type="secondary">-</Typography.Text>,
    },
    {
      title: '공통 코드',
      key: 'codeId',
      width: 160,
      // 코드ID를 누르면 코드값 목록이 있는 공통 코드 상세로 이동한다.
      render: (_, column) =>
        column.codeGroupId ? (
          <Tooltip title={column.codeName}>
            <Link to={`/codes/${column.codeGroupId}`} className="mono">
              {column.codeId}
            </Link>
          </Tooltip>
        ) : (
          <Typography.Text type="secondary">-</Typography.Text>
        ),
    },
    { title: '검출 결과', key: 'issues', width: 220, render: (_, column) => <ColumnIssueTags column={column} /> },
    {
      title: '작업',
      key: 'actions',
      width: 150,
      fixed: 'right',
      render: (_, column) => (
        <Space size={0}>
          <Button type="link" size="small" onClick={() => setFormColumn({ open: true, column })}>
            수정
          </Button>
          <Button type="link" size="small" onClick={() => setHistoryColumn(column)}>
            이력
          </Button>
          <Button type="link" size="small" danger onClick={() => handleDeleteColumn(column)}>
            삭제
          </Button>
        </Space>
      ),
    },
  ]

  if (tableId === undefined || tableQuery.isError) {
    return (
      <>
        <PageHeader title="테이블 정의" onBack={backToList} />
        <Alert
          type="error"
          showIcon
          title={tableQuery.isError ? errorMessage(tableQuery.error) : '테이블을 찾을 수 없습니다.'}
          action={<Button onClick={backToList}>목록으로</Button>}
        />
      </>
    )
  }

  return (
    <>
      <PageHeader
        title={table?.physicalName ?? '테이블 정의'}
        subTitle={table?.logicalName}
        onBack={backToList}
        extra={
          table && (
            <Space wrap>
              <Button icon={<EditOutlined />} onClick={() => setEditingTable(true)}>
                테이블 수정
              </Button>
              <Button
                danger
                icon={<DeleteOutlined />}
                onClick={() => confirmDeleteTable(table, { columnCount: columnsQuery.data?.length, onDeleted: backToList })}
              >
                테이블 삭제
              </Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormColumn({ open: true, column: null })}>
                컬럼 추가
              </Button>
            </Space>
          )
        }
      />

      <Card className="page-card">
        {tableQuery.isPending ? (
          <Skeleton active paragraph={{ rows: 2 }} />
        ) : (
          table && (
            <Descriptions column={{ xs: 1, md: 2 }} size="small">
              <Descriptions.Item label={TABLE_FIELD_LABELS.physicalName}>
                <span className="mono">{table.physicalName}</span>
              </Descriptions.Item>
              <Descriptions.Item label={TABLE_FIELD_LABELS.logicalName}>{table.logicalName}</Descriptions.Item>
              <Descriptions.Item label={TABLE_FIELD_LABELS.description} span={{ xs: 1, md: 2 }}>
                {table.description || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="등록 일시">{formatDateTime(table.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="수정 일시">{formatDateTime(table.updatedAt)}</Descriptions.Item>
            </Descriptions>
          )
        )}
      </Card>

      <Card
        className="page-card"
        title={
          <Space size={8} wrap>
            컬럼
            {columnsQuery.isSuccess && (
              <Typography.Text type="secondary" className="card-title-hint">
                {allColumns.length}개 · 표준 {allColumns.length - nonStandardCount} · 비표준 {nonStandardCount}
              </Typography.Text>
            )}
          </Space>
        }
        extra={
          <Checkbox checked={onlyNonStandard} onChange={(event) => setOnlyNonStandard(event.target.checked)}>
            비표준만 보기
          </Checkbox>
        }
      >
        {columnsQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(columnsQuery.error)} />}
        <Table<MetaColumnResponse>
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={visibleColumns}
          loading={columnsQuery.isFetching}
          scroll={{ x: 1460 }}
          // 테이블 하나의 컬럼 전체를 한 번에 받으므로 나누지 않는다.
          pagination={false}
          locale={{ emptyText: onlyNonStandard ? '비표준 컬럼이 없습니다' : '컬럼이 없습니다. 컬럼 추가로 등록하세요.' }}
        />
      </Card>

      <Card className="page-card" title="테이블 변경 이력">
        <ChangeHistoryTimeline targetType="TABLE" targetId={tableId} fieldLabels={TABLE_FIELD_LABELS} />
      </Card>

      <TableFormModal open={editingTable} table={table ?? null} onClose={() => setEditingTable(false)} />

      <ColumnFormModal
        open={formColumn.open}
        tableId={tableId}
        column={formColumn.column}
        nextSeq={nextSeq}
        onClose={() => setFormColumn({ open: false, column: null })}
      />

      <Drawer
        open={historyColumn !== null}
        onClose={() => setHistoryColumn(null)}
        size="large"
        title={historyColumn ? `${historyColumn.physicalName} 변경 이력` : '컬럼 변경 이력'}
      >
        {historyColumn?.id !== undefined && (
          <ChangeHistoryTimeline targetType="COLUMN" targetId={historyColumn.id} fieldLabels={COLUMN_FIELD_LABELS} />
        )}
      </Drawer>
    </>
  )
}
