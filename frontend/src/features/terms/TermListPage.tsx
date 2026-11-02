import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, App, Button, Card, Input, Space, Table, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useMenus } from '@/layouts/MenuContext'
import { DomainSelect } from '@/components/DomainSelect'
import { ExcelActions } from '@/components/ExcelActions'
import { PageHeader } from '@/components/PageHeader'
import { ReasonModal } from '@/components/ReasonModal'
import { WordSelect } from '@/components/WordSelect'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { formatDataType } from '@/utils/dataType'
import { formatDateTime } from '@/utils/format'
import { TermDetailDrawer } from './TermDetailDrawer'
import { TermFormModal } from './TermFormModal'
import type { TermResponse } from './termFields'

/** URL 쿼리스트링에 두는 검색 조건 이름. 도메인 상세의 "외 N건 보기"가 ?domainId=로 연결한다 */
const FILTER_KEYS = ['keyword', 'domainId', 'wordId'] as const

/** 주소의 문자열을 양의 정수 ID로 바꾼다. 잘못된 값은 조건에서 뺀다. */
function parseId(value: string | undefined): number | undefined {
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : undefined
}

/**
 * 표준 용어 화면: 검색(용어명·영문약어, 도메인, 구성 단어) · 목록 · 상세 · 등록/수정 · 삭제 · 엑셀.
 * 구조와 역할별 동작은 표준 단어 화면(features/words)과 같다.
 */
export function TermListPage() {
  const { can } = useMenus()
  // 권한이 있으면 바로 반영하고, 없으면 같은 버튼이 변경 신청이 된다.
  const canCreate = can('/terms', 'create')
  const canUpdate = can('/terms', 'update')
  const canDelete = can('/terms', 'delete')
  const { message, modal } = App.useApp()
  const invalidate = useInvalidateApi()
  const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)
  const query = { keyword: filters.keyword, domainId: parseId(filters.domainId), wordId: parseId(filters.wordId) }

  const listQuery = $api.useQuery(
    'get',
    '/api/terms',
    { params: { query: { ...query, page: page - 1, size } } },
    { placeholderData: keepPreviousData },
  )

  const [formTerm, setFormTerm] = useState<{ open: boolean; term: TermResponse | null }>({ open: false, term: null })
  const [detailId, setDetailId] = useState<number | null>(null)
  const [deleteRequestTarget, setDeleteRequestTarget] = useState<TermResponse | null>(null)

  const deleteTerm = $api.useMutation('delete', '/api/terms/{id}')
  const requestTerm = $api.useMutation('post', '/api/requests/terms')

  const refreshTerms = () => invalidate('/api/terms', '/api/terms/{id}', '/api/histories')
  /** 검색 조건 하나만 바꾸고 나머지는 유지한다. */
  const updateFilter = (patch: Partial<typeof filters>) => setFilters({ ...filters, ...patch })

  const openEdit = (term: TermResponse) => setFormTerm({ open: true, term })

  const handleDelete = (term: TermResponse) => {
    if (term.id === undefined) {
      return
    }
    if (!canDelete) {
      setDeleteRequestTarget(term)
      return
    }
    const id = term.id
    modal.confirm({
      title: '용어를 삭제할까요?',
      content: `${term.name} (${term.abbreviation}) — 컬럼에 매핑된 용어는 삭제할 수 없습니다.`,
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        deleteTerm
          .mutateAsync({ params: { path: { id } } })
          .then(() => {
            message.success('용어를 삭제했습니다.')
            setDetailId(null)
            refreshTerms()
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
    requestTerm.mutate(
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

  const columns: TableColumnsType<TermResponse> = [
    {
      title: '용어명',
      dataIndex: 'name',
      width: 180,
      render: (name: string, term) => (
        <Typography.Link onClick={() => term.id !== undefined && setDetailId(term.id)}>{name}</Typography.Link>
      ),
    },
    { title: '영문약어', dataIndex: 'abbreviation', width: 180, render: (value: string) => <span className="mono">{value}</span> },
    {
      title: '구성 단어',
      key: 'words',
      width: 200,
      ellipsis: true,
      render: (_, term) => term.words?.map((word) => word.name).join(' + '),
    },
    { title: '도메인', dataIndex: 'domainName', width: 140 },
    {
      title: '데이터 타입',
      key: 'dataType',
      width: 140,
      render: (_, term) => <span className="mono">{formatDataType(term.dataType, term.dataLength, term.dataScale)}</span>,
    },
    { title: '설명', dataIndex: 'description', ellipsis: true },
    { title: '수정 일시', dataIndex: 'updatedAt', width: 170, render: (value: string) => formatDateTime(value) },
    {
      title: '작업',
      key: 'actions',
      width: canUpdate && canDelete ? 110 : 150,
      fixed: 'right',
      render: (_, term) => (
        <Space size={0}>
          <Button type="link" size="small" onClick={() => openEdit(term)}>
            {canUpdate ? '수정' : '수정 신청'}
          </Button>
          <Button type="link" size="small" danger onClick={() => handleDelete(term)}>
            {canDelete ? '삭제' : '삭제 신청'}
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title="표준 용어"
        extra={
          <Space wrap>
            <ExcelActions
              target="terms"
              downloadParams={query}
              onUploaded={(result) => (result.mode === 'APPLIED' ? refreshTerms() : invalidate('/api/requests'))}
            />
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormTerm({ open: true, term: null })}>
              {canCreate ? '용어 등록' : '등록 신청'}
            </Button>
          </Space>
        }
      />

      {!(canCreate && canUpdate && canDelete) && (
        <Alert
          type="info"
          showIcon
          className="page-notice"
          title="용어 등록·수정·삭제와 엑셀 업로드는 변경 신청으로 접수되며, 관리자가 승인하면 반영됩니다."
        />
      )}

      <Card>
        <div className="list-toolbar">
          <Input.Search
            key={filters.keyword ?? ''}
            defaultValue={filters.keyword}
            placeholder="용어명, 영문약어"
            allowClear
            className="search-input"
            onSearch={(keyword) => updateFilter({ keyword })}
          />
          <WordSelect
            value={query.wordId}
            onChange={(id) => updateFilter({ wordId: id === undefined ? undefined : String(id) })}
            placeholder="구성 단어 전체"
            allowClear
            className="filter-select"
          />
          <DomainSelect
            value={query.domainId}
            onChange={(id) => updateFilter({ domainId: id === undefined ? undefined : String(id) })}
            placeholder="도메인 전체"
            allowClear
            className="filter-select"
          />
          <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
            초기화
          </Button>
        </div>

        {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}

        <Table<TermResponse>
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={listQuery.data?.content}
          loading={listQuery.isFetching}
          scroll={{ x: 1150 }}
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

      <TermFormModal open={formTerm.open} term={formTerm.term} onClose={() => setFormTerm({ open: false, term: null })} />

      <TermDetailDrawer termId={detailId} onClose={() => setDetailId(null)} onEdit={openEdit} onDelete={handleDelete} />

      <ReasonModal
        open={deleteRequestTarget !== null}
        title="용어 삭제 신청"
        description={
          <Typography.Paragraph>
            <Typography.Text strong>
              {deleteRequestTarget?.name} ({deleteRequestTarget?.abbreviation})
            </Typography.Text>{' '}
            삭제를 신청합니다. 관리자가 승인하면 삭제됩니다.
          </Typography.Paragraph>
        }
        confirmLoading={requestTerm.isPending}
        onSubmit={submitDeleteRequest}
        onCancel={() => setDeleteRequestTarget(null)}
      />
    </>
  )
}
