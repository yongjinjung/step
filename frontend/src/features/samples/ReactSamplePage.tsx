import { Alert, Button, Checkbox, Input, Space, Spin, Tag, Typography } from 'antd'
import {
  createContext,
  memo,
  useActionState,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useOptimistic,
  useRef,
  useState,
  useTransition,
  type ReactNode,
} from 'react'
import { PageHeader } from '@/components/PageHeader'
import { SampleSection } from './SampleSection'

/**
 * React 19 샘플 화면.
 *
 * 이 프로젝트의 화면을 만들 때 실제로 쓰는 것만 골라, 동작하는 예제와 코드를 함께 둔다.
 * 각 섹션의 데모는 이 파일 안의 작은 컴포넌트이고, 코드 블록은 그 컴포넌트의 핵심 부분이다.
 */
export function ReactSamplePage() {
  return (
    <>
      <PageHeader title="React 19 샘플" subTitle="상태 · 이펙트 · 훅 · React 19 새 기능" />

      <Space orientation="vertical" size="large" className="full-width">
        <Alert
          type="info"
          showIcon
          title="React 19"
          description={
            <>
              화면은 <b>상태(state)를 그리는 함수</b>다. 값을 직접 DOM에 쓰지 않고 상태를 바꾸면 React가 다시 그린다. 아래
              예제는 모두 이 화면에서 실제로 동작하므로, 눌러 보고 바로 아래 코드를 복사해 쓰면 된다.
            </>
          }
        />

        <SampleSection
          title="1. 컴포넌트와 props — 화면을 나누는 단위"
          summary={
            <>
              컴포넌트는 <b>props를 받아 JSX를 돌려주는 함수</b>다. 대문자로 시작하는 이름만 컴포넌트로 인식된다. props는
              읽기 전용이라 컴포넌트 안에서 바꾸지 않는다.
            </>
          }
          points={[
            '하나의 컴포넌트가 화면 절반을 넘어가면 나눈다. 이 프로젝트는 목록/폼 모달/상세 드로어로 나눈다.',
            <>
              여러 자식을 감쌀 때 불필요한 <code>div</code> 대신 프래그먼트(<code>{'<>...</>'}</code>)를 쓴다.
            </>,
            <>
              props 타입은 <code>interface XxxProps</code>로 파일 위쪽에 선언한다.
            </>,
          ]}
          demo={
            <Space wrap>
              <StatusBadge label="표준 단어" tone="success" />
              <StatusBadge label="비표준 컬럼" tone="warning" />
              <StatusBadge label="삭제됨" />
            </Space>
          }
          code={`interface StatusBadgeProps {
  label: string
  /** 색 구분. 값을 union으로 제한하면 오타를 컴파일 단계에서 잡는다 */
  tone?: 'success' | 'warning'
}

function StatusBadge({ label, tone }: StatusBadgeProps) {
  const color = tone === 'success' ? 'green' : tone === 'warning' ? 'orange' : undefined
  return <Tag color={color}>{label}</Tag>
}

// 사용
<StatusBadge label="표준 단어" tone="success" />`}
        />

        <SampleSection
          title="2. useState — 화면이 기억하는 값"
          summary={
            <>
              값이 바뀌면 화면도 바뀌어야 하는 것만 상태로 둔다. <code>setState</code>는 <b>비동기</b>이고, 이전 값을 기준으로
              계산할 때는 반드시 함수형 업데이트(<code>{'setCount(c => c + 1)'}</code>)를 쓴다.
            </>
          }
          points={[
            <>
              같은 렌더 안에서 <code>setCount(count + 1)</code>를 두 번 부르면 1만 증가한다. <code>count</code>가 그 렌더에
              고정된 값이기 때문이다.
            </>,
            '다른 상태로 계산할 수 있는 값은 상태로 두지 않는다(아래 6번 참고).',
            <>
              초기값 계산이 무거우면 <code>{'useState(() => makeInitial())'}</code>처럼 함수로 넘겨 첫 렌더에서만 계산한다.
            </>,
          ]}
          demo={<CounterDemo />}
          code={`function CounterDemo() {
  const [count, setCount] = useState(0)

  return (
    <Space>
      <Button onClick={() => setCount(count - 1)}>-1</Button>
      <b>{count}</b>
      <Button onClick={() => setCount(count + 1)}>+1</Button>

      {/* 이전 값을 기준으로 여러 번 바꿀 때는 함수형 업데이트 */}
      <Button onClick={() => { setCount(c => c + 1); setCount(c => c + 1) }}>+2 (함수형)</Button>

      {/* 이렇게 하면 +1만 된다: count는 이 렌더에 고정된 값 */}
      <Button onClick={() => { setCount(count + 1); setCount(count + 1) }}>+2 시도 (틀린 예)</Button>
    </Space>
  )
}`}
        />

        <SampleSection
          title="3. 목록 렌더링과 불변 업데이트"
          summary={
            <>
              배열은 <code>map</code>으로 그리고, 각 항목에 <b>안정적인 key</b>(보통 서버가 준 id)를 준다. 상태인 배열/객체는
              절대 직접 바꾸지 않고(<code>push</code>, <code>obj.x = 1</code> 금지) 새 값을 만들어 넣는다.
            </>
          }
          points={[
            'key에 배열 인덱스를 쓰면 순서가 바뀌거나 중간이 지워질 때 입력값이 엉뚱한 줄에 남는다.',
            <>
              추가는 <code>{'[...items, next]'}</code>, 삭제는 <code>filter</code>, 수정은 <code>{'map(i => i.id === id ? { ...i, done: true } : i)'}</code>.
            </>,
            '서버 데이터 목록은 상태로 복사하지 말고 TanStack Query 캐시를 그대로 그린다(11번 참고).',
          ]}
          demo={<TodoDemo />}
          code={`interface Todo {
  id: number
  text: string
  done: boolean
}

function TodoDemo() {
  const [todos, setTodos] = useState<Todo[]>([{ id: 1, text: '표준 단어 등록', done: false }])
  const [text, setText] = useState('')

  const add = () => {
    const value = text.trim()
    if (!value) return
    // 새 배열을 만들어 넣는다. todos.push(...)는 화면이 다시 그려지지 않는다.
    setTodos(prev => [...prev, { id: Date.now(), text: value, done: false }])
    setText('')
  }

  const toggle = (id: number) =>
    setTodos(prev => prev.map(todo => (todo.id === id ? { ...todo, done: !todo.done } : todo)))

  const remove = (id: number) => setTodos(prev => prev.filter(todo => todo.id !== id))

  return (
    <>
      <Space.Compact>
        <Input value={text} onChange={e => setText(e.target.value)} onPressEnter={add} placeholder="할 일" />
        <Button type="primary" onClick={add}>추가</Button>
      </Space.Compact>
      <ul>
        {todos.map(todo => (
          // key는 id. 인덱스를 쓰면 삭제·정렬 때 상태가 밀린다.
          <li key={todo.id}>
            <Checkbox checked={todo.done} onChange={() => toggle(todo.id)}>{todo.text}</Checkbox>
            <Button size="small" type="text" danger onClick={() => remove(todo.id)}>삭제</Button>
          </li>
        ))}
      </ul>
    </>
  )
}`}
        />

        <SampleSection
          title="4. 조건부 렌더링 — 로딩 · 오류 · 빈 목록"
          summary={
            <>
              화면은 보통 <b>로딩 / 오류 / 비어 있음 / 정상</b> 네 가지를 그린다. 이 순서로 먼저 걸러 내고 마지막에 정상
              화면을 그리면 조건이 겹치지 않는다.
            </>
          }
          points={[
            <>
              <code>{'{count && <div/>}'}</code>는 count가 0일 때 화면에 <b>0</b>이 찍힌다. <code>{'{count > 0 && ...}'}</code>로 쓴다.
            </>,
            <>
              이 프로젝트 목록 화면은 <code>isPending</code>이면 <code>Skeleton</code>, <code>isError</code>면 <code>Alert</code>,
              그 외에는 <code>Table</code>을 그린다.
            </>,
          ]}
          demo={<ConditionalDemo />}
          code={`function ListArea({ query }: { query: UseQueryResult<WordResponse[]> }) {
  if (query.isPending) return <Skeleton active />
  if (query.isError) return <Alert type="error" showIcon title={errorMessage(query.error)} />
  if (query.data.length === 0) return <Empty description="조회 결과가 없습니다." />
  return <Table dataSource={query.data} rowKey="id" columns={columns} />
}`}
        />

        <SampleSection
          title="5. 제어 컴포넌트 — 입력값도 상태다"
          summary={
            <>
              입력값을 <code>value</code>와 <code>onChange</code>로 상태에 묶으면, 입력 도중에도 다른 곳에서 그 값을 쓰거나
              버튼을 켜고 끌 수 있다.
            </>
          }
          points={[
            <>
              <code>value</code>만 주고 <code>onChange</code>를 빼면 글자가 입력되지 않는다.
            </>,
            <>
              필드가 여러 개인 폼은 상태를 직접 만들지 말고 Ant Design <code>Form</code>을 쓴다(Ant Design 샘플 2번).
            </>,
          ]}
          demo={<ControlledInputDemo />}
          code={`function ControlledInputDemo() {
  const [name, setName] = useState('')
  const trimmed = name.trim()

  return (
    <>
      <Input value={name} onChange={e => setName(e.target.value)} maxLength={20} showCount placeholder="단어명" />
      <Button type="primary" disabled={trimmed.length < 2}>등록</Button>
      {trimmed && <>미리보기: <b>{trimmed}</b></>}
    </>
  )
}`}
        />

        <SampleSection
          title="6. 파생 값은 상태로 두지 않는다"
          summary={
            <>
              다른 상태로 계산할 수 있는 값은 렌더링할 때마다 계산한다. 상태를 두 개로 나누면 <b>동기화 버그</b>가 생긴다.
              계산이 정말 무거울 때만 <code>useMemo</code>로 감싼다.
            </>
          }
          points={[
            <>
              <code>useMemo</code>는 "느려서 넣는" 최적화지, 값이 안 바뀌게 보장하는 장치가 아니다. 먼저 그냥 계산하고,
              느릴 때 넣는다.
            </>,
            <>
              아래 데모는 의존성이 바뀔 때만 계산이 다시 도는 것을 횟수로 보여 준다(입력 글자를 바꿔 보고, 다른 버튼도 눌러
              보라).
            </>,
          ]}
          demo={<DerivedStateDemo />}
          code={`function DerivedStateDemo() {
  const [keyword, setKeyword] = useState('')
  const [tick, setTick] = useState(0)

  // 파생 값: 상태로 따로 두지 않는다
  const matched = useMemo(
    () => WORDS.filter(word => word.includes(keyword.trim())),
    [keyword], // tick이 바뀌어도 다시 계산하지 않는다
  )

  return (
    <>
      <Input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="단어 검색" />
      <Button onClick={() => setTick(t => t + 1)}>관계없는 상태 바꾸기 ({tick})</Button>
      <div>{matched.join(', ') || '결과 없음'}</div>
    </>
  )
}`}
        />

        <SampleSection
          title="7. useEffect — 화면 밖 세상과 맞추기"
          summary={
            <>
              타이머, 구독, 브라우저 API처럼 <b>렌더링 밖에서 일어나는 일</b>에만 쓴다. 서버 데이터 조회는 useEffect로 하지
              않고 TanStack Query를 쓴다.
            </>
          }
          points={[
            <>
              정리 함수(return)에서 반드시 되돌린다. 안 그러면 타이머·구독이 쌓인다.
            </>,
            <>
              개발 모드(<code>StrictMode</code>)에서는 이펙트가 한 번 더 실행된다. 정리 함수가 제대로 있으면 문제가 없다는 걸
              확인시켜 주는 장치다.
            </>,
            <>
              의존성 배열에 쓰는 값은 모두 적는다. 빼려고 <code>eslint-disable</code>을 붙이지 말고 코드를 바꾼다.
            </>,
            <>
              실제 예: <code>src/hooks/useDebouncedValue.ts</code> — 검색어가 멈춘 뒤에만 API를 부른다.
            </>,
          ]}
          demo={<DebouncedSearchDemo />}
          code={`// src/hooks/useDebouncedValue.ts
export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delay)
    // 값이 또 바뀌거나 화면이 사라지면 이전 타이머를 지운다
    return () => window.clearTimeout(timer)
  }, [value, delay])

  return debounced
}

// 사용: 글자마다가 아니라 멈춘 뒤에만 조회한다
const [keyword, setKeyword] = useState('')
const debouncedKeyword = useDebouncedValue(keyword, 300)
const query = $api.useQuery('get', '/api/words', { params: { query: { name: debouncedKeyword } } })`}
        />

        <SampleSection
          title="8. 커스텀 훅 — 화면 로직을 재사용하기"
          summary={
            <>
              <code>use</code>로 시작하는 함수 안에서 다른 훅을 부르면 커스텀 훅이다. 여러 화면이 같은 상태 묶음을 쓸 때
              만든다.
            </>
          }
          points={[
            '컴포넌트 최상단에서만 훅을 부른다. 조건문·반복문·콜백 안에서 부르면 안 된다.',
            <>
              이 프로젝트의 커스텀 훅: <code>useListSearchParams</code>(검색 조건을 URL에), <code>useInvalidateApi</code>(조회
              캐시 무효화), <code>useDebouncedValue</code>, <code>useMenus</code>, <code>useAuth</code>.
            </>,
          ]}
          demo={<ToggleHookDemo />}
          code={`/** 열림/닫힘처럼 두 값을 오가는 상태. 모달·드로어에 쓴다 */
function useToggle(initial = false) {
  const [value, setValue] = useState(initial)
  // 함수를 props로 내려줄 때는 매 렌더 새로 만들지 않도록 useCallback으로 감싼다
  const toggle = useCallback(() => setValue(v => !v), [])
  const open = useCallback(() => setValue(true), [])
  const close = useCallback(() => setValue(false), [])
  return { value, toggle, open, close }
}

// 사용
const modal = useToggle()
<Button onClick={modal.open}>등록</Button>
<Modal open={modal.value} onCancel={modal.close} destroyOnHidden>...</Modal>`}
        />

        <SampleSection
          title="9. Context — 멀리 있는 컴포넌트에 값 전달"
          summary={
            <>
              로그인 사용자, 메뉴 권한처럼 <b>화면 전체가 쓰는 값</b>만 Context로 둔다. props 두세 단계 내리는 정도는 그냥
              내리는 편이 읽기 쉽다.
            </>
          }
          points={[
            <>
              Provider의 <code>value</code>에 객체 리터럴을 바로 넣으면 매 렌더마다 새 객체라 하위가 전부 다시 그려진다.
              <code>useMemo</code>로 감싼다.
            </>,
            <>
              Provider 밖에서 쓰면 바로 알 수 있게, 훅에서 <code>undefined</code>를 검사해 에러를 던진다.
            </>,
            <>
              실제 예: <code>src/auth/AuthContext.tsx</code>, <code>src/layouts/MenuContext.tsx</code>,{' '}
              <code>src/layouts/WorkspaceTabsContext.tsx</code>.
            </>,
          ]}
          demo={<ContextDemo />}
          code={`interface ThemeValue {
  dark: boolean
  toggle: () => void
}

const ThemeContext = createContext<ThemeValue | undefined>(undefined)

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [dark, setDark] = useState(false)
  const toggle = useCallback(() => setDark(d => !d), [])
  // value를 useMemo로 고정해야 dark가 바뀔 때만 하위가 다시 그려진다
  const value = useMemo(() => ({ dark, toggle }), [dark, toggle])
  return <ThemeContext value={value}>{children}</ThemeContext>
}

/** React 19부터 <Context.Provider> 대신 <Context>로 바로 쓸 수 있다 */
export function useTheme() {
  const value = useContext(ThemeContext)
  if (!value) throw new Error('useTheme은 ThemeProvider 안에서만 쓸 수 있습니다.')
  return value
}`}
        />

        <SampleSection
          title="10. memo · useCallback — 다시 그리기 줄이기"
          summary={
            <>
              <code>memo</code>로 감싼 컴포넌트는 props가 그대로면 다시 그리지 않는다. 다만 <b>함수나 객체 props를 매 렌더
              새로 만들면</b> memo가 소용없으므로 <code>useCallback</code>/<code>useMemo</code>와 같이 쓴다.
            </>
          }
          points={[
            '기본은 "감싸지 않는다"이다. 실제로 느린 곳(큰 표, 무거운 차트)만 측정해서 감싼다.',
            '아래 데모에서 왼쪽은 부모가 그려질 때마다 같이 그려지고, 오른쪽(memo + useCallback)은 자기 값이 바뀔 때만 그려진다.',
          ]}
          demo={<MemoDemo />}
          code={`const HeavyRow = memo(function HeavyRow({ label, onPick }: { label: string; onPick: () => void }) {
  renderCount.current += 1 // 데모용
  return <Button onClick={onPick}>{label}</Button>
})

function Parent() {
  const [, setTick] = useState(0)
  // useCallback이 없으면 매 렌더 새 함수가 만들어져 memo가 무력해진다
  const handlePick = useCallback(() => console.log('picked'), [])
  return <HeavyRow label="표준 단어" onPick={handlePick} />
}`}
        />

        <SampleSection
          title="11. React 19 (1) — 폼 액션과 useActionState"
          summary={
            <>
              <code>{'<form action={함수}>'}</code>에 비동기 함수를 주면 React가 <b>제출 중 상태</b>를 관리해 준다.{' '}
              <code>useActionState</code>는 [결과, 액션, 진행중]을 돌려준다.
            </>
          }
          points={[
            <>
              제출 중에는 <code>isPending</code>이 true라 버튼 로딩을 직접 관리하지 않아도 된다.
            </>,
            <>
              이 프로젝트는 서버 호출을 Ant Design <code>Form</code> + <code>$api.useMutation</code>으로 하므로, 폼 액션은
              화면 안에서만 끝나는 간단한 폼에 쓴다.
            </>,
          ]}
          demo={<ActionStateDemo />}
          code={`function ActionStateDemo() {
  const [result, submit, isPending] = useActionState(
    async (_previous: string | null, formData: FormData) => {
      const name = String(formData.get('name') ?? '').trim()
      if (!name) return '이름을 입력하세요.'
      await new Promise(resolve => setTimeout(resolve, 600)) // 서버 호출 흉내
      return name + ' 님으로 저장했습니다.'
    },
    null, // 초기 결과
  )

  return (
    <form action={submit}>
      <Input name="name" placeholder="이름" />
      <Button htmlType="submit" type="primary" loading={isPending}>저장</Button>
      {result && <div>{result}</div>}
    </form>
  )
}`}
        />

        <SampleSection
          title="12. React 19 (2) — useOptimistic · useTransition"
          summary={
            <>
              <code>useOptimistic</code>은 서버 응답을 기다리는 동안 <b>성공했다고 가정한 화면</b>을 먼저 보여 준다.{' '}
              <code>useTransition</code>은 급하지 않은 갱신을 표시해 입력이 끊기지 않게 한다.
            </>
          }
          points={[
            <>
              <code>useOptimistic</code>의 값은 액션이 끝나면 자동으로 실제 상태로 돌아간다. 실패하면 되돌아간 화면을 사용자가
              보게 되므로, 실패 메시지를 꼭 함께 보여 준다.
            </>,
            <>
              <code>startTransition</code> 안의 상태 변경은 우선순위가 낮아진다. 큰 목록을 필터링할 때 입력 반응이 살아 있다.
            </>,
            <>
              React 19부터 <code>ref</code>를 그냥 props로 받을 수 있다(<code>forwardRef</code> 불필요).
            </>,
          ]}
          demo={<OptimisticDemo />}
          code={`function OptimisticDemo() {
  const [memos, setMemos] = useState<string[]>(['첫 번째 메모'])
  // 두 번째 인자: 현재 값 + 보낸 값 → 화면에 먼저 보여 줄 값
  const [shown, addOptimistic] = useOptimistic(memos, (current: string[], adding: string) => [
    ...current,
    adding + ' (저장 중…)',
  ])

  const save = async (formData: FormData) => {
    const value = String(formData.get('memo') ?? '').trim()
    if (!value) return
    addOptimistic(value)                                   // 즉시 화면에 보인다
    await new Promise(resolve => setTimeout(resolve, 1200)) // 서버 호출 흉내
    setMemos(previous => [...previous, value])             // 실제 상태 반영
  }

  return (
    <form action={save}>
      <Input name="memo" placeholder="메모" />
      <Button htmlType="submit">추가</Button>
      <ul>{shown.map(memo => <li key={memo}>{memo}</li>)}</ul>
    </form>
  )
}`}
        />

        <SampleSection
          title="13. 이 프로젝트의 실전 패턴 — 서버 데이터는 TanStack Query로"
          summary={
            <>
              서버에서 받은 데이터는 <code>useState</code>에 복사하지 않는다. <code>$api</code> 훅이 캐시·로딩·오류를 모두
              들고 있고, 저장 뒤에는 관련 경로를 무효화해 다시 불러온다.
            </>
          }
          points={[
            <>
              경로·파라미터·응답 타입이 <code>src/api/schema.d.ts</code>에서 오므로 오타는 컴파일 오류가 된다. 백엔드 API를
              바꾸면 <code>npm run api:generate</code>를 돌린다.
            </>,
            <>
              목록 조회에는 <code>placeholderData: keepPreviousData</code>를 줘서 페이지를 넘길 때 화면이 비지 않게 한다.
            </>,
            <>
              저장 후에는 <code>useInvalidateApi()</code>로 목록·단건·이력 경로를 함께 무효화한다.
            </>,
            <>
              전체 예시는 <code>src/features/words/WordListPage.tsx</code>에 있다.
            </>,
          ]}
          code={`import { keepPreviousData } from '@tanstack/react-query'
import { $api } from '@/api/client'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { useListSearchParams } from '@/hooks/useListSearchParams'

const FILTER_KEYS = ['name', 'abbreviation'] as const // 컴포넌트 밖 상수 (매 렌더 새 배열 금지)

export function WordListPage() {
  const { filters, page, size, setFilters, setPage } = useListSearchParams(FILTER_KEYS)
  const invalidate = useInvalidateApi()

  // 조회: 경로와 query 타입이 schema.d.ts에서 온다
  const listQuery = $api.useQuery(
    'get',
    '/api/words',
    { params: { query: { ...filters, page: page - 1, size } } }, // 화면은 1부터, API는 0부터
    { placeholderData: keepPreviousData },
  )

  // 등록
  const createWord = $api.useMutation('post', '/api/words')

  const submit = (values: WordRequest) =>
    createWord.mutate({ body: values }, {
      onSuccess: () => {
        message.success('등록했습니다.')
        invalidate('/api/words', '/api/histories') // 관련 조회를 다시 불러온다
      },
      onError: (error: unknown) => {
        // 오류 응답은 생성 타입에 없어 unknown으로 받는다
        if (!applyFieldErrors(form, error)) message.error(errorMessage(error))
      },
    })

  return <Table dataSource={listQuery.data?.content} rowKey="id" loading={listQuery.isPending} columns={columns} />
}`}
          codeTitle="src/features/words/WordListPage.tsx (요약)"
        />

        <Alert
          type="success"
          showIcon
          title="다음 단계"
          description={
            <>
              새 화면은 <code>src/features/&#123;기능&#125;/</code>에 목록 페이지 · 폼 모달 · 상세 드로어 · 필드 정의를 만들고,{' '}
              <code>src/layouts/screens.tsx</code>의 <code>PAGES</code>에 메뉴 경로로 등록한다. 등록하지 않은 경로의 메뉴는
              "준비 중" 화면이 나온다.
            </>
          }
        />
      </Space>
    </>
  )
}

