import { keepPreviousData } from '@tanstack/react-query'
import { Alert, App, Button, Empty, Input, Skeleton, Space, Table, Tag, Typography } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { PageHeader } from '@/components/PageHeader'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { SampleSection } from './SampleSection'

/**
 * API 연동(TanStack Query) 샘플 화면.
 *
 * 이 화면의 조회 데모는 실제 백엔드를 부른다(GET만, 읽기 전용이라 안전하다). 등록·수정·삭제 데모는
 * 실제 업무 데이터를 건드리지 않도록 서버 호출을 흉내 낸다 — 코드는 실제와 같고 동작만 흉내다.
 */
export function DataFetchingSamplePage() {
  return (
    <>
      <PageHeader title="API 연동 샘플" subTitle="TanStack Query + openapi-fetch — 이 프로젝트의 $api 훅" />

      <Space orientation="vertical" size="large" className="full-width">
        <Alert
          type="info"
          showIcon
          title="$api 훅"
          description={
            <>
              화면은 fetch를 직접 부르지 않는다. <code>$api.useQuery</code>/<code>$api.useMutation</code>이 로딩·오류·캐시를
              대신 들고 있고, 경로·파라미터·응답 타입은 전부 <code>schema.d.ts</code>에서 온다. 아래 조회 데모는{' '}
              <b>실제로 백엔드의 표준 단어를 가져온다.</b>
            </>
          }
        />

        <SampleSection
          title="1. 목록 조회 — useQuery"
          summary={
            <>
              세 번째 인자로 <code>['get', 경로, 옵션]</code>을 주면 로딩(<code>isPending</code>), 오류(<code>isError</code>
              ), 성공(<code>data</code>)이 모두 따라온다. 컴포넌트가 사라지면 요청도 자동으로 정리된다.
            </>
          }
          points={[
            <>
              첫 인자 <code>'get'</code>·둘째 인자 경로 문자열의 오타는 <b>컴파일 오류</b>가 된다(생성 타입 기준).
            </>,
            <>
              <code>params.query</code>의 키도 백엔드 DTO와 같은 이름·타입이어야 한다.
            </>,
            <>
              같은 경로·같은 파라미터로 두 화면이 동시에 부르면 <b>요청은 한 번만</b> 나가고 결과를 같이 쓴다(캐시 공유).
            </>,
          ]}
          demo={<BasicQueryDemo />}
          code={`const listQuery = $api.useQuery('get', '/api/words', { params: { query: { page: 0, size: 5 } } })

if (listQuery.isPending) return <Skeleton active />
if (listQuery.isError) return <Alert type="error" showIcon title={errorMessage(listQuery.error)} />
return <Table dataSource={listQuery.data.content} rowKey="id" columns={columns} />`}
        />

        <SampleSection
          title="2. 페이지네이션 — placeholderData: keepPreviousData"
          summary={
            <>
              페이지를 넘기면 파라미터가 바뀌어 <b>새 조회</b>가 시작된다. 옵션을 안 주면 그 순간 목록이 비었다가 다시
              채워져 화면이 깜빡인다. <code>keepPreviousData</code>를 주면 새 결과가 올 때까지 이전 페이지를 그대로
              보여준다.
            </>
          }
          points={[
            <>
              <code>isFetching</code>(새로 가져오는 중)과 <code>isPending</code>(데이터가 아예 없음)을 구분한다.
              페이지네이션에는 <code>isFetching</code>을 <code>loading</code>으로 준다.
            </>,
            <>
              이 프로젝트 목록 화면은 전부 이 옵션을 쓴다(<code>WordListPage</code> 등).
            </>,
          ]}
          demo={<PaginationDemo />}
          code={`const listQuery = $api.useQuery(
  'get', '/api/words',
  { params: { query: { page: page - 1, size } } },
  { placeholderData: keepPreviousData },   // 페이지가 바뀌어도 이전 목록을 유지한다
)

<Table loading={listQuery.isFetching} dataSource={listQuery.data?.content} ... />`}
        />

        <SampleSection
          title="3. 서버 검색 + 디바운스"
          summary={
            <>
              입력할 때마다 API를 부르지 않고, <code>useDebouncedValue</code>로 <b>입력이 멈춘 뒤</b>에만 조회한다. 서버
              검색 Select(Ant Design 샘플 5번)도 같은 원리다.
            </>
          }
          points={[
            <>
              <code>enabled</code> 옵션으로 조건부 조회를 켜고 끈다(여기서는 항상 켜져 있고 <code>keyword</code>가 비어도
              전체를 가져온다).
            </>,
            <>
              디바운스 값이 바뀔 때만 <code>useQuery</code>의 파라미터가 바뀌므로, 그때만 실제 네트워크 요청이 나간다.
            </>,
          ]}
          demo={<DebouncedSearchDemo />}
          code={`const [keyword, setKeyword] = useState('')
const debounced = useDebouncedValue(keyword, 300)

const listQuery = $api.useQuery('get', '/api/words', { params: { query: { keyword: debounced, size: 5 } } })

<Input onChange={e => setKeyword(e.target.value)} />   {/* keyword가 아니라 debounced로 조회한다 */}`}
        />

        <SampleSection
          title="4. 단건 조회 — 목록에서 고른 값의 상세"
          summary={
            <>
              <code>enabled: id !== undefined</code>로 값이 있을 때만 조회한다. 목록 조회와 <b>같은 경로·같은 id</b>로
              단건 조회를 부르는 화면이 이미 그 데이터를 캐시에 들고 있으면 다시 부르지 않는다.
            </>
          }
          points={[
            <>
              <code>TermFormModal</code>의 <code>useQueries</code>(줄마다 단건 조회)도 같은 캐시를 공유해서, 이미 목록에서
              본 단어는 다시 요청하지 않는다.
            </>,
          ]}
          demo={<DetailQueryDemo />}
          code={`const [selectedId, setSelectedId] = useState<number>()

const detailQuery = $api.useQuery(
  'get', '/api/words/{id}',
  { params: { path: { id: selectedId ?? 0 } } },
  { enabled: selectedId !== undefined },   // 값이 있을 때만 요청한다
)`}
        />

        <SampleSection
          title="5. 등록·수정·삭제 — useMutation"
          summary={
            <>
              조회와 달리 뮤테이션은 <b>부르는 순간 실행</b>되지 않고 <code>.mutate(...)</code>를 호출할 때 실행된다.{' '}
              <code>onSuccess</code>/<code>onError</code>로 후속 처리를 한다.
            </>
          }
          points={[
            <>
              아래 데모는 실제 업무 데이터를 건드리지 않도록 <b>서버 호출을 흉내</b> 냈다. 코드 블록은 실제로 저장하는
              코드 그대로다.
            </>,
            <>
              오류는 <code>unknown</code>으로 받는다(오류 응답은 API 문서에 없어 생성 타입이 없다). <code>isApiError</code>
              로 좁힌 뒤 <code>error.code</code>로 분기한다.
            </>,
            <>
              <code>createWord.isPending</code>을 저장 버튼의 <code>loading</code>에 그대로 준다.
            </>,
          ]}
          demo={<MutationDemo />}
          code={`const createWord = $api.useMutation('post', '/api/words')

const submit = (values: WordRequest) =>
  createWord.mutate({ body: values }, {
    onSuccess: () => {
      message.success('등록했습니다.')
      invalidate('/api/words', '/api/histories')   // 6번: 관련 조회를 다시 불러온다
    },
    onError: (error: unknown) => {
      if (!applyFieldErrors(form, error)) message.error(errorMessage(error))
    },
  })

<Button type="primary" htmlType="submit" loading={createWord.isPending}>저장</Button>`}
        />

        <SampleSection
          title="6. 캐시 무효화 — useInvalidateApi"
          summary={
            <>
              저장에 성공했다고 화면이 저절로 최신 상태가 되지는 않는다. <b>관련 조회 캐시를 무효화</b>해야 다시
              불러온다. 이 프로젝트는 매번 직접 쓰지 않고 <code>useInvalidateApi()</code>로 통일한다.
            </>
          }
          points={[
            <>
              조회 키가 <code>['get', 경로, 파라미터]</code>라서 <code>['get', 경로]</code>까지만 주면 그 경로의{' '}
              <b>모든 조회</b>(검색 조건이 다른 것 포함)가 함께 무효화된다.
            </>,
            <>
              등록·수정·삭제 뒤에는 목록 경로뿐 아니라 관련 화면 경로도 같이 넘긴다(예:{' '}
              <code>{"invalidate('/api/words', '/api/histories')"}</code> — 이력 화면도 방금 생긴 이력을 보여줘야 하므로).
            </>,
          ]}
          code={`// hooks/useInvalidateApi.ts
export function useInvalidateApi() {
  const queryClient = useQueryClient()
  return useCallback((...apiPaths: (keyof paths)[]) => {
    for (const path of apiPaths) {
      void queryClient.invalidateQueries({ queryKey: ['get', path] })
    }
  }, [queryClient])
}

// 사용
const invalidate = useInvalidateApi()
invalidate('/api/words', '/api/words/{id}', '/api/histories')`}
        />

        <SampleSection
          title="7. 여러 조회를 한 번에 — useQueries"
          summary={
            <>
              배열의 각 항목마다 조회가 필요할 때(용어의 구성 단어 여러 개) <code>useQuery</code>를 반복 호출할 수 없으니
              (훅은 반복문 안에서 부를 수 없다) <code>useQueries</code>로 배열을 통째로 넘긴다.
            </>
          }
          points={[
            <>
              <code>$api.queryOptions(...)</code>가 <code>useQuery</code>와 같은 조회 키를 만들어 캐시를 공유한다.
            </>,
            <>
              실제 예: <code>TermFormModal</code>이 구성 단어 줄마다 단건 조회해 합성 이름 미리보기를 만든다.
            </>,
          ]}
          code={`const wordQueries = useQueries({
  queries: wordIds.map(id => ({
    ...$api.queryOptions('get', '/api/words/{id}', { params: { path: { id: id ?? 0 } } }),
    enabled: id !== undefined,
  })),
})
const words = wordQueries.map(query => query.data)`}
          codeTitle="src/features/terms/TermFormModal.tsx (발췌)"
        />

        <SampleSection
          title="8. 재시도 정책과 오류 처리"
          summary={
            <>
              이 프로젝트는 <b>업무 오류는 재시도하지 않고 네트워크 오류만</b> 재시도한다. 같은 잘못된 요청을 다시 보내
              봤자 같은 오류가 나기 때문이다.
            </>
          }
          points={[
            <>
              <code>isApiError</code>로 백엔드가 준 오류(<code>{'{ code, message, fieldErrors }'}</code>)인지 가린다.
              아니면 네트워크 오류로 보고 재시도 대상이 된다.
            </>,
            <>
              화면 분기는 오류 메시지 문자열이 아니라 <code>error.code</code>(예: <code>DUPLICATE_WORD_NAME</code>)로
              한다. 메시지는 문구가 바뀔 수 있다.
            </>,
          ]}
          code={`// main.tsx
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => !isApiError(error) && failureCount < 2,  // 업무 오류는 재시도 안 함
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
  },
})`}
        />

        <Alert
          type="success"
          showIcon
          title="정리 — 목록 화면 데이터 흐름"
          description={
            <>
              <code>useListSearchParams</code>(URL 상태) → <code>$api.useQuery</code>(조회, <code>keepPreviousData</code>)
              → <code>$api.useMutation</code>(저장) → <code>onSuccess</code>에서 <code>useInvalidateApi()</code>(다시
              불러오기). 자세한 라우팅 부분은 "라우팅과 탭" 샘플의 4번을 함께 본다.
            </>
          }
        />
      </Space>
    </>
  )
}

