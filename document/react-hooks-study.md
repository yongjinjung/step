# React 훅(Hook) 개념 (학습용)

> 예시는 처방 화면 코드에서 가져왔다.
> 관련 문서: [목록 화면 해설](prescription-list-page-study.md) · [상세 드로어 해설](prescription-detail-drawer-study.md) · [등록 모달 해설](prescription-form-modal-study.md)

---

## 1. 왜 필요한가: 컴포넌트는 렌더링할 때마다 처음부터 다시 실행된다

```tsx
function Counter() {
  let count = 0                                              // 렌더링마다 다시 0
  return <button onClick={() => count++}>{count}</button>   // 눌러도 화면은 0
}
```

함수 컴포넌트는 그냥 함수다. React는 화면을 다시 그릴 때마다 이 함수를 **처음부터 다시 호출**한다. 안에 선언한 지역 변수는 호출이 끝나면 사라지고, 다음 호출에서 새로 만들어진다. 위 코드에는 문제가 두 가지 있다.

1. `count++`를 해도 값이 남지 않는다(다음 호출에서 다시 `0`).
2. 값을 바꿔도 React는 다시 그려야 한다는 것을 모른다.

컴포넌트에는 이런 능력이 필요하다.

| 필요한 것 | 예 | 해결하는 훅 |
|---|---|---|
| 렌더링이 반복돼도 **값 기억** + 바뀌면 **다시 그리기** | 드로어 열림 여부 | `useState` |
| 렌더링 **뒤에** 할 일 | 모달을 열면 폼 초기화 | `useEffect` |
| 위쪽 컴포넌트가 준 **공용 데이터** | 로그인 정보, 메뉴 권한 | `useContext` |
| 비싼 계산·함수를 **재사용** | 주소 → 검색 조건 객체 | `useMemo`, `useCallback` |

**훅은 React가 컴포넌트 바깥에 보관해 주는 기억 칸과 기능을 꺼내 쓰는 함수다.** 이름은 모두 `use`로 시작한다.

```tsx
function Counter() {
  const [count, setCount] = useState(0)                              // React가 보관한 값을 꺼낸다
  return <button onClick={() => setCount(count + 1)}>{count}</button> // 값 저장 + 다시 그리기 요청
}
```

---

## 2. 동작 원리: React는 "몇 번째로 부른 훅인가"로 칸을 찾는다

훅에는 이름표가 없다. React는 컴포넌트마다 칸 배열을 두고 **호출 순서**로 칸을 찾는다. 아래는 개념만 보여주는 단순화한 코드다(실제 React는 연결 리스트로 구현한다).

```ts
// React 내부를 아주 단순하게 흉내 낸 것
let slots = []        // 이 컴포넌트의 기억 칸들
let index = 0         // 이번 렌더링에서 몇 번째 훅인지

function useState(initial) {
  const i = index++                                      // 내 칸 번호
  if (slots[i] === undefined) slots[i] = initial         // 첫 렌더링에만 초기값
  const setState = (next) => { slots[i] = next; rerender() }
  return [slots[i], setState]
}

function render() {
  index = 0           // 렌더링 시작마다 0번 칸부터 다시 센다
  Component()
}
```

`PrescriptionDetailDrawer`는 렌더링마다 이 순서로 칸을 꺼낸다.

| 순서 | 호출 | 칸에 든 것 |
|---|---|---|
| 0 | `$api.useQuery(...)` | 상세 조회 상태 |
| 1 | `App.useApp()` | `message` 등 |
| 2 | `useMenus()` | 메뉴·권한 |
| 3 | `useInvalidateApi()` | 무효화 함수 |
| 4 | `useState(false)` | `cancelOpen` |
| 5 | `$api.useMutation(...)` | 취소 요청 상태 |

`useState(false)`의 `false`는 **첫 렌더링에만** 쓰인다. 두 번째 렌더링부터는 4번 칸에 저장된 값이 나온다.

---

## 3. 훅의 두 가지 규칙과 그 이유

### 규칙 1. 최상단에서만 부른다

컴포넌트(또는 커스텀 훅)의 최상단에서만 부른다. `if`, 반복문, 콜백, 조기 `return` 뒤에서는 부르지 않는다.