/* ------------------------------------------------------------------ */
/* 아래는 각 섹션의 데모 컴포넌트다. 코드 블록에 실린 코드와 같은 내용이다. */
/* ------------------------------------------------------------------ */

interface StatusBadgeProps {
  label: string
  /** 색 구분. 값을 union으로 제한하면 오타를 컴파일 단계에서 잡는다 */
  tone?: 'success' | 'warning'
}

/** 1번 데모: props를 받아 JSX를 돌려주는 가장 단순한 컴포넌트 */
function StatusBadge({ label, tone }: StatusBadgeProps) {
  const color = tone === 'success' ? 'green' : tone === 'warning' ? 'orange' : undefined
  return <Tag color={color}>{label}</Tag>
}

/** 2번 데모: setState의 함수형 업데이트가 왜 필요한지 눌러서 확인한다 */
function CounterDemo() {
  const [count, setCount] = useState(0)

  return (
    <Space wrap align="center">
      <Button onClick={() => setCount(count - 1)}>-1</Button>
      <Typography.Text strong className="sample-number">
        {count}
      </Typography.Text>
      <Button onClick={() => setCount(count + 1)}>+1</Button>
      <Button
        onClick={() => {
          setCount((c) => c + 1)
          setCount((c) => c + 1)
        }}
      >
        +2 (함수형)
      </Button>
      <Button
        onClick={() => {
          setCount(count + 1)
          setCount(count + 1)
        }}
      >
        +2 시도 (틀린 예 → +1)
      </Button>
      <Button type="text" onClick={() => setCount(0)}>
        초기화
      </Button>
    </Space>
  )
}

