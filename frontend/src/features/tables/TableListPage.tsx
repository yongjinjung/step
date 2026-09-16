import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Space, Table, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { ExcelActions } from '@/components/ExcelActions'
import { PageHeader } from '@/components/PageHeader'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { formatDateTime } from '@/utils/format'
import { TableFormModal } from './TableFormModal'
import type { MetaTableResponse } from './tableFields'
import { useDeleteTable } from './useDeleteTable'

/** URL 쿼리스트링에 두는 검색 조건 이름 */
const FILTER_KEYS = ['keyword'] as const

/**
 * 테이블 정의 목록: 검색(물리명·논리명) · 등록/수정 · 삭제 · 엑셀.
 *
 * 컬럼과 비표준 검출 결과는 상세 화면(/tables/:id)에서 다룬다. 등록하면 바로 상세 화면으로 이동해 컬럼을 추가한다.
 * 표준 사전과 달리 테이블 정의는 USER도 바로 반영하므로(신청 없음) 역할별로 버튼이 달라지지 않는다.
 */
export function TableListPage() {
  const navigate = useNavigate()
  const invalidate = useInvalidateApi()
  const confirmDelete = useDeleteTable()
  const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)

  const listQuery = $api.useQuery(
    'get',
    '/api/tables',
    { params: { query: { keyword: filters.keyword, page: page - 1, size } } },
    { placeholderData: keepPreviousData },
  )

  const [formTable, setFormTable] = useState<{ open: boolean; table: MetaTableResponse | null }>({ open: false, table: null })

  const columns: TableColumnsType<MetaTableResponse> = [
    {
      title: '물리명',
      dataIndex: 'physicalName',
      width: 240,
      render: (name: string, table) => (
        <Link to={`/tables/${table.id}`} className="mono">
          {name}
        </Link>
      ),
    },
    { title: '논리명', dataIndex: 'logicalName', width: 200 },
    { title: '설명', dataIndex: 'description', ellipsis: true },
    { title: '수정 일시', dataIndex: 'updatedAt', width: 170, render: (value: string) => formatDateTime(value) },
    {
      title: '작업',
      key: 'actions',
      width: 110,
      fixed: 'right',
      render: (_, table) => (
        <Space size={0}>
          <Button type="link" size="small" onClick={() => setFormTable({ open: true, table })}>
            수정
          </Button>
          <Button type="link" size="small" danger onClick={() => confirmDelete(table)}>
            삭제
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title="테이블 정의"
        extra={
          <Space wrap>
            <ExcelActions
              target="tables"
              downloadParams={{ keyword: filters.keyword }}
              onUploaded={() => invalidate('/api/tables', '/api/histories', '/api/columns/non-standard')}
            />
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormTable({ open: true, table: null })}>
              테이블 등록
            </Button>
          </Space>
        }
      />

      <Card>
        <div className="list-toolbar">
          <Input.Search
            key={filters.keyword ?? ''}
            defaultValue={filters.keyword}
            placeholder="물리명, 논리명"
            allowClear
            className="search-input"
            onSearch={(keyword) => setFilters({ keyword })}
          />
          <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
            초기화
          </Button>
        </div>

        {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}

        <Table<MetaTableResponse>
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={listQuery.data?.content}
          loading={listQuery.isFetching}
          scroll={{ x: 850 }}
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

      <TableFormModal
        open={formTable.open}
        table={formTable.table}
        onClose={() => setFormTable({ open: false, table: null })}
        onSaved={(saved, created) => created && saved.id !== undefined && navigate(`/tables/${saved.id}`)}
      />
    </>
  )
}
