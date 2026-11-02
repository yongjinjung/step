import { Alert, Button, Space, Table, Tag, Typography } from 'antd'
import { useState, type ReactNode } from 'react'
import { PageHeader } from '@/components/PageHeader'
import { SampleSection } from './SampleSection'

/**
 * TypeScript 샘플 화면.
 *
 * "타입을 왜 이렇게 쓰는지"를 이 프로젝트의 실제 코드(생성 스키마 타입, 오류 타입 가드, 필드 정의)와 묶어서 보여 준다.
 * 컴파일러 설정은 `tsconfig.app.json`에 있고, `npm run typecheck`로 타입만 검사할 수 있다.
 */
export function TypeScriptSamplePage() {
  return (
    <>
      <PageHeader title="TypeScript 샘플" subTitle="타입 설계 · 제네릭 · 타입 가드 · 생성 타입 활용" />

      <Space orientation="vertical" size="large" className="full-width">
        <Alert
          type="info"
          showIcon
          title="TypeScript 5.9"
          description={
            <>
              타입은 <b>주석이 아니라 검사</b>다. 실행 전에 오타와 빠뜨린 분기를 잡아 준다. 아래 예제는 이 프로젝트에서
              실제로 쓰는 패턴만 골랐고, 코드는 그대로 복사해 써도 컴파일된다.
            </>
          }
        />

        <SampleSection
          title="1. 추론을 믿고, 경계에만 타입을 적는다"
          summary={
            <>
              지역 변수는 대부분 타입을 적지 않아도 된다. <b>함수의 매개변수·반환값, 컴포넌트 props, 외부에서 들어오는
              값</b>처럼 경계에만 적는다.
            </>
          }
          points={[
            <>
              <code>let name = '고객'</code>은 <code>string</code>으로, <code>const name = '고객'</code>은 리터럴 타입{' '}
              <code>'고객'</code>으로 추론된다.
            </>,
            <>
              <code>any</code>는 검사를 끄는 것이라 쓰지 않는다. 모양을 모르면 <code>unknown</code>으로 받고 좁힌다(8번).
            </>,
            <>
              반환 타입은 보통 추론에 맡기되, 공개 함수(여러 화면이 쓰는 유틸)는 적어 두면 실수로 모양이 바뀌는 걸 막는다.
            </>,
          ]}
          code={`// 경계에 적는다
export function formatDateTime(value?: string | null): string {
  if (!value) return '-'
  return new Date(value).toLocaleString('ko-KR')
}

// 안쪽은 추론에 맡긴다
const parts = value.split('-')   // string[]
const year = Number(parts[0])    // number

// 외부에서 오는 값은 unknown으로 받는다 (any 금지)
function handleError(error: unknown) { /* ... */ }`}
        />

        <SampleSection
          title="2. interface와 type — 언제 무엇을"
          summary={
            <>
              객체 모양은 <code>interface</code>, 그 밖(유니온·함수·유틸리티 조합)은 <code>type</code>을 쓴다. 이 프로젝트는
              props를 <code>interface XxxProps</code>로 쓰고, API DTO는 생성된 <code>type</code>을 가져다 쓴다.
            </>
          }
          points={[
            <>
              <code>interface</code>는 같은 이름으로 다시 선언하면 합쳐진다(선언 병합). 라이브러리 타입을 늘릴 때 쓴다.
            </>,
            <>
              <code>type</code>은 유니온(<code>{"'A' | 'B'"}</code>), 교차(<code>&</code>), 매핑 타입을 만들 수 있다.
            </>,
            '읽기 전용으로 두면 실수로 바꾸는 코드가 컴파일 오류가 된다.',
          ]}
          code={`interface WordFormProps {
  /** 수정할 단어. null이면 등록 모드 */
  word: WordResponse | null
  onClose: () => void
}

/** 유니온·교차는 type */
type RequestStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'CANCELED'
type WordFormValues = WordRequest & { reason?: string }

/** 바꾸지 못하게 */
interface Column {
  readonly id: number
  readonly name: string
}`}
        />

        <SampleSection
          title="3. 판별 유니온 — 분기를 빠뜨리지 않게"
          summary={
            <>
              공통 필드(<code>status</code>, <code>type</code>)로 구분되는 여러 모양을 유니온으로 묶으면, 분기할 때 각 갈래의
              필드만 쓸 수 있고 <b>빠뜨린 갈래는 컴파일 오류</b>가 된다.
            </>
          }
          points={[
            <>
              <code>switch</code>의 <code>default</code>에서 <code>never</code>에 대입하면, 새 갈래를 추가했을 때 여기서
              오류가 난다. 백엔드 enum(<code>TargetType</code> 등)이 늘어난 걸 놓치지 않는 방법이다.
            </>,
            <>
              아래 데모는 <code>ListState</code> 유니온 하나로 로딩/오류/성공 화면을 그린다. 성공일 때만{' '}
              <code>items</code>를, 오류일 때만 <code>message</code>를 쓸 수 있다.
            </>,
          ]}
          demo={<DiscriminatedUnionDemo />}
          code={`type ListState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'success'; items: string[] }

function render(state: ListState): ReactNode {
  switch (state.status) {
    case 'loading':
      return <Spin />
    case 'error':
      return <Alert type="error" title={state.message} />  // items는 여기서 쓸 수 없다
    case 'success':
      return <ul>{state.items.map(item => <li key={item}>{item}</li>)}</ul>
    default: {
      // 갈래를 추가하고 case를 빠뜨리면 여기서 컴파일 오류가 난다
      const unreachable: never = state
      return unreachable
    }
  }
}`}
        />

        <SampleSection
          title="4. 제네릭 — 타입을 잃지 않고 재사용하기"
          summary={
            <>
              들어온 타입을 그대로 돌려줘야 하는 함수·컴포넌트에 쓴다. <code>any</code>를 쓰면 타입이 사라지지만 제네릭은
              호출한 쪽의 타입을 유지한다.
            </>
          }
          points={[
            <>
              <code>{'<T extends { id: number }>'}</code>처럼 제약을 걸면 안에서 그 필드를 쓸 수 있다.
            </>,
            <>
              <code>.tsx</code> 파일에서 화살표 함수의 제네릭은 JSX와 헷갈리므로 <code>{'<T,>'}</code>처럼 쉼표를 붙이거나{' '}
              <code>function</code> 선언을 쓴다.
            </>,
            <>
              실제 예: <code>useDebouncedValue&lt;T&gt;</code>, <code>useListSearchParams&lt;K extends string&gt;</code>.
            </>,
          ]}
          demo={<GenericDemo />}
          code={`/** id로 묶어 보기 쉬운 Map을 만든다. 돌려주는 값이 입력 타입을 그대로 유지한다 */
function indexById<T extends { id: number }>(items: T[]): Map<number, T> {
  return new Map(items.map(item => [item.id, item]))
}

const words = indexById(wordList)
words.get(1)?.name // T가 WordResponse라 name을 쓸 수 있다

/** 제네릭 컴포넌트 */
interface PickListProps<T> {
  items: T[]
  labelOf: (item: T) => string
  onPick: (item: T) => void
}

function PickList<T>({ items, labelOf, onPick }: PickListProps<T>) {
  return items.map((item, index) => (
    <Button key={index} onClick={() => onPick(item)}>{labelOf(item)}</Button>
  ))
}`}
        />

        <SampleSection
          title="5. 유틸리티 타입 — 이미 있는 타입에서 만들어 쓰기"
          summary={
            <>
              같은 모양을 두 번 적지 않는다. 응답 타입에서 필요한 필드만 뽑거나(<code>Pick</code>), 일부만 있는 값을
              표현하거나(<code>Partial</code>), 키가 정해진 사전을 만든다(<code>Record</code>).
            </>
          }
          points={[
            <>
              <code>Record&lt;RequestStatus, &#123; label: string; color: string &#125;&gt;</code>로 만들면 상태를 하나
              빠뜨렸을 때 컴파일 오류가 난다. 이 프로젝트의 <code>REQUEST_STATUSES</code>가 그 방식이다.
            </>,
            <>
              <code>NonNullable</code>은 생성 타입의 <code>| undefined</code>를 떼어 낼 때 유용하다(백엔드 응답 필드는 대부분
              선택 필드로 생성된다).
            </>,
          ]}
          demo={<UtilityTypeDemo />}
          code={`type WordResponse = components['schemas']['WordResponse']

type WordSummary = Pick<WordResponse, 'id' | 'name'>          // 필요한 필드만
type WordPatch = Partial<WordRequest>                          // 일부만 바꿀 때
type WithoutAudit = Omit<WordResponse, 'createdAt' | 'updatedAt'>
type StatusMeta = Record<RequestStatus, { label: string; color: string }>
type Word = NonNullable<WordResponse['name']>                  // undefined 제거

/** 키를 빠뜨리면 컴파일 오류가 난다 */
export const REQUEST_STATUSES: StatusMeta = {
  REQUESTED: { label: '대기', color: 'blue' },
  APPROVED: { label: '승인', color: 'green' },
  REJECTED: { label: '반려', color: 'red' },
  CANCELED: { label: '취소', color: 'default' },
}`}
        />

        <SampleSection
          title="6. as const와 satisfies"
          summary={
            <>
              <code>as const</code>는 값을 리터럴 타입으로 굳혀 유니온을 만들 때 쓰고, <code>satisfies</code>는{' '}
              <b>타입은 검사하되 좁은 추론은 남긴다</b>.
            </>
          }
          points={[
            <>
              <code>const KEYS = ['name', 'abbreviation'] as const</code> → <code>readonly ['name', 'abbreviation']</code>.{' '}
              <code>typeof KEYS[number]</code>로 유니온을 얻는다.
            </>,
            <>
              <code>: StatusMeta</code>로 적으면 키가 검사되지만 값의 구체 타입을 잃는다. <code>satisfies StatusMeta</code>는
              검사도 하고 <code>colors.APPROVED.color</code>가 <code>'green'</code> 리터럴로 남는다.
            </>,
            <>
              이 프로젝트의 <code>FILTER_KEYS</code>가 <code>as const</code>를 쓰는 이유는, 훅이{' '}
              <code>K extends string</code>으로 받아 <code>filters.name</code>처럼 키를 검사해 주기 때문이다.
            </>,
          ]}
          demo={<AsConstDemo />}
          code={`const FILTER_KEYS = ['name', 'abbreviation'] as const
type FilterKey = (typeof FILTER_KEYS)[number] // 'name' | 'abbreviation'

const { filters } = useListSearchParams(FILTER_KEYS)
filters.name          // OK
filters.nmae          // 컴파일 오류 (오타를 잡는다)

const TONE = {
  APPROVED: { color: 'green' },
  REJECTED: { color: 'red' },
} satisfies Record<string, { color: string }>

TONE.APPROVED.color   // 'green' 리터럴로 남는다`}
        />

        <SampleSection
          title="7. keyof · typeof · 인덱스 접근"
          summary={
            <>
              이미 있는 값이나 타입에서 새 타입을 끌어낸다. 상수 객체를 하나 고치면 관련 타입이 모두 따라오게 만드는 방법이다.
            </>
          }
          points={[
            <>
              <code>typeof 값</code>은 값에서 타입을 얻고, <code>keyof 타입</code>은 키의 유니온을 얻는다.
            </>,
            <>
              <code>타입['필드']</code>로 필드 타입을, <code>배열타입[number]</code>로 원소 타입을 꺼낸다.
            </>,
          ]}
          code={`const HISTORY_FIELD_LABELS = {
  name: '단어명',
  abbreviation: '영문약어',
} as const

type HistoryField = keyof typeof HISTORY_FIELD_LABELS   // 'name' | 'abbreviation'
type Label = (typeof HISTORY_FIELD_LABELS)[HistoryField] // '단어명' | '영문약어'

type PageResponse = components['schemas']['PageResponseWordResponse']
type WordRow = NonNullable<PageResponse['content']>[number] // 목록 한 줄의 타입`}
        />

        <SampleSection
          title="8. 타입 가드 — unknown을 안전하게 좁히기"
          summary={
            <>
              네트워크 오류, 라이브러리가 던진 값처럼 모양을 모르는 값은 <code>unknown</code>으로 받고{' '}
              <code>값 is 타입</code> 함수로 좁힌다. 이 프로젝트의 <code>isApiError</code>가 그 예다.
            </>
          }
          points={[
            <>
              오류 응답은 API 문서에 선언돼 있지 않아 생성 타입이 <code>never</code>다. 그래서{' '}
              <code>onError: (error: unknown) =&gt; ...</code>로 받는다.
            </>,
            <>
              분기는 메시지가 아니라 <code>error.code</code>(백엔드 <code>ErrorCode</code> 이름)로 한다. 메시지는 바뀔 수 있다.
            </>,
            <>
              실제 코드: <code>src/api/errors.ts</code>.
            </>,
          ]}
          demo={<TypeGuardDemo />}
          code={`// src/api/errors.ts
export interface ApiError {
  /** ErrorCode 이름. 화면 분기는 메시지 대신 이 값으로 한다 */
  code: string
  message: string
  fieldErrors: FieldErrorDetail[]
}

export function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ApiError).code === 'string' &&
    typeof (value as ApiError).message === 'string'
  )
}

export function errorMessage(error: unknown, fallback = '요청을 처리하지 못했습니다.'): string {
  return isApiError(error) ? error.message : fallback // 여기서 error는 ApiError로 좁혀진다
}`}
        />

        <SampleSection
          title="9. null과 undefined — strict가 잡아 주는 것"
          summary={
            <>
              <code>strict</code>가 켜져 있어 <code>null</code>일 수 있는 값을 그냥 쓰면 컴파일 오류가 난다. 옵셔널 체이닝
              (<code>?.</code>)과 널 병합(<code>??</code>)으로 다룬다.
            </>
          }
          points={[
            <>
              <code>||</code>는 <code>0</code>, <code>''</code>도 기본값으로 바꿔 버린다. 값이 없을 때만 대체하려면{' '}
              <code>??</code>를 쓴다.
            </>,
            <>
              생성 타입의 응답 필드는 대부분 선택 필드라 <code>data?.content ?? []</code> 형태를 자주 쓴다.
            </>,
            <>
              <code>!</code>(non-null 단언)은 "내가 보장한다"는 뜻이라 꼭 필요할 때만 쓴다(<code>document.getElementById('root')!</code>).
            </>,
          ]}
          code={`const rows = listQuery.data?.content ?? []     // 없으면 빈 배열
const total = listQuery.data?.totalElements ?? 0
const size = Number(searchParams.get('size')) || 20 // 0·NaN도 기본값으로 → 여기서는 || 가 맞다

// 옵셔널 체이닝은 호출·인덱스에도 쓴다
const first = rows[0]?.name
onChange?.(value)`}
        />

        <SampleSection
          title="10. React와 함께 쓰기"
          summary={<>props, 상태, 이벤트 타입만 알면 화면 코드의 대부분이 해결된다.</>}
          points={[
            <>
              자식으로 아무거나 받을 때는 <code>ReactNode</code>, 컴포넌트 자체를 받을 때는 <code>ComponentType</code>.
            </>,
            <>
              <code>useState</code>는 초기값에서 추론된다. <code>null</code>로 시작하면{' '}
              <code>useState&lt;Word | null&gt;(null)</code>처럼 제네릭을 적는다.
            </>,
            <>
              <code>verbatimModuleSyntax</code>가 켜져 있어 <b>타입만 가져올 때는 반드시 <code>import type</code></b>을 쓴다.
              안 그러면 빌드 오류가 난다.
            </>,
          ]}
          code={`import { useState, type ChangeEvent, type ReactNode } from 'react'
import type { ColumnsType } from 'antd/es/table'
import type { components } from '@/api/schema'

type WordResponse = components['schemas']['WordResponse']

interface PanelProps {
  title: string
  children: ReactNode            // JSX, 문자열, 숫자, 배열 모두 가능
  extra?: ReactNode
}

const [editing, setEditing] = useState<WordResponse | null>(null) // null로 시작하면 제네릭을 적는다

const onChange = (event: ChangeEvent<HTMLInputElement>) => setKeyword(event.target.value)

const columns: ColumnsType<WordResponse> = [
  { title: '단어명', dataIndex: 'name' },   // dataIndex 오타를 잡아 준다
]`}
        />

        <SampleSection
          title="11. 이 프로젝트 규칙 — 생성 타입만 쓴다"
          summary={
            <>
              백엔드 OpenAPI 문서에서 <code>src/api/schema.d.ts</code>를 생성한다. 화면은 DTO 타입을 손으로 적지 않고 여기서
              가져온다. 백엔드가 바뀌면 <b>타입 오류가 곧 영향받는 화면 목록</b>이 된다.
            </>
          }
          points={[
            <>
              백엔드를 띄운 뒤 <code>npm run api:generate</code>를 돌리고, <code>openapi/api-docs.json</code>과{' '}
              <code>schema.d.ts</code>를 함께 커밋한다. <code>schema.d.ts</code>는 직접 고치지 않는다.
            </>,
            <>
              <code>tsconfig.app.json</code>의 주요 플래그: <code>strict</code>, <code>noUnusedLocals</code>,{' '}
              <code>noUnusedParameters</code>, <code>verbatimModuleSyntax</code>, <code>erasableSyntaxOnly</code>(
              <code>enum</code>·생성자 프로퍼티 금지 — 유니온 상수로 대신한다).
            </>,
            <>
              타입만 검사하려면 <code>npm run typecheck</code>, 빌드는 <code>npm run build</code>(<code>tsc -b</code> 후 vite
              빌드).
            </>,
          ]}
          code={`import type { components } from '@/api/schema'

type WordResponse = components['schemas']['WordResponse']
type WordRequest = components['schemas']['WordRequest']

// 경로·쿼리·본문·응답 타입이 모두 schema.d.ts에서 온다
const query = $api.useQuery('get', '/api/words', { params: { query: { name: '고객', page: 0, size: 20 } } })
const create = $api.useMutation('post', '/api/words')

create.mutate({ body: { name: '고객', abbreviation: 'CUST' } satisfies WordRequest })`}
          codeTitle="src/features/words/wordFields.ts 등에서 쓰는 방식"
        />
      </Space>
    </>
  )
}