interface Todo {
  id: number
  text: string
  done: boolean
}

/** 3번 데모: 배열 상태의 추가·수정·삭제를 모두 새 배열로 만든다 */
function TodoDemo() {
  const [todos, setTodos] = useState<Todo[]>([
    { id: 1, text: '표준 단어 등록', done: true },
    { id: 2, text: '테이블 정의 검토', done: false },
  ])
  const [text, setText] = useState('')

  const add = () => {
    const value = text.trim()
    if (!value) {
      return
    }
    setTodos((prev) => [...prev, { id: Date.now(), text: value, done: false }])
    setText('')
  }

  const toggle = (id: number) => setTodos((prev) => prev.map((todo) => (todo.id === id ? { ...todo, done: !todo.done } : todo)))
  const remove = (id: number) => setTodos((prev) => prev.filter((todo) => todo.id !== id))

  return (
    <Space orientation="vertical" className="full-width">
      <Space.Compact className="sample-input-row">
        <Input value={text} onChange={(e) => setText(e.target.value)} onPressEnter={add} placeholder="할 일을 입력하고 Enter" />
        <Button type="primary" onClick={add}>
          추가
        </Button>
      </Space.Compact>
      <ul className="sample-todo-list">
        {todos.map((todo) => (
          <li key={todo.id} className="sample-todo">
            <Checkbox checked={todo.done} onChange={() => toggle(todo.id)}>
              <span className={todo.done ? 'sample-todo-done' : undefined}>{todo.text}</span>
            </Checkbox>
            <Button size="small" type="text" danger onClick={() => remove(todo.id)}>
              삭제
            </Button>
          </li>
        ))}
        {todos.length === 0 && <li className="sample-empty">할 일이 없습니다.</li>}
      </ul>
    </Space>
  )
}