/* ------------------------------------------------------------------ */
/* 데모 컴포넌트 — 1~4번은 실제 백엔드를 호출한다(GET만, 읽기 전용)         */
/* ------------------------------------------------------------------ */

interface DemoWordRow {
  id?: number
  name?: string
  abbreviation?: string
  classifier?: boolean
}

/** 1번 데모: 실제 GET /api/words 를 부른다 */
function BasicQueryDemo() {
  const listQuery = $api.useQuery('get', '/api/words', { params: { query: { page: 0, size: 5 } } })

  if (listQuery.isPending) {
    return <Skeleton active paragraph={{ rows: 3 }} />
  }
  if (listQuery.isError) {
    return <Alert type="error" showIcon title={errorMessage(listQuery.error, '조회 권한이 없거나 백엔드에 연결할 수 없습니다.')} />
  }
  const rows = (listQuery.data?.content ?? []) as DemoWordRow[]
  if (rows.length === 0) {
    return <Empty description="등록된 단어가 없습니다." />
  }

  return (
    <Space orientation="vertical" className="full-width">
      <Typography.Text type="secondary">실제 백엔드에서 가져온 표준 단어 (최대 5건):</Typography.Text>
      <Space wrap>
        {rows.map((row) => (
          <Tag key={row.id} color="blue">
            {row.name} ({row.abbreviation})
          </Tag>
        ))}
      </Space>
    </Space>
  )
}

