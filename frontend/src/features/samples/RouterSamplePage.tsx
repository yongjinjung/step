import { Alert, Button, Input, Space, Tag, Typography } from 'antd'
import { useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { PageHeader } from '@/components/PageHeader'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { SampleSection } from './SampleSection'

/**
 * React Router 7 샘플 화면.
 *
 * 이 프로젝트는 화면 탭(포틀릿) 때문에 라우팅이 일반적인 SPA와 다르게 동작한다.
 * "주소 하나 = 화면 하나"가 아니라 "탭마다 자기 주소를 기억"하는 구조라, 그 이유와 코드를 함께 본다.
 */
export function RouterSamplePage() {
  return (
    <>
      <PageHeader title="라우팅과 탭 샘플" subTitle="React Router 7 · 이 프로젝트의 화면 탭(포틀릿) 구조" />

      <Space orientation="vertical" size="large" className="full-width">
        <Alert
          type="info"
          showIcon
          title="이 프로젝트의 라우팅은 두 단계다"
          description={
            <>
              <b>React Router</b>가 "이 주소에 어떤 화면을 그릴지"를 정하고, 그 위에 이 프로젝트가 만든{' '}
              <b>화면 탭 시스템</b>이 "열어 둔 화면들을 탭으로 유지"한다. 라우터만 알아서는 왜 탭을 옮겨도 검색 조건이
              그대로인지 설명이 안 되므로 6번을 꼭 읽는다.
            </>
          }
        />

        <SampleSection
          title="1. 이 프로젝트의 라우팅 구조"
          summary={
            <>
              로그인 여부만 <code>router.tsx</code>가 가르고, 로그인 뒤 모든 주소는 <code>AppLayout</code> 하나가 받는다.
              실제로 어떤 화면을 그릴지는 <code>layouts/screens.tsx</code>의 <code>ScreenRoutes</code>가 정한다.
            </>
          }
          points={[
            <>
              <code>{'{ path: \'*\', element: <AppLayout /> }'}</code> 하나로 모든 주소를 받는다. 일반적인{' '}
              <code>{'<Route path="/words" element={...}>'}</code> 나열이 아니다.
            </>,
            <>
              <code>AppLayout</code> 안에서 <code>WorkspaceTabs</code>가 열린 탭 줄을, <code>ScreenRoutes</code>가 각 탭의
              내용을 그린다(6번에서 자세히).
            </>,
            <>
              <code>MenuProvider</code>가 <code>/api/menus</code>를 한 번 불러와 <code>useMenus()</code>로 어디서든 쓸 수
              있게 한다(메뉴 이름, 권한, 첫 화면 주소).
            </>,
          ]}
          code={`// router.tsx
export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,          // 로그인 안 했으면 /login으로
    children: [
      {
        element: <MenuLayout />,        // <MenuProvider><Outlet /></MenuProvider>
        children: [
          { index: true, element: <HomeRedirect /> },   // "/" → 메뉴의 첫 화면으로
          { path: '*', element: <AppLayout /> },         // 나머지 전부 AppLayout 하나가 받는다
        ],
      },
    ],
  },
])`}
          codeTitle="src/router.tsx"
        />

        <SampleSection
          title="2. 화면 이동 — Link와 useNavigate"
          summary={
            <>
              사용자가 누르는 이동은 <code>Link</code>(또는 <code>NavLink</code>), 코드에서 결정하는 이동(저장 후 목록으로,
              뒤로 가기)은 <code>useNavigate()</code>를 쓴다.
            </>
          }
          points={[
            <>
              <code>{'<a href>'}</code>를 쓰면 페이지 전체가 새로고침된다. 앱 안의 이동은 항상 <code>Link</code>나{' '}
              <code>navigate</code>로 한다.
            </>,
            <>
              <code>navigate(-1)</code>은 브라우저 뒤로 가기와 같다. 탭 시스템에서는 "이전 화면"이 아니라 "이 탭이 전에
              있던 주소"로 돌아간다.
            </>,
            <>
              목록의 행을 눌러 상세로 이동할 때도 같은 방식이다(<code>Link to={'`/tables/${id}`'}</code>).
            </>,
          ]}
          demo={<NavigationDemo />}
          code={`import { Link, useNavigate } from 'react-router'

// 사용자가 누르는 이동
<Link to="/typescript">TypeScript 샘플로</Link>

// 코드로 결정하는 이동
const navigate = useNavigate()
const afterSave = () => navigate('/words')   // 저장 후 목록으로
const goBack = () => navigate(-1)            // 이 탭이 전에 있던 주소로`}
        />

        <SampleSection
          title="3. URL에 상태 담기 — useSearchParams"
          summary={
            <>
              검색어·필터·페이지를 <code>useState</code>가 아니라 <b>주소</b>에 두면 새로고침해도, 뒤로 가기를 해도,
              주소를 복사해 공유해도 같은 조건이 유지된다.
            </>
          }
          points={[
            <>
              아래 입력에 글자를 쳐 보면 브라우저 주소창의 <code>?demoQ=</code>가 바로 바뀐다(이 탭의 진짜 주소다).
            </>,
            <>
              값이 비었을 때는 파라미터 자체를 지운다. 그래야 기본 상태의 주소가 지저분해지지 않는다.
            </>,
            <>
              화면마다 이 패턴을 직접 짜지 않고, 다음 섹션의 <code>useListSearchParams</code>로 통일한다.
            </>,
          ]}
          demo={<SearchParamsDemo />}
          code={`const [searchParams, setSearchParams] = useSearchParams()
const keyword = searchParams.get('demoQ') ?? ''

const onChange = (value: string) => {
  setSearchParams(previous => {
    const params = new URLSearchParams(previous)
    if (value.trim()) params.set('demoQ', value.trim())
    else params.delete('demoQ')   // 비어 있으면 주소에서 뺀다
    return params
  })
}`}
        />

        <SampleSection
          title="4. useListSearchParams — 이 프로젝트의 목록 상태 훅"
          summary={
            <>
              모든 목록 화면(<code>WordListPage</code> 등)이 검색 조건·페이지를 3번의 방식으로 직접 짜지 않고 이 훅
              하나로 통일한다. <code>page</code>는 화면 기준 <b>1부터</b>이고 API에는 <code>page - 1</code>을 넘긴다.
            </>
          }
          points={[
            <>
              <code>filterKeys</code>는 반드시 <b>컴포넌트 밖 상수</b>로 선언한다(<code>const FILTER_KEYS = [...] as
              const</code>). 렌더마다 새 배열을 넘기면 참조가 매번 달라져 의존성 배열이 매 렌더 바뀐다.
            </>,
            <>
              <code>setFilters</code>를 부르면 자동으로 1페이지로 돌아간다. 검색 조건이 바뀌었는데 3페이지에 머물러
              있으면 안 되기 때문이다.
            </>,
            <>
              <code>setPaging</code>은 표의 크기(<code>pageSize</code>)가 바뀌면 1페이지로, 페이지만 바뀌면 그 페이지로
              간다(<code>WordListPage</code>의 <code>Table pagination.onChange</code>에 그대로 연결한다).
            </>,
          ]}
          demo={<ListSearchParamsDemo />}
          code={`const FILTER_KEYS = ['keyword', 'classifier'] as const  // 컴포넌트 밖 상수

function WordListPage() {
  const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)

  const listQuery = $api.useQuery('get', '/api/words', {
    params: { query: { keyword: filters.keyword, page: page - 1, size } },  // 화면 1부터 → API 0부터
  })

  return (
    <>
      <Input.Search defaultValue={filters.keyword} onSearch={keyword => setFilters({ keyword })} />
      <Table pagination={{ current: page, pageSize: size, total: ..., onChange: setPaging }} />
    </>
  )
}`}
        />

        <SampleSection
          title="5. useParams — 상세 화면 주소"
          summary={
            <>
              하위 목록이 주 내용인 상세(테이블의 컬럼 등)는 드로어 대신 <b>별도 주소</b>(<code>/tables/:id</code>)로 만든다.
              그래야 그 상세만 새로고침·공유·탭 두 개로 열기가 된다. 주소의 <code>:id</code> 값은{' '}
              <code>useParams()</code>로 읽는다.
            </>
          }
          points={[
            <>
              이런 상세 라우트는 <code>screens.tsx</code>의 <code>PAGES</code>(메뉴 목록)가 아니라{' '}
              <code>ScreenRoutes</code>에 직접 <code>{'<Route path="tables/:id">'}</code>로 추가한다. 메뉴에는 없지만
              목록의 링크로만 열린다.
            </>,
            <>
              권한은 상세 화면 자체가 아니라 <b>상위 메뉴 기준</b>으로 검사한다(<code>{'<RequireMenu path="/tables">'}</code>).
            </>,
            <>
              아래 버튼을 누르면 이 화면 바로 아래 <code>/router-demo/:id</code> 라우트로 이동해 실제로{' '}
              <code>useParams()</code>가 그 값을 읽는 것을 볼 수 있다(이 프로젝트의 상세 라우트와 같은 방식).
            </>,
          ]}
          demo={<ParamsDemo />}
          code={`// screens.tsx — 메뉴 목록(PAGES)이 아니라 ScreenRoutes에 직접 추가한다
<Route
  path="tables/:id"
  element={
    <RequireMenu path="/tables">   {/* 권한은 상위 메뉴 기준 */}
      <TableDetailPage />
    </RequireMenu>
  }
/>

// TableDetailPage.tsx
function TableDetailPage() {
  const { id } = useParams()   // 주소 세그먼트를 문자열로 받는다 (숫자면 Number(id)로 바꾼다)
  const tableQuery = $api.useQuery('get', '/api/tables/{id}', { params: { path: { id: Number(id) } } })
  ...
}`}
        />

        <SampleSection
          title="6. 화면 탭(포틀릿) — 왜, 어떻게"
          summary={
            <>
              메뉴나 링크로 연 화면은 <b>탭으로 쌓인다</b>. 탭을 옮겨도 검색 조건·입력값·조회 결과가 그대로인 이유는{' '}
              <b>열어 둔 탭을 전부 그려 두고, 보고 있는 탭만 보여주기 때문</b>이다(언마운트하지 않는다).
            </>
          }
          points={[
            <>
              탭 키는 주소의 <code>pathname</code>이다. 그래서 같은 화면(<code>/words</code>)은 탭 하나, 상세(
              <code>/tables/3</code>, <code>/tables/7</code>)는 대상마다 다른 탭이 열린다.
            </>,
            <>
              탭마다 <code>{'<Routes location={탭의 주소}>'}</code>로 따로 그린다. 그래서 그 탭 안의{' '}
              <code>useParams</code>·<code>useSearchParams</code>는 <b>그 탭의 주소</b>를 읽는다 — 다른 탭이 주소를
              바꿔도 영향받지 않는다.
            </>,
            <>
              한 번도 열지 않은(새로고침으로 복원된) 탭은 그리지 않는다. 실제로 클릭해야 그때부터 계속 살아 있다. 최대
              12개까지 열리고, 넘으면 가장 오래된 탭부터 닫힌다.
            </>,
            <>
              탭 이름은 저장하지 않고 그릴 때 메뉴에서 찾는다. 상세 화면은 <code>useTabTitle(대상 이름)</code>으로
              "메뉴명 · 대상 이름"이 되게 알려준다.
            </>,
            <>
              탭은 사용자별로 <code>sessionStorage</code>에 저장되고, 로그아웃하거나 다른 사용자로 로그인하면 지워진다.
            </>,
          ]}
          demo={<TabConceptDemo />}
          code={`// layouts/screens.tsx — 탭 하나가 보여줄 화면을 그 탭의 주소로 고른다
export function ScreenRoutes({ location }: { location: string }) {
  return (
    <Routes location={location}>   {/* 이 탭의 주소만 본다 */}
      {SCREEN_PATHS.map(path => <Route key={path} path={path.slice(1)} element={...} />)}
    </Routes>
  )
}

// layouts/WorkspaceTabs.tsx (개념 요약) — 열린 탭을 모두 그리고, 보는 탭만 hidden을 뗀다
{tabs.map(tab => (
  <div key={tab.key} hidden={tab.key !== activeKey} className="workspace-pane">
    <ScreenRoutes location={tab.path} />   {/* path = pathname + search, 탭마다 다르다 */}
  </div>
))}

// layouts/WorkspaceTabsContext.tsx — 탭에 대상 이름 알려주기
export function useTabTitle(title: string | undefined) {
  const { pathname } = useLocation()
  const { setTabTitle } = useWorkspaceTabs()
  useEffect(() => { if (title) setTabTitle(pathname, title) }, [title, pathname, setTabTitle])
}`}
          codeTitle="layouts/screens.tsx · WorkspaceTabs.tsx · WorkspaceTabsContext.tsx (요약)"
        />

        <SampleSection
          title="7. 새 화면을 라우터에 연결하기"
          summary={<>화면 코드를 다 만들었다면 아래 두 가지를 해야 실제로 열린다.</>}
          points={[
            <>
              ① <code>layouts/screens.tsx</code>의 <code>PAGES</code>에 <code>{"'/새경로': <새화면 />"}</code>를
              추가한다. 이게 없으면 메뉴를 눌러도 "준비 중" 화면만 보인다.
            </>,
            <>
              ② 메뉴 관리 화면(<code>/menus</code>)에서 그 경로로 메뉴를 등록한다. 메뉴가 없으면 주소를 직접 쳐야만
              들어갈 수 있고 메뉴 막대에는 나오지 않는다.
            </>,
            <>
              하위 목록이 주 내용인 상세(5번)는 메뉴에 등록하지 않고 <code>ScreenRoutes</code>에 라우트만 추가한다.
            </>,
            <>
              메뉴 경로는 <b>권한의 이름이기도 하다</b>(백엔드 <code>MenuPaths</code>). 화면을 옮기며 경로를 바꾸면
              그 메뉴에 걸린 권한이 API 규칙과 이어지지 않게 되니 주의한다.
            </>,
          ]}
        />
      </Space>
    </>
  )
}

/* ------------------------------------------------------------------ */
/* 데모 컴포넌트                                                        */
/* ------------------------------------------------------------------ */

/** 2번 데모: Link와 useNavigate. 실제로 다른 샘플 화면으로 이동한다(탭이 새로 열린다). */
function NavigationDemo() {
  const navigate = useNavigate()

  return (
    <Space wrap align="center">
      <Link to="/typescript">
        <Button>Link로 TypeScript 샘플 열기</Button>
      </Link>
      <Button onClick={() => navigate('/vite')}>navigate()로 Vite 샘플 열기</Button>
      <Button onClick={() => navigate(-1)}>navigate(-1) 뒤로</Button>
      <Typography.Text type="secondary">누르면 이 화면(라우팅 샘플) 탭은 그대로 두고 새 탭이 열린다.</Typography.Text>
    </Space>
  )
}

/** 3번 데모: 진짜 useSearchParams. 입력하면 이 탭의 실제 주소가 바뀐다. */
function SearchParamsDemo() {
  const [searchParams, setSearchParams] = useSearchParams()
  const keyword = searchParams.get('demoQ') ?? ''

  const onChange = (value: string) => {
    setSearchParams((previous) => {
      const params = new URLSearchParams(previous)
      if (value.trim()) {
        params.set('demoQ', value.trim())
      } else {
        params.delete('demoQ')
      }
      return params
    })
  }

  return (
    <Space orientation="vertical" className="full-width">
      <Input value={keyword} onChange={(e) => onChange(e.target.value)} placeholder="검색어를 입력해 보세요" className="sample-input" />
      <Typography.Text type="secondary">
        지금 이 탭의 주소: <code>?demoQ={keyword || '(없음)'}</code> — 주소창에서도 확인할 수 있다. 새로고침해도 값이
        남는다.
      </Typography.Text>
    </Space>
  )
}

const DEMO_FILTER_KEYS = ['demoKeyword'] as const

/** 4번 데모: 실제 useListSearchParams 훅을 그대로 쓴 예 */
function ListSearchParamsDemo() {
  const { filters, page, size, setFilters, setPaging } = useListSearchParams(DEMO_FILTER_KEYS, 5)
  const total = 23 // 데모용 가짜 전체 건수

  return (
    <Space orientation="vertical" className="full-width">
      <Input.Search
        key={filters.demoKeyword ?? ''}
        defaultValue={filters.demoKeyword}
        placeholder="검색하면 1페이지로 돌아간다"
        allowClear
        className="sample-input"
        onSearch={(value) => setFilters({ demoKeyword: value })}
      />
      <Space wrap align="center">
        <Tag color="blue">page={page}</Tag>
        <Tag color="blue">size={size}</Tag>
        <Tag>검색어: {filters.demoKeyword || '(없음)'}</Tag>
        <Button size="small" onClick={() => setPaging(page + 1, size)} disabled={page * size >= total}>
          다음 페이지
        </Button>
        <Button size="small" onClick={() => setPaging(1, size)} disabled={page === 1}>
          첫 페이지
        </Button>
      </Space>
    </Space>
  )
}

/** 5번 데모: 실제로 /router-demo/:id 라우트로 이동해 useParams를 보여준다 */
function ParamsDemo() {
  const [id, setId] = useState(7)

  return (
    <Space wrap align="center">
      <Typography.Text>예시 id:</Typography.Text>
      <Input
        type="number"
        value={id}
        onChange={(e) => setId(Number(e.target.value) || 0)}
        className="sample-number-input"
      />
      <Link to={`/router-demo/${id}`}>
        <Button type="primary">/router-demo/{id} 열기</Button>
      </Link>
    </Space>
  )
}

/** 5번 데모가 실제로 여는 상세 라우트. 진짜 useParams()로 주소의 값을 읽는다 (screens.tsx에 등록됨). */
export function RouterParamDemoPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  return (
    <>
      <PageHeader title="라우팅 샘플 — 상세" onBack={() => navigate('/router')} />
      <Alert
        type="success"
        showIcon
        title="useParams()로 읽은 값"
        description={
          <>
            주소는 <code>/router-demo/{id}</code>이고, <code>{"useParams().id"}</code>는{' '}
            <Tag color="blue">{id}</Tag>입니다. 실제 화면(<code>/tables/:id</code>)에서는 이 값으로 단건 조회를
            합니다.
          </>
        }
      />
    </>
  )
}