/** 4번 데모: 로딩 → 오류 → 빈 목록 → 정상 순서로 그린다 */
function ConditionalDemo() {
  const [state, setState] = useState<'loading' | 'error' | 'empty' | 'ready'>('ready')

  return (
    <Space orientation="vertical" className="full-width">
      <Space wrap>
        {(['loading', 'error', 'empty', 'ready'] as const).map((value) => (
          <Button key={value} type={state === value ? 'primary' : 'default'} onClick={() => setState(value)}>
            {{ loading: '로딩 중', error: '오류', empty: '결과 없음', ready: '정상' }[value]}
          </Button>
        ))}
      </Space>
      <div className="sample-preview">
        {state === 'loading' ? (
          <Spin />
        ) : state === 'error' ? (
          <Alert type="error" showIcon title="목록을 불러오지 못했습니다." />
        ) : state === 'empty' ? (
          <Typography.Text type="secondary">조회 결과가 없습니다.</Typography.Text>
        ) : (
          <Space wrap>
            <Tag>고객</Tag>
            <Tag>계좌</Tag>
            <Tag>거래</Tag>
          </Space>
        )}
      </div>
    </Space>
  )
}

/** 5번 데모: value + onChange로 입력값을 상태에 묶는다 */
function ControlledInputDemo() {
  const [name, setName] = useState('')
  const trimmed = name.trim()

  return (
    <Space orientation="vertical" className="full-width">
      <Input
        value={name}
        onChange={(e) => setName(e.target.value)}
        maxLength={20}
        showCount
        placeholder="단어명"
        className="sample-input"
      />
      <Space align="center" wrap>
        <Button type="primary" disabled={trimmed.length < 2}>
          등록
        </Button>
        <Typography.Text type="secondary">
          {trimmed.length < 2 ? '2글자 이상 입력해야 등록할 수 있습니다.' : `미리보기: ${trimmed}`}
        </Typography.Text>
      </Space>
    </Space>
  )
}