```tsx
// ❌ 열렸을 때만 조회하고 싶어서
if (open) {
  const detailQuery = $api.useQuery(...)   // 닫히면 0번 칸을 건너뛰어 뒤의 칸이 모두 한 칸씩 어긋난다
}

// ✅ 항상 부르고, 실행 여부는 옵션으로
const detailQuery = $api.useQuery(
  'get', '/api/prescriptions/{id}',
  { params: { path: { id: prescriptionId ?? 0 } } },
  { enabled: open },
)
```

상세 드로어의 `?? 0` + `enabled: open`은 이 규칙 때문에 생긴 코드다. 조건에 따라 훅 개수가 달라지면 React가 칸을 잘못 짝짓는다. 개발 모드에서는 "Rendered fewer hooks than expected" 같은 오류가 난다.

```tsx
// ❌ 조기 return 뒤의 훅도 같은 문제
if (!open) return null
const [x, setX] = useState(0)     // open일 때만 불린다

// ✅ 훅을 모두 부른 뒤에 return
const [x, setX] = useState(0)
if (!open) return null
```

### 규칙 2. React 컴포넌트나 다른 훅 안에서만 부른다

일반 함수나 이벤트 핸들러에서 부르면 어느 컴포넌트의 칸인지 React가 알 수 없다.

```tsx
// ❌ 클릭할 때 훅 호출
<Button onClick={() => { const q = $api.useQuery(...) }} />

// ✅ 훅은 위에서 만들고, 클릭에서는 그 결과(함수)를 부른다
const cancelPrescription = $api.useMutation('post', '/api/prescriptions/{id}/cancel')
<Button onClick={() => cancelPrescription.mutate(...)} />
```

두 규칙은 eslint의 `react-hooks/rules-of-hooks`가 검사한다.

---

## 4. 기본 훅 (React가 주는 것)

### 4.1 `useState`: 값 기억 + 다시 그리기

```tsx
const [detailId, setDetailId] = useState<number | null>(null)
```
- `[현재 값, 바꾸는 함수]`를 돌려준다.
- 초기값만으로 타입을 정할 수 없으면(`null`) 제네릭으로 알려 준다.

**값은 즉시 바뀌지 않는다.**
```tsx
setDetailId(5)
console.log(detailId)    // 아직 이전 값. 5는 "다음 렌더링"에서 보인다
```
`setXxx`는 "이 값으로 다시 그려 달라"는 **요청**이다. 다음에 컴포넌트 함수가 다시 실행될 때 `useState`가 새 값을 돌려준다.

**이전 값을 바탕으로 바꿀 때는 함수로 넘긴다.**
```tsx
setCount(count + 1); setCount(count + 1)          // 둘 다 같은 count를 봐서 +1만 된다
setCount((c) => c + 1); setCount((c) => c + 1)     // +2
```
`useListSearchParams`의 `setSearchParams((previous) => ...)`도 같은 이유로 함수 형태를 쓴다.

**state에 두지 않는 것:** props나 다른 state에서 **계산할 수 있는 값**은 state로 두지 않는다. 두 값이 어긋날 수 있기 때문이다.
```tsx
const open = prescriptionId !== null    // 상세 드로어: useState로 따로 두지 않는다
```

### 4.2 `useEffect`: 렌더링 뒤에 할 일

```tsx
// 등록 모달: 열 때마다 폼 초기화
useEffect(() => {
  if (!open) return
  form.resetFields()
  form.setFieldsValue({ prescriptionType: 'OUTPATIENT', prescribedAt: nowForDateTimeInput(), ... })
}, [open, form])
```

| 의존 배열 | 실행 시점 |
|---|---|
| 없음 | 매 렌더링 뒤 |
| `[]` | 처음 한 번(마운트) |
| `[open, form]` | 둘 중 하나라도 바뀐 렌더링 뒤 |

- 렌더링 **중에** 부수 효과(폼 값 바꾸기, 요청, 구독)를 일으키면 안 된다. 렌더링은 "화면이 어떻게 생겼나"를 계산하는 일만 해야 한다. 부수 효과는 `useEffect`나 이벤트 핸들러에서 한다.
- **정리(cleanup) 함수**: 효과가 반환한 함수는 다음 효과 직전이나 컴포넌트가 사라질 때 실행된다.
  ```tsx
  useEffect(() => {
    const timer = setInterval(tick, 1000)
    return () => clearInterval(timer)     // 정리
  }, [])
  ```
