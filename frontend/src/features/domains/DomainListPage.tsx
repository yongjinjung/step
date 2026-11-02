import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, App, Button, Card, Input, Space, Table, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useMenus } from '@/layouts/MenuContext'
import { ClassifierWordSelect } from '@/components/ClassifierWordSelect'
import { DataTypeSelect } from '@/components/DataTypeSelect'
import { ExcelActions } from '@/components/ExcelActions'
import { PageHeader } from '@/components/PageHeader'
import { ReasonModal } from '@/components/ReasonModal'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { DATA_TYPES, formatDataType, type DataType } from '@/utils/dataType'
import { formatDateTime } from '@/utils/format'
import { DomainDetailDrawer } from './DomainDetailDrawer'
import { DomainFormModal } from './DomainFormModal'
import type { DomainResponse } from './domainFields'

/** URL 쿼리스트링에 두는 검색 조건 이름 */
const FILTER_KEYS = ['keyword', 'classifierWordId', 'dataType'] as const

/** 주소의 문자열을 API 파라미터로 바꾼다. 잘못된 값은 조건에서 뺀다. */
function parseFilters(filters: Record<(typeof FILTER_KEYS)[number], string | undefined>) {
  const classifierWordId = Number(filters.classifierWordId)
  return {
    keyword: filters.keyword,
    classifierWordId: Number.isInteger(classifierWordId) && classifierWordId > 0 ? classifierWordId : undefined,
    dataType: DATA_TYPES.includes(filters.dataType as DataType) ? (filters.dataType as DataType) : undefined,
  }
}

/**
 * 표준 도메인 화면: 검색(도메인명·분류어·데이터 타입) · 목록 · 상세 · 등록/수정 · 삭제 · 엑셀.
 * 구조와 역할별 동작은 표준 단어 화면(features/words)과 같다.
 */
