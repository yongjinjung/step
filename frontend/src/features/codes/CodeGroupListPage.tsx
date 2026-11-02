import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Radio, Space, Table, Tag, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useMenus } from '@/layouts/MenuContext'
import { PageHeader } from '@/components/PageHeader'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { formatDateTime } from '@/utils/format'
import { CodeGroupFormModal } from './CodeGroupFormModal'
import type { CodeGroupResponse } from './codeFields'
import { useDeleteCodeGroup } from './useDeleteCodeGroup'

/** URL 쿼리스트링에 두는 검색 조건 이름 */
const FILTER_KEYS = ['keyword', 'used'] as const

const USED_OPTIONS = [
  { label: '전체', value: '' },
  { label: '사용', value: 'true' },
  { label: '미사용', value: 'false' },
]

/**
 * 공통 코드 그룹 목록: 검색(코드ID·코드명·사용 여부) · 등록/수정 · 삭제.
 *
 * 코드값과 매핑된 컬럼은 상세 화면(/codes/:id)에서 다룬다. 등록하면 바로 상세 화면으로 이동해 코드값을 추가한다.
 * 공통 코드는 신청 대상이 아니라 권한이 있는 사람만 바꾼다. 권한이 없으면 조회만 보여준다(권한 검사는 서버가 한다).
 */
export function CodeGroupListPage() {
  const { can } = useMenus()
  const canCreate = can('/codes', 'create')
  const canUpdate = can('/codes', 'update')
  const canDelete = can('/codes', 'delete')
  const navigate = useNavigate()
  const confirmDelete = useDeleteCodeGroup()
  const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)

  const used = filters.used === 'true' ? true : filters.used === 'false' ? false : undefined
  const listQuery = $api.useQuery(
    'get',
    '/api/code-groups',
    { params: { query: { keyword: filters.keyword, used, page: page - 1, size } } },
    { placeholderData: keepPreviousData },
  )

  const [formGroup, setFormGroup] = useState<{ open: boolean; group: CodeGroupResponse | null }>({ open: false, group: null })

  const columns: TableColumnsType<CodeGroupResponse> = [
    {
      title: '코드ID',
      dataIndex: 'codeId',
      width: 220,
      render: (codeId: string, group) => (
        <Link to={`/codes/${group.id}`} className="mono">
          {codeId}
        </Link>
      ),
    },
    { title: '코드명', dataIndex: 'codeName', width: 200 },
    {
      title: '사용',
      dataIndex: 'used',
      width: 90,
      align: 'center',
      render: (value: boolean) => (value ? <Tag color="green">사용</Tag> : <Tag>미사용</Tag>),
    },
    { title: '설명', dataIndex: 'description', ellipsis: true },
    { title: '수정 일시', dataIndex: 'updatedAt', width: 170, render: (value: string) => formatDateTime(value) },
    ...(canUpdate || canDelete
      ? ([
          {
            title: '작업',
            key: 'actions',
            width: 110,
            fixed: 'right',
            render: (_, group) => (
              <Space size={0}>
                <Button type="link" size="small" onClick={() => setFormGroup({ open: true, group })}>
                  수정
                </Button>
                <Button type="link" size="small" danger onClick={() => confirmDelete(group)}>
                  삭제
                </Button>
              </Space>
            ),
          },
        ] satisfies TableColumnsType<CodeGroupResponse>)
      : []),
  ]

  return (
    <>
      <PageHeader
        title="공통 코드"
        extra={
          canCreate && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormGroup({ open: true, group: null })}>
              코드 그룹 등록
            </Button>
          )
        }
      />

      {!(canCreate && canUpdate && canDelete) && (
        <Alert type="info" showIcon className="page-notice" title="공통 코드를 바꾸려면 공통 코드 메뉴의 등록·수정·삭제 권한이 필요합니다." />
      )}

      <Card>
        <div className="list-toolbar">
          <Input.Search
            key={filters.keyword ?? ''}
            defaultValue={filters.keyword}
            placeholder="코드ID, 코드명"
            allowClear
            className="search-input"
            onSearch={(keyword) => setFilters({ keyword, used: filters.used })}
          />
          <Radio.Group
            optionType="button"
            options={USED_OPTIONS}
            value={filters.used ?? ''}
            onChange={(event) => setFilters({ keyword: filters.keyword, used: event.target.value || undefined })}
          />
          <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
            초기화
          </Button>
        </div>

        {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}

        <Table<CodeGroupResponse>
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={listQuery.data?.content}
          loading={listQuery.isFetching}
          scroll={{ x: 900 }}
          locale={{ emptyText: <Typography.Text type="secondary">코드 그룹이 없습니다</Typography.Text> }}
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

      <CodeGroupFormModal
        open={formGroup.open}
        group={formGroup.group}
        onClose={() => setFormGroup({ open: false, group: null })}
        onSaved={(saved, created) => created && saved.id !== undefined && navigate(`/codes/${saved.id}`)}
      />
    </>
  )
}