- 효과 안에서 쓰는 값은 의존 배열에 모두 적는다(eslint `react-hooks/exhaustive-deps`). 빠뜨리면 **옛 값**을 계속 보게 된다.
- 개발 모드(`StrictMode`)에서는 마운트 직후 효과를 일부러 한 번 더 실행해 정리 누락을 찾아낸다. 효과가 두 번 실행돼도 문제가 없게 작성한다.
- 서버 조회는 `useEffect` + `fetch`로 직접 하지 않고 `useQuery`를 쓴다. 캐시, 중복 요청, 경쟁 상태(늦게 온 옛 응답)를 대신 처리해 준다.

### 4.3 `useMemo`·`useCallback`: 같은 값을 다시 만들지 않기

```tsx
// useListSearchParams
const filters = useMemo(
  () => Object.fromEntries(filterKeys.map((key) => [key, searchParams.get(key) ?? undefined])),
  [filterKeys, searchParams],
)
const setFilters = useCallback((next) => { ... }, [filterKeys, setSearchParams])
```

| 훅 | 기억하는 것 | 쓰는 곳 |
|---|---|---|
| `useMemo(() => 값, deps)` | 계산 **결과** | 무거운 계산, 다른 훅의 의존값으로 쓰는 객체 |
| `useCallback(fn, deps)` | **함수** 자체 | 다른 훅의 의존값이나 자식 props로 넘기는 함수 |

`useCallback(fn, deps)`는 `useMemo(() => fn, deps)`와 같다.

**의존값은 `===`(참조)로 비교한다.**
```ts
['a'] === ['a']   // false: 내용이 같아도 다른 배열
```
렌더링마다 새로 만든 배열·객체·함수를 의존값으로 넣으면 매번 "바뀜"으로 판정되어 기억이 소용없어진다. `FILTER_KEYS`를 컴포넌트 **밖** 상수로 둔 이유다.

모든 값을 감쌀 필요는 없다. 가벼운 계산(`pickCode`)이나 의존값으로 쓰이지 않는 함수(`updateFilter`, `handleFinish`)는 그냥 둔다. 감싸는 비용(의존값 비교)이 더 들 수 있다.

### 4.4 `useContext`: 위에서 내려준 공용 값

```tsx
// layouts/MenuContext.tsx (개념)
const MenuContext = createContext<MenuContextValue | null>(null)

export function MenuProvider({ children }) {
  const menus = /* /api/menus 조회 */
  const can = (path, action) => /* 권한 계산 */
  return <MenuContext.Provider value={{ menus, can }}>{children}</MenuContext.Provider>
}

export function useMenus() {
  const value = useContext(MenuContext)
  if (!value) throw new Error('MenuProvider 안에서만 쓸 수 있습니다.')
  return value
}
```
- `Provider` 아래 어느 깊이의 컴포넌트든 `useContext`로 값을 꺼낸다. props로 층층이 넘기지 않아도 된다(prop drilling 방지).
- `value`가 바뀌면 그 컨텍스트를 쓰는 컴포넌트가 다시 렌더링된다.
- 이 프로젝트는 `useContext`를 직접 쓰지 않고 `useMenus()`, `useAuth()`처럼 감싼 커스텀 훅으로 꺼낸다.

### 4.5 `useRef`: 기억하지만 다시 그리지 않는 값

```tsx
const inputRef = useRef<HTMLInputElement>(null)
<input ref={inputRef} />
inputRef.current?.focus()
```
- `ref.current`를 바꿔도 다시 렌더링되지 않는다. DOM 요소, 타이머 ID, 이전 값 보관에 쓴다.
- 화면에 보여야 하는 값은 `useState`, 보이지 않아도 되는 값은 `useRef`.

---

## 5. 라이브러리 훅

라이브러리도 같은 원리(내부에서 기본 훅을 써서)로 자기 기능을 훅으로 내준다.