/** 6번 데모에서 거를 목록 */
const SAMPLE_WORDS = ['고객', '고객명', '계좌', '계좌번호', '거래', '거래일자', '금액']

/** 6번 데모: 파생 값은 상태가 아니라 계산이다. useMemo는 의존성이 바뀔 때만 다시 계산한다 */
function DerivedStateDemo() {
  const [keyword, setKeyword] = useState('')
  const [tick, setTick] = useState(0)
  const [calculations, setCalculations] = useState(0)

  const matched = useMemo(() => {
    // 계산이 실제로 몇 번 도는지 세어 보려고 상태를 올린다. 실무 코드에서는 이렇게 하지 않는다.
    setCalculations((count) => count + 1)
    return SAMPLE_WORDS.filter((word) => word.includes(keyword.trim()))
  }, [keyword])

  return (
    <Space orientation="vertical" className="full-width">
      <Space wrap>
        <Input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="단어 검색 (예: 고객)"
          className="sample-input"
        />
        <Button onClick={() => setTick((t) => t + 1)}>관계없는 상태 바꾸기 ({tick})</Button>
      </Space>
      <Space wrap>
        <Tag color="blue">계산 횟수 {calculations}</Tag>
        <Typography.Text>{matched.length > 0 ? matched.join(', ') : '결과 없음'}</Typography.Text>
      </Space>
      <Typography.Text type="secondary">
        오른쪽 버튼을 아무리 눌러도 계산 횟수가 늘지 않는다 — 의존성(keyword)이 그대로이기 때문이다.
      </Typography.Text>
    </Space>
  )
}

