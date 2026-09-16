import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, App, Button, Card, Input, Radio, Space, Table, Tag, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useAuth } from '@/auth/AuthContext'
import { ExcelActions } from '@/components/ExcelActions'
import { PageHeader } from '@/components/PageHeader'
import { ReasonModal } from '@/components/ReasonModal'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { formatDateTime } from '@/utils/format'
import { WordDetailDrawer } from './WordDetailDrawer'
import { WordFormModal } from './WordFormModal'
import type { WordResponse } from './wordFields'

/** URL 쿼리스트링에 두는 검색 조건 이름 (컴포넌트 밖 상수여야 한다: useListSearchParams 참고) */
const FILTER_KEYS = ['keyword', 'classifier'] as const

const CLASSIFIER_OPTIONS = [
  { label: '전체', value: '' },
  { label: '분류어', value: 'true' },
  { label: '일반', value: 'false' },
]

/**
 * 표준 단어 화면: 검색 · 목록 · 상세 · 등록/수정 · 삭제 · 엑셀.
 *
 * ADMIN은 바로 반영하고, USER는 같은 버튼이 변경 신청이 된다(서버 권한 규칙과 같다).
 * 검색 조건과 페이지는 주소(?keyword=&classifier=&page=)에 남아 새로고침해도 유지된다.
 * 이 화면을 도메인·용어·테이블 화면의 기준 구조로 쓴다.
 */
export function WordListPage() {
  const { user } = useAuth()
  const isAdmin = user?.isAdmin ?? false
  const { message, modal } = App.useApp()
  const invalidate = useInvalidateApi()
  const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)

  const classifier = filters.classifier === 'true' ? true : filters.classifier === 'false' ? false : undefined
  const listQuery = $api.useQuery(
    'get',
    '/api/words',
    { params: { query: { keyword: filters.keyword, classifier, page: page - 1, size } } },
    // 페이지를 넘길 때 이전 목록을 유지해 표가 깜빡이지 않게 한다.
    { placeholderData: keepPreviousData },
  )

  const [formWord, setFormWord] = useState<{ open: boolean; word: WordResponse | null }>({ open: false, word: null })
  const [detailId, setDetailId] = useState<number | null>(null)
  const [deleteRequestTarget, setDeleteRequestTarget] = useState<WordResponse | null>(null)

  const deleteWord = $api.useMutation('delete', '/api/words/{id}')
  const requestWord = $api.useMutation('post', '/api/requests/words')

  const refreshWords = () => invalidate('/api/words', '/api/words/{id}', '/api/histories')

  const openCreate = () => setFormWord({ open: true, word: null })
  const openEdit = (word: WordResponse) => setFormWord({ open: true, word })

  /** ADMIN은 확인 후 바로 삭제, USER는 사유를 받아 삭제 신청. */
  const handleDelete = (word: WordResponse) => {
    if (word.id === undefined) {
      return
    }
    if (!isAdmin) {
      setDeleteRequestTarget(word)
      return
    }
    const id = word.id
    modal.confirm({
      title: '단어를 삭제할까요?',
      content: `${word.name} (${word.abbreviation}) — 도메인이나 용어에서 사용 중이면 삭제할 수 없습니다.`,
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      // 실패해도 확인 창은 닫고 사유를 알림으로 보여준다.
      onOk: () =>
        deleteWord
          .mutateAsync({ params: { path: { id } } })
          .then(() => {
            message.success('단어를 삭제했습니다.')
            setDetailId(null)
            refreshWords()
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
    requestWord.mutate(
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

  const columns: TableColumnsType<WordResponse> = [
    {
      title: '단어명',
      dataIndex: 'name',
      width: 160,
      render: (name: string, word) => (
        <Typography.Link onClick={() => word.id !== undefined && setDetailId(word.id)}>{name}</Typography.Link>
      ),
    },
    { title: '영문약어', dataIndex: 'abbreviation', width: 140, render: (value: string) => <span className="mono">{value}</span> },
    { title: '영문명', dataIndex: 'englishName', width: 200, ellipsis: true },
    {
      title: '분류어',
      dataIndex: 'classifier',
      width: 90,
      align: 'center',
      render: (value: boolean) => (value ? <Tag color="geekblue">분류어</Tag> : <Typography.Text type="secondary">-</Typography.Text>),
    },
    { title: '설명', dataIndex: 'description', ellipsis: true },
    { title: '수정 일시', dataIndex: 'updatedAt', width: 170, render: (value: string) => formatDateTime(value) },
    {
      title: '작업',
      key: 'actions',
      width: isAdmin ? 110 : 150,
      fixed: 'right',
      render: (_, word) => (
        <Space size={0}>
          <Button type="link" size="small" onClick={() => openEdit(word)}>
            {isAdmin ? '수정' : '수정 신청'}
          </Button>
          <Button type="link" size="small" danger onClick={() => handleDelete(word)}>
            {isAdmin ? '삭제' : '삭제 신청'}
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title="표준 단어"
        extra={
          <Space wrap>
            <ExcelActions
              target="words"
              downloadParams={{ keyword: filters.keyword, classifier: filters.classifier }}
              onUploaded={(result) => (result.mode === 'APPLIED' ? refreshWords() : invalidate('/api/requests'))}
            />
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
              {isAdmin ? '단어 등록' : '등록 신청'}
            </Button>
          </Space>
        }
      />

      {!isAdmin && (
        <Alert
          type="info"
          showIcon
          className="page-notice"
          title="단어 등록·수정·삭제와 엑셀 업로드는 변경 신청으로 접수되며, 관리자가 승인하면 반영됩니다."
        />
      )}

      <Card>
        <div className="list-toolbar">
          <Input.Search
            // 주소의 검색어가 바뀌면(뒤로 가기 등) 입력란을 다시 만들어 값을 맞춘다.
            key={filters.keyword ?? ''}
            defaultValue={filters.keyword}
            placeholder="단어명, 영문약어, 영문명"
            allowClear
            className="search-input"
            onSearch={(keyword) => setFilters({ keyword, classifier: filters.classifier })}
          />
          <Radio.Group
            optionType="button"
            options={CLASSIFIER_OPTIONS}
            value={filters.classifier ?? ''}
            onChange={(event) => setFilters({ keyword: filters.keyword, classifier: event.target.value || undefined })}
          />
          <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
            초기화
          </Button>
        </div>

        {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}

        <Table<WordResponse>
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={listQuery.data?.content}
          loading={listQuery.isFetching}
          scroll={{ x: 1000 }}
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

      <WordFormModal open={formWord.open} word={formWord.word} onClose={() => setFormWord({ open: false, word: null })} />

      <WordDetailDrawer
        wordId={detailId}
        onClose={() => setDetailId(null)}
        onEdit={openEdit}
        onDelete={handleDelete}
      />

      <ReasonModal
        open={deleteRequestTarget !== null}
        title="단어 삭제 신청"
        description={
          <Typography.Paragraph>
            <Typography.Text strong>
              {deleteRequestTarget?.name} ({deleteRequestTarget?.abbreviation})
            </Typography.Text>{' '}
            삭제를 신청합니다. 관리자가 승인하면 삭제됩니다.
          </Typography.Paragraph>
        }
        confirmLoading={requestWord.isPending}
        onSubmit={submitDeleteRequest}
        onCancel={() => setDeleteRequestTarget(null)}
      />
    </>
  )
}