/** 6번 데모: 탭이 왜 상태를 유지하는지 흉내. 두 패널을 항상 그려 두고 hidden만 바꾼다(진짜 탭 구현과 같은 원리). */
function TabConceptDemo() {
  const [active, setActive] = useState<'a' | 'b'>('a')
  const [countA, setCountA] = useState(0)
  const [countB, setCountB] = useState(0)

  return (
    <Space orientation="vertical" className="full-width">
      <Space>
        <Button type={active === 'a' ? 'primary' : 'default'} onClick={() => setActive('a')}>
          가짜 탭 A
        </Button>
        <Button type={active === 'b' ? 'primary' : 'default'} onClick={() => setActive('b')}>
          가짜 탭 B
        </Button>
      </Space>

      {/* 둘 다 항상 렌더링된다. display를 hidden 속성으로만 바꾼다 — 언마운트하지 않는다. */}
      <div hidden={active !== 'a'} className="sample-preview">
        <Space align="center">
          <Typography.Text>탭 A의 카운터: {countA}</Typography.Text>
          <Button size="small" onClick={() => setCountA((c) => c + 1)}>
            +1
          </Button>
        </Space>
      </div>
      <div hidden={active !== 'b'} className="sample-preview">
        <Space align="center">
          <Typography.Text>탭 B의 카운터: {countB}</Typography.Text>
          <Button size="small" onClick={() => setCountB((c) => c + 1)}>
            +1
          </Button>
        </Space>
      </div>

      <Typography.Text type="secondary">
        A에서 +1을 누르고 B로 갔다가 다시 A로 돌아와도 값이 남는다 — 화면이 사라진 적이 없기 때문이다. 실제 탭도 같은
        방식으로 검색 조건과 조회 결과를 유지한다.
      </Typography.Text>
    </Space>
  )
}