/** 7번 데모: useDebouncedValue와 같은 방식. 입력이 멈춘 뒤에만 "조회"한다 */
function DebouncedSearchDemo() {
  const [keyword, setKeyword] = useState('')
  const [debounced, setDebounced] = useState('')
  const [calls, setCalls] = useState(0)

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebounced(keyword)
      setCalls((count) => count + 1)
    }, 400)
    return () => window.clearTimeout(timer) // 다음 입력이 오면 이전 타이머를 지운다
  }, [keyword])

  return (
    <Space orientation="vertical" className="full-width">
      <Input
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        placeholder="빠르게 입력해 보세요"
        className="sample-input"
      />
      <Space wrap>
        <Tag>입력값: {keyword || '(없음)'}</Tag>
        <Tag color="blue">0.4초 뒤 값: {debounced || '(없음)'}</Tag>
        <Tag color="green">조회 횟수 {calls}</Tag>
      </Space>
    </Space>
  )
}

/** 8번 데모용 커스텀 훅: 열림/닫힘 상태 */
function useToggle(initial = false) {
  const [value, setValue] = useState(initial)
  const toggle = useCallback(() => setValue((v) => !v), [])
  const open = useCallback(() => setValue(true), [])
  const close = useCallback(() => setValue(false), [])
  return { value, toggle, open, close }
}