| 훅 | 출처 | 하는 일 | 처방 화면의 예 |
|---|---|---|---|
| `$api.useQuery` | TanStack Query (openapi-react-query) | 서버 조회. 캐시·로딩·오류 상태를 관리하고 결과가 오면 다시 렌더링 | 목록, 상세 조회 |
| `$api.useMutation` | 같음 | 서버 변경. `mutate()`를 불러야 실행 | 등록, 취소 |
| `useQueryClient` | TanStack Query | 캐시 전체에 접근 | `useInvalidateApi` 내부 |
| `useSearchParams` | React Router | 주소 `?a=1` 읽기·쓰기. 주소가 바뀌면 다시 렌더링 | `useListSearchParams` 내부 |
| `Form.useForm` | Ant Design | 폼 인스턴스(렌더링이 반복돼도 같은 객체) | 등록 모달, `ReasonModal` |
| `App.useApp` | Ant Design | 테마가 적용된 `message`, `modal` | 성공·오류 알림 |

**`useQuery`에 `await`가 없는 이유**
```tsx
const listQuery = $api.useQuery('get', '/api/prescriptions', ...)
// 첫 렌더링: listQuery.data === undefined, isPending === true  → 요청 시작
// 응답 도착: TanStack Query가 다시 렌더링시킴
// 두 번째 렌더링: listQuery.data === { content: [...], totalElements: 123 }
```
훅은 지금 상태를 **바로** 돌려준다. 비동기 결과는 "다시 렌더링"으로 전달된다. 컴포넌트 함수는 `async`가 될 수 없다.

---

## 6. 커스텀 훅: 훅을 묶어 만든 내 훅

안에서 훅을 쓰는 함수를 만들고 이름을 `use`로 시작하면 커스텀 훅이다. 여러 화면에서 반복되는 "훅 + 로직" 묶음을 한 곳에 모은다.

```ts
// hooks/useListSearchParams.ts (요약)
export function useListSearchParams<K extends string>(filterKeys: readonly K[], defaultSize = 20) {
  const [searchParams, setSearchParams] = useSearchParams()                 // 라이브러리 훅
  const filters = useMemo(() => /* 주소 → 조건 객체 */, [filterKeys, searchParams])
  const page = Math.max(1, Number(searchParams.get('page')) || 1)          // 일반 계산
  const setFilters = useCallback((next) => /* 조건 → 주소 */, [filterKeys, setSearchParams])
  const setPaging = useCallback((p, s) => /* 페이지 → 주소 */, [...])
  return { filters, page, size, setFilters, setPaging }
}
```

| 커스텀 훅 | 안에서 쓰는 훅 | 감춘 로직 | 쓰는 곳 |
|---|---|---|---|
| `useListSearchParams` | `useSearchParams`, `useMemo`, `useCallback` | 주소 ↔ 검색 조건·페이지 변환 | 모든 목록 화면 |
| `useInvalidateApi` | `useQueryClient`, `useCallback` | 경로로 캐시 무효화 | 등록·수정·삭제 뒤 |
| `useMenus` | `useContext` | 메뉴·권한 `can()` | 버튼 표시 |
| `useAuth` | `useContext` | 로그인 사용자·로그아웃 | 헤더, 내 정보 |
| `useDebouncedValue` | `useState`, `useEffect` | 입력이 멈춘 뒤에 값 반영 | 서버 검색 선택 박스 |

**커스텀 훅은 상태를 공유하지 않는다.** 두 화면이 `useListSearchParams`를 쓰면 화면마다 각자의 칸이 생긴다. 공유되는 것은 **로직**뿐이다. 데이터를 공유하려면 바깥 저장소를 쓴다.
- 주소(`useSearchParams`)
- Context(`useMenus`)
- TanStack Query 캐시(같은 쿼리 키를 쓰면 같은 데이터)

커스텀 훅에도 훅 규칙이 그대로 적용된다(최상단에서, 컴포넌트나 훅 안에서).

---

## 7. 훅으로 만들까, 일반 함수로 만들까

기준은 하나다. **안에서 훅을 부르는가.**