/* ------------------------------------------------------------------ */
/* 데모 컴포넌트                                                        */
/* ------------------------------------------------------------------ */

/** 3번 데모에서 쓰는 판별 유니온. status 값으로 갈래가 결정된다 */
type ListState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'success'; items: string[] }

const LIST_STATES: ListState[] = [
  { status: 'loading' },
  { status: 'error', message: '목록을 불러오지 못했습니다.' },
  { status: 'success', items: ['고객', '계좌', '거래'] },
]

function renderListState(state: ListState): ReactNode {
  switch (state.status) {
    case 'loading':
      return <Typography.Text type="secondary">불러오는 중…</Typography.Text>
    case 'error':
      // 이 갈래에서는 items를 쓸 수 없다. 타입이 막아 준다.
      return <Alert type="error" showIcon title={state.message} />
    case 'success':
      return (
        <Space wrap>
          {state.items.map((item) => (
            <Tag key={item} color="blue">
              {item}
            </Tag>
          ))}
        </Space>
      )
    default: {
      // 갈래를 늘리고 case를 빠뜨리면 여기서 컴파일 오류가 난다.
      const unreachable: never = state
      return unreachable
    }
  }
}

function DiscriminatedUnionDemo() {
  const [index, setIndex] = useState(0)
  const state = LIST_STATES[index]

  return (
    <Space orientation="vertical" className="full-width">
      <Space wrap>
        {LIST_STATES.map((candidate, candidateIndex) => (
          <Button
            key={candidate.status}
            type={index === candidateIndex ? 'primary' : 'default'}
            onClick={() => setIndex(candidateIndex)}
          >
            {candidate.status}
          </Button>
        ))}
      </Space>
      <div className="sample-preview">{renderListState(state)}</div>
    </Space>
  )
}