export function DomainListPage() {
  const { can } = useMenus()
  // 권한이 있으면 바로 반영하고, 없으면 같은 버튼이 변경 신청이 된다.
  const canCreate = can('/domains', 'create')
  const canUpdate = can('/domains', 'update')
  const canDelete = can('/domains', 'delete')
  const { message, modal } = App.useApp()
  const invalidate = useInvalidateApi()
  const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)
  const query = parseFilters(filters)

  const listQuery = $api.useQuery(
    'get',
    '/api/domains',
    { params: { query: { ...query, page: page - 1, size } } },
    { placeholderData: keepPreviousData },
  )

  const [formDomain, setFormDomain] = useState<{ open: boolean; domain: DomainResponse | null }>({ open: false, domain: null })
  const [detailId, setDetailId] = useState<number | null>(null)
  const [deleteRequestTarget, setDeleteRequestTarget] = useState<DomainResponse | null>(null)

  const deleteDomain = $api.useMutation('delete', '/api/domains/{id}')
  const requestDomain = $api.useMutation('post', '/api/requests/domains')

  const refreshDomains = () => invalidate('/api/domains', '/api/domains/{id}', '/api/histories')
  /** 검색 조건 하나만 바꾸고 나머지는 유지한다. */
  const updateFilter = (patch: Partial<typeof filters>) => setFilters({ ...filters, ...patch })

  const openEdit = (domain: DomainResponse) => setFormDomain({ open: true, domain })

  const handleDelete = (domain: DomainResponse) => {
    if (domain.id === undefined) {
      return
    }
    if (!canDelete) {
      setDeleteRequestTarget(domain)
      return
    }
    const id = domain.id
    modal.confirm({
      title: '도메인을 삭제할까요?',
      content: `${domain.name} — 용어에서 사용 중이면 삭제할 수 없습니다.`,
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        deleteDomain
          .mutateAsync({ params: { path: { id } } })
          .then(() => {
            message.success('도메인을 삭제했습니다.')
            setDetailId(null)
            refreshDomains()
          })
          .catch((error: unknown) => {
            message.error(errorMessage(error))
          }),
    })
  }

  const submitDeleteRequest = (reason: string | undefined) => {
    if (deleteRequestTarget?.id === undefined) {
      return
    }
    requestDomain.mutate(
      { body: { requestType: 'DELETE', targetId: deleteRequestTarget.id, reason } },
      {
        onSuccess: () => {
          message.success('삭제를 신청했습니다. 관리자가 승인하면 반영됩니다.')
          setDeleteRequestTarget(null)
          invalidate('/api/requests')
        },
        onError: (error: unknown) => message.error(errorMessage(error)),
      },
    )
  }

  const columns: TableColumnsType<DomainResponse> = [
    {
      title: '도메인명',
      dataIndex: 'name',
      width: 180,
      render: (name: string, domain) => (
        <Typography.Link onClick={() => domain.id !== undefined && setDetailId(domain.id)}>{name}</Typography.Link>
      ),
    },
    { title: '분류어', dataIndex: 'classifierWordName', width: 120 },
    {
      title: '데이터 타입',
      key: 'dataType',
      width: 160,
      render: (_, domain) => <span className="mono">{formatDataType(domain.dataType, domain.dataLength, domain.dataScale)}</span>,
    },
    { title: '설명', dataIndex: 'description', ellipsis: true },
    { title: '수정 일시', dataIndex: 'updatedAt', width: 170, render: (value: string) => formatDateTime(value) },
    {
      title: '작업',
      key: 'actions',
      width: canUpdate && canDelete ? 110 : 150,
      fixed: 'right',
      render: (_, domain) => (
        <Space size={0}>
          <Button type="link" size="small" onClick={() => openEdit(domain)}>
            {canUpdate ? '수정' : '수정 신청'}
          </Button>
          <Button type="link" size="small" danger onClick={() => handleDelete(domain)}>
            {canDelete ? '삭제' : '삭제 신청'}
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title="표준 도메인"
        extra={
          <Space wrap>
            <ExcelActions
              target="domains"
              downloadParams={query}
              onUploaded={(result) => (result.mode === 'APPLIED' ? refreshDomains() : invalidate('/api/requests'))}
            />
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormDomain({ open: true, domain: null })}>
              {canCreate ? '도메인 등록' : '등록 신청'}
            </Button>
          </Space>
        }
      />

      {!(canCreate && canUpdate && canDelete) && (
        <Alert
          type="info"
          showIcon
          className="page-notice"
          title="도메인 등록·수정·삭제와 엑셀 업로드는 변경 신청으로 접수되며, 관리자가 승인하면 반영됩니다."
        />
      )}

      <Card>
        <div className="list-toolbar">
          <Input.Search
            key={filters.keyword ?? ''}
            defaultValue={filters.keyword}
            placeholder="도메인명"
            allowClear
            className="search-input"
            onSearch={(keyword) => updateFilter({ keyword })}
          />
          <ClassifierWordSelect
            value={query.classifierWordId}
            onChange={(id) => updateFilter({ classifierWordId: id === undefined ? undefined : String(id) })}
            placeholder="분류어 전체"
            allowClear
            className="filter-select"
          />
          <DataTypeSelect
            value={query.dataType}
            onChange={(dataType) => updateFilter({ dataType })}
            placeholder="데이터 타입 전체"
            allowClear
            className="filter-select"
          />
          <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
            초기화
          </Button>
        </div>

        {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}

        <Table<DomainResponse>
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={listQuery.data?.content}
          loading={listQuery.isFetching}
          scroll={{ x: 900 }}
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

      <DomainFormModal
        open={formDomain.open}
        domain={formDomain.domain}
        onClose={() => setFormDomain({ open: false, domain: null })}
      />

      <DomainDetailDrawer domainId={detailId} onClose={() => setDetailId(null)} onEdit={openEdit} onDelete={handleDelete} />

      <ReasonModal
        open={deleteRequestTarget !== null}
        title="도메인 삭제 신청"
        description={
          <Typography.Paragraph>
            <Typography.Text strong>{deleteRequestTarget?.name}</Typography.Text> 삭제를 신청합니다. 관리자가 승인하면 삭제됩니다.
          </Typography.Paragraph>
        }
        confirmLoading={requestDomain.isPending}
        onSubmit={submitDeleteRequest}
        onCancel={() => setDeleteRequestTarget(null)}
      />
    </>
  )
}