| 함수 | 안에서 훅을 부르나 | 종류 |
|---|---|---|
| `pickCode`, `pickDate`, `formatQuantity`, `formatDateTime`, `nowForDateTimeInput` | 아니요 | 일반 함수. 어디서든, 조건문 안에서도 부를 수 있다 |
| `useListSearchParams`, `useInvalidateApi`, `useMenus` | 예 | 훅. `use` 접두어, 훅 규칙 적용 |

- 훅을 부르지 않는데 `use`를 붙이면 쓸데없이 훅 규칙에 묶인다.
- 훅을 부르면서 `use`를 빼면 린트가 규칙 위반을 잡지 못한다.

---

## 8. 처방 화면에서 쓰는 훅 지도

| 파일 | 훅 | 용도 |
|---|---|---|
| `PrescriptionListPage` | `useMenus` | 등록 버튼 권한 |
| | `useListSearchParams` | 검색 조건·페이지 ↔ 주소 |
| | `$api.useQuery` + `keepPreviousData` | 목록 조회, 페이지 이동 시 깜빡임 방지 |
| | `useState` ×2 | 상세 드로어 ID, 등록 모달 열림 |
| `PrescriptionDetailDrawer` | `$api.useQuery` + `enabled` | 열렸을 때만 상세 조회 |
| | `App.useApp` | 알림 |
| | `useMenus` | 취소 버튼 권한 |
| | `useInvalidateApi` | 취소 후 목록·상세 다시 조회 |
| | `useState` | 취소 사유 모달 열림 |
| | `$api.useMutation` | 취소 요청 |
| `PrescriptionFormModal` | `App.useApp` | 알림 |
| | `useInvalidateApi` | 등록 후 목록 다시 조회 |
| | `Form.useForm` | 폼 인스턴스 |
| | `$api.useMutation` | 등록 요청 |
| | `useEffect` | 열 때마다 폼 초기화 |

---

## 9. 자주 하는 실수

| 실수 | 증상 | 고치는 법 |
|---|---|---|
| 조건문 안에서 훅 호출 | "Rendered fewer hooks than expected" | 항상 부르고 옵션(`enabled`)이나 결과로 분기 |
| `setX` 직후 `x`를 읽음 | 이전 값이 나옴 | 새 값을 변수에 두고 쓰거나, 다음 렌더링에서 읽기 |
| 렌더링 중 `setX` 호출 (`onClick={setX(1)}`) | 무한 렌더링 | `onClick={() => setX(1)}`처럼 함수를 넘긴다 |
| 의존 배열 누락 | 효과·콜백이 옛 값을 봄 | 쓰는 값을 모두 적는다(eslint 경고를 따른다) |
| 렌더링마다 새 배열·객체를 의존값으로 | `useMemo`·`useEffect`가 매번 실행 | 컴포넌트 밖 상수로 두거나 `useMemo`로 감싼다 |
| 계산 가능한 값을 state로 복사 | props와 state가 어긋남 | 렌더링 중에 계산한다(`const open = id !== null`) |
| `useEffect` + `fetch`로 직접 조회 | 중복 요청, 늦은 응답이 덮어씀 | `useQuery`를 쓴다 |

---

## 한 줄 요약

**훅은 매번 다시 실행되는 컴포넌트 함수가 React에 맡겨 둔 기억 칸과 기능을 꺼내는 함수다. React는 호출 순서로 칸을 찾으므로 항상 같은 순서로, 최상단에서 불러야 한다.**

---

## 스스로 확인해 볼 것

1. `PrescriptionListPage`에서 `useState` 두 줄의 순서를 바꿔도 동작은 같다. 그런데 `if (canCreate)` 안으로 `useState(false)`를 옮기면 왜 안 되는가?
2. 등록 모달의 `useEffect` 의존 배열에서 `open`을 빼면 어떻게 되는가?
3. `useListSearchParams`의 `useMemo`를 지우고 `filters`를 그냥 계산하면 동작이 달라지는가? 무엇이 달라지는가?
4. 목록 화면 두 탭(같은 `/prescriptions`, 다른 검색 조건)을 열면 `useListSearchParams` 상태는 공유되는가? 같은 조건이면 `useQuery` 데이터는 공유되는가?
5. `formatQuantity`에 `use`를 붙여 `useFormatQuantity`로 바꾸면 어떤 불편이 생기는가?