interface SampleWord {
  id: number
  name: string
  abbreviation: string
}

const SAMPLE_WORD_ROWS: SampleWord[] = [
  { id: 1, name: '고객', abbreviation: 'CUST' },
  { id: 2, name: '계좌', abbreviation: 'ACCT' },
  { id: 3, name: '거래', abbreviation: 'TRAN' },
]

/** 4번 데모: 제약을 건 제네릭. 안에서 item.id를 쓸 수 있다 */
function indexById<T extends { id: number }>(items: T[]): Map<number, T> {
  return new Map(items.map((item) => [item.id, item]))
}

interface PickListProps<T> {
  items: T[]
  labelOf: (item: T) => string
  onPick: (item: T) => void
}

/** 4번 데모: 제네릭 컴포넌트. 쓰는 쪽의 타입이 labelOf·onPick에 그대로 전달된다 */
function PickList<T>({ items, labelOf, onPick }: PickListProps<T>) {
  return (
    <Space wrap>
      {items.map((item, index) => (
        <Button key={index} size="small" onClick={() => onPick(item)}>
          {labelOf(item)}
        </Button>
      ))}
    </Space>
  )
}

function GenericDemo() {
  const [picked, setPicked] = useState<SampleWord | null>(null)
  const byId = indexById(SAMPLE_WORD_ROWS)

  return (
    <Space orientation="vertical" className="full-width">
      {/* labelOf의 word는 SampleWord로 추론된다 */}
      <PickList items={SAMPLE_WORD_ROWS} labelOf={(word) => word.name} onPick={setPicked} />
      <Typography.Text type="secondary">
        {picked ? `선택: ${picked.name} (${picked.abbreviation}) · id로 찾기: ${byId.get(picked.id)?.name}` : '단어를 골라 보세요.'}
      </Typography.Text>
    </Space>
  )
}

type SampleStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'CANCELED'

/** 5번 데모: Record로 키를 강제하면 상태를 하나 빠뜨렸을 때 컴파일 오류가 난다 */
const SAMPLE_STATUSES: Record<SampleStatus, { label: string; color: string }> = {
  REQUESTED: { label: '대기', color: 'blue' },
  APPROVED: { label: '승인', color: 'green' },
  REJECTED: { label: '반려', color: 'red' },
  CANCELED: { label: '취소', color: 'default' },
}

function UtilityTypeDemo() {
  // Pick으로 뽑은 타입만 쓰는 표
  const rows: Pick<SampleWord, 'id' | 'name'>[] = SAMPLE_WORD_ROWS.map(({ id, name }) => ({ id, name }))

  return (
    <Space orientation="vertical" className="full-width">
      <Space wrap>
        {(Object.keys(SAMPLE_STATUSES) as SampleStatus[]).map((status) => (
          <Tag key={status} color={SAMPLE_STATUSES[status].color}>
            {SAMPLE_STATUSES[status].label}
          </Tag>
        ))}
      </Space>
      <Table
        size="small"
        pagination={false}
        rowKey="id"
        dataSource={rows}
        columns={[
          { title: 'id', dataIndex: 'id', width: 80 },
          { title: '단어명', dataIndex: 'name' },
        ]}
      />
    </Space>
  )
}