/** 2번 데모: 실제 GET, keepPreviousData로 페이지 전환 시 화면이 비지 않는다 */
function PaginationDemo() {
  const [page, setPage] = useState(1)
  const size = 3
  const listQuery = $api.useQuery(
    'get',
    '/api/words',
    { params: { query: { page: page - 1, size } } },
    { placeholderData: keepPreviousData },
  )

  const rows = (listQuery.data?.content ?? []) as DemoWordRow[]
  const total = listQuery.data?.totalElements ?? 0

  return (
    <Space orientation="vertical" className="full-width">
      <Table
        size="small"
        rowKey="id"
        loading={listQuery.isFetching}
        pagination={false}
        dataSource={rows}
        columns={[
          { title: '단어명', dataIndex: 'name' },
          { title: '영문약어', dataIndex: 'abbreviation' },
        ]}
      />
      <Space wrap align="center">
        <Button size="small" disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>
          이전
        </Button>
        <Tag>
          {page}페이지 / 총 {total.toLocaleString()}건
        </Tag>
        <Button size="small" disabled={page * size >= total} onClick={() => setPage((p) => p + 1)}>
          다음
        </Button>
      </Space>
    </Space>
  )
}

/** 3번 데모: 디바운스 후 실제 GET (검색어 조건) */
function DebouncedSearchDemo() {
  const [keyword, setKeyword] = useState('')
  const debounced = useDebouncedValue(keyword, 300)
  const listQuery = $api.useQuery('get', '/api/words', { params: { query: { keyword: debounced || undefined, size: 5 } } })
  const rows = (listQuery.data?.content ?? []) as DemoWordRow[]

  return (
    <Space orientation="vertical" className="full-width">
      <Input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="단어명으로 검색 (예: 고객)" className="sample-input" />
      {listQuery.isFetching ? (
        <Skeleton active paragraph={{ rows: 1 }} />
      ) : listQuery.isError ? (
        <Alert type="error" showIcon title={errorMessage(listQuery.error)} />
      ) : rows.length === 0 ? (
        <Typography.Text type="secondary">검색 결과가 없습니다.</Typography.Text>
      ) : (
        <Space wrap>
          {rows.map((row) => (
            <Tag key={row.id}>{row.name}</Tag>
          ))}
        </Space>
      )}
    </Space>
  )
}