/** 8번 데모: 훅으로 묶으면 쓰는 쪽이 세 줄로 줄어든다 */
function ToggleHookDemo() {
  const panel = useToggle()

  return (
    <Space orientation="vertical" className="full-width">
      <Space wrap>
        <Button type="primary" onClick={panel.open}>
          열기
        </Button>
        <Button onClick={panel.close}>닫기</Button>
        <Button onClick={panel.toggle}>뒤집기</Button>
      </Space>
      {panel.value && (
        <div className="sample-preview">
          <Typography.Text>열린 영역입니다. 모달·드로어도 같은 방식으로 제어한다.</Typography.Text>
        </div>
      )}
    </Space>
  )
}

interface SampleThemeValue {
  dark: boolean
  toggle: () => void
}

const SampleThemeContext = createContext<SampleThemeValue | undefined>(undefined)

/** 9번 데모: Provider. React 19부터 `<Context>`를 Provider로 바로 쓸 수 있다 */
function SampleThemeProvider({ children }: { children: ReactNode }) {
  const [dark, setDark] = useState(false)
  const toggle = useCallback(() => setDark((d) => !d), [])
  // 객체를 그대로 넘기면 매 렌더 새 값이라 하위가 전부 다시 그려진다.
  const value = useMemo(() => ({ dark, toggle }), [dark, toggle])
  return <SampleThemeContext value={value}>{children}</SampleThemeContext>
}

/** 9번 데모: Provider 밖에서 쓰면 바로 알 수 있게 에러를 던진다 */
function useSampleTheme() {
  const value = useContext(SampleThemeContext)
  if (!value) {
    throw new Error('useSampleTheme은 SampleThemeProvider 안에서만 쓸 수 있습니다.')
  }
  return value
}