const DEMO_FILTER_KEYS = ['name', 'abbreviation'] as const
type DemoFilterKey = (typeof DEMO_FILTER_KEYS)[number]

function AsConstDemo() {
  const [key, setKey] = useState<DemoFilterKey>('name')

  return (
    <Space orientation="vertical" className="full-width">
      <Space wrap>
        {DEMO_FILTER_KEYS.map((candidate) => (
          <Button key={candidate} type={key === candidate ? 'primary' : 'default'} onClick={() => setKey(candidate)}>
            {candidate}
          </Button>
        ))}
      </Space>
      <Typography.Text type="secondary">
        선택한 검색 조건: <code>{key}</code> — 이 값의 타입은 <code>'name' | 'abbreviation'</code>이라 오타를 쓸 수 없다.
      </Typography.Text>
    </Space>
  )
}

/** 8번 데모: errors.ts의 isApiError와 같은 모양 */
interface DemoApiError {
  code: string
  message: string
}

function isDemoApiError(value: unknown): value is DemoApiError {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as DemoApiError).code === 'string' &&
    typeof (value as DemoApiError).message === 'string'
  )
}

function demoErrorMessage(error: unknown): string {
  // 좁혀진 뒤에야 error.message를 쓸 수 있다
  return isDemoApiError(error) ? `[${error.code}] ${error.message}` : '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

function TypeGuardDemo() {
  const [shown, setShown] = useState<string | null>(null)

  return (
    <Space orientation="vertical" className="full-width">
      <Space wrap>
        <Button onClick={() => setShown(demoErrorMessage({ code: 'DUPLICATE_WORD_NAME', message: '이미 있는 단어명입니다.' }))}>
          백엔드 업무 오류
        </Button>
        <Button onClick={() => setShown(demoErrorMessage(new TypeError('Failed to fetch')))}>네트워크 오류</Button>
      </Space>
      {shown && <Alert type="warning" showIcon title={shown} />}
    </Space>
  )
}