/** 4번 데모: 목록에서 고르면 그 id로 실제 단건 조회를 한다 */
function DetailQueryDemo() {
  const listQuery = $api.useQuery('get', '/api/words', { params: { query: { page: 0, size: 5 } } })
  const [selectedId, setSelectedId] = useState<number>()

  const detailQuery = $api.useQuery(
    'get',
    '/api/words/{id}',
    { params: { path: { id: selectedId ?? 0 } } },
    { enabled: selectedId !== undefined },
  )

  const rows = (listQuery.data?.content ?? []) as DemoWordRow[]

  return (
    <Space orientation="vertical" className="full-width">
      <Space wrap>
        {rows.map((row) => (
          <Button key={row.id} size="small" type={selectedId === row.id ? 'primary' : 'default'} onClick={() => setSelectedId(row.id)}>
            {row.name}
          </Button>
        ))}
        {rows.length === 0 && !listQuery.isPending && <Typography.Text type="secondary">단어가 없습니다.</Typography.Text>}
      </Space>
      {selectedId !== undefined && (
        <div className="sample-preview">
          {detailQuery.isPending ? (
            <Skeleton active paragraph={{ rows: 1 }} />
          ) : detailQuery.isError ? (
            <Alert type="error" showIcon title={errorMessage(detailQuery.error)} />
          ) : (
            <Typography.Text>
              단건 조회 결과: <b>{detailQuery.data?.name}</b> ({detailQuery.data?.abbreviation}) — id {selectedId}
            </Typography.Text>
          )}
        </div>
      )}
    </Space>
  )
}

/** 5번 데모: 서버 호출을 흉내 낸 등록. 실제 /api/words에는 쓰지 않는다(업무 데이터 보호). */
function MutationDemo() {
  const { message } = App.useApp()
  const invalidate = useInvalidateApi()
  const [name, setName] = useState('')
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState<string[]>([])

  const submit = () => {
    const value = name.trim()
    if (!value) {
      return
    }
    setSaving(true)
    // 실제로는 $api.useMutation('post', '/api/words').mutate(...) 가 이 자리에 들어간다.
    window.setTimeout(() => {
      setSaving(false)
      setSaved((prev) => [...prev, value])
      setName('')
      message.success(`'${value}' 등록 (데모 — 실제로 저장하지 않음)`)
      invalidate('/api/words') // 실제 캐시 무효화도 함께 보여준다 (진짜 목록 데모에 반영되지는 않는다)
    }, 500)
  }

  return (
    <Space orientation="vertical" className="full-width">
      <Space.Compact className="sample-input-row">
        <Input value={name} onChange={(e) => setName(e.target.value)} onPressEnter={submit} placeholder="단어명 (데모)" />
        <Button type="primary" loading={saving} onClick={submit}>
          저장
        </Button>
      </Space.Compact>
      {saved.length > 0 && (
        <Space wrap>
          {saved.map((item, index) => (
            <Tag key={index} color="green">
              {item}
            </Tag>
          ))}
        </Space>
      )}
    </Space>
  )
}