function SampleThemeConsumer() {
  const { dark, toggle } = useSampleTheme()
  return (
    <div className={dark ? 'sample-preview sample-preview-dark' : 'sample-preview'}>
      <Space wrap align="center">
        <Typography.Text>현재 테마: {dark ? '어두움' : '밝음'}</Typography.Text>
        <Button size="small" onClick={toggle}>
          바꾸기
        </Button>
      </Space>
    </div>
  )
}

function ContextDemo() {
  return (
    <SampleThemeProvider>
      {/* 중간에 몇 단계가 있어도 props를 내리지 않는다 */}
      <SampleThemeConsumer />
    </SampleThemeProvider>
  )
}

interface SampleRowProps {
  label: string
  onPick: (label: string) => void
}

/** 10번 데모: memo로 감싼 자식. props가 그대로면 다시 그리지 않는다 */
const MemoRow = memo(function MemoRow({ label, onPick }: SampleRowProps) {
  const renders = useRenderCount()
  return (
    <Space>
      <Button size="small" onClick={() => onPick(label)}>
        {label}
      </Button>
      <Tag color="green">렌더 {renders}회</Tag>
    </Space>
  )
})

/** 10번 데모: 감싸지 않은 자식. 부모가 그려질 때마다 같이 그려진다 */
function PlainRow({ label, onPick }: SampleRowProps) {
  const renders = useRenderCount()
  return (
    <Space>
      <Button size="small" onClick={() => onPick(label)}>
        {label}
      </Button>
      <Tag>렌더 {renders}회</Tag>
    </Space>
  )
}

/**
 * 렌더 횟수를 세는 데모용 훅.
 *
 * ref는 값이 바뀌어도 다시 그리지 않으므로 이런 계측에 쓴다(상태로 세면 무한 렌더가 된다).
 * 개발 모드(StrictMode)는 렌더를 한 번 더 하므로 숫자가 2씩 오른다. 비교용으로만 본다.
 */
function useRenderCount() {
  const count = useRef(0)
  count.current += 1
  return count.current
}

function MemoDemo() {
  const [tick, setTick] = useState(0)
  const [picked, setPicked] = useState('-')
  // useCallback이 없으면 매 렌더 새 함수가 만들어져 memo가 무력해진다.
  const handlePick = useCallback((label: string) => setPicked(label), [])

  return (
    <Space orientation="vertical" className="full-width">
      <Space wrap align="center">
        <Button onClick={() => setTick((t) => t + 1)}>부모 다시 그리기 ({tick})</Button>
        <Typography.Text type="secondary">마지막 선택: {picked}</Typography.Text>
      </Space>
      <Space wrap size="large">
        <PlainRow label="그냥 컴포넌트" onPick={handlePick} />
        <MemoRow label="memo + useCallback" onPick={handlePick} />
      </Space>
    </Space>
  )
}

/** 11번 데모: useActionState가 진행 상태까지 관리한다 */
function ActionStateDemo() {
  const [result, submit, isPending] = useActionState(async (_previous: string | null, formData: FormData) => {
    const name = String(formData.get('name') ?? '').trim()
    if (!name) {
      return '이름을 입력하세요.'
    }
    await new Promise((resolve) => setTimeout(resolve, 600)) // 서버 호출 흉내
    return `${name} 님으로 저장했습니다.`
  }, null)

  return (
    <form action={submit}>
      <Space wrap align="center">
        <Input name="name" placeholder="이름" className="sample-input" />
        <Button htmlType="submit" type="primary" loading={isPending}>
          저장
        </Button>
        {result && <Typography.Text type="secondary">{result}</Typography.Text>}
      </Space>
    </form>
  )
}

/** 12번 데모: 저장이 끝나기 전에 목록에 먼저 보여 준다 */
function OptimisticDemo() {
  const [memos, setMemos] = useState<string[]>(['첫 번째 메모'])
  const [shown, addOptimistic] = useOptimistic(memos, (current: string[], adding: string) => [...current, `${adding} (저장 중…)`])
  const [saving, startTransition] = useTransition()

  const save = async (formData: FormData) => {
    const value = String(formData.get('memo') ?? '').trim()
    if (!value) {
      return
    }
    addOptimistic(value) // 즉시 화면에 보인다
    await new Promise((resolve) => setTimeout(resolve, 1200)) // 서버 호출 흉내
    startTransition(() => setMemos((previous) => [...previous, value]))
  }

  return (
    <form action={save}>
      <Space orientation="vertical" className="full-width">
        <Space wrap>
          <Input name="memo" placeholder="메모를 적고 추가" className="sample-input" />
          <Button htmlType="submit">추가</Button>
          {saving && <Tag color="blue">반영 중</Tag>}
        </Space>
        <ul className="plain-list">
          {shown.map((memo) => (
            <li key={memo}>{memo}</li>
          ))}
        </ul>
      </Space>
    </form>
  )
}
