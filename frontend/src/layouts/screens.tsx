import type { ReactElement } from 'react'
import { Route, Routes } from 'react-router'
import { RequireAuth } from '@/auth/RequireAuth'
import { CodeGroupDetailPage } from '@/features/codes/CodeGroupDetailPage'
import { CodeGroupListPage } from '@/features/codes/CodeGroupListPage'
import { DomainListPage } from '@/features/domains/DomainListPage'
import { HistoryListPage } from '@/features/histories/HistoryListPage'
import { RequestListPage } from '@/features/requests/RequestListPage'
import { NonStandardColumnListPage } from '@/features/tables/NonStandardColumnListPage'
import { TableDetailPage } from '@/features/tables/TableDetailPage'
import { TableListPage } from '@/features/tables/TableListPage'
import { TermListPage } from '@/features/terms/TermListPage'
import { WordListPage } from '@/features/words/WordListPage'
import { MyPage } from '@/pages/MyPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'
import { NAV_ITEMS, type NavItem } from './navigation'

/**
 * 메뉴 경로 → 화면. 새 화면을 만들면 여기에 등록한다 (없으면 준비 중 화면이 나온다).
 */
const PAGES: Partial<Record<string, ReactElement>> = {
  '/words': <WordListPage />,
  '/domains': <DomainListPage />,
  '/terms': <TermListPage />,
  '/codes': <CodeGroupListPage />,
  '/tables': <TableListPage />,
  '/non-standard-columns': <NonStandardColumnListPage />,
  '/requests': <RequestListPage />,
  '/histories': <HistoryListPage />,
}

/** 관리자 전용 메뉴는 화면을 권한 검사로 감싼다 (서버도 같은 규칙으로 막는다). */
function screenOf(item: NavItem): ReactElement {
  const page = PAGES[item.path] ?? <PlaceholderPage item={item} />
  return item.adminOnly ? <RequireAuth adminOnly>{page}</RequireAuth> : page
}

/**
 * 탭 하나가 보여줄 화면을 주소로 고른다.
 *
 * `<Routes location=...>`에 탭의 주소를 넘기므로, 보고 있지 않은 탭도 자기 주소 기준으로 그려진다.
 * 화면 안의 `useParams`(`/tables/:id`)와 `useSearchParams`(검색 조건)도 그 주소를 읽는다.
 * 즉 탭마다 검색 조건과 페이지를 따로 기억한다.
 *
 * @param location 탭의 주소 (`pathname + search`)
 */
export function ScreenRoutes({ location }: { location: string }) {
  return (
    <Routes location={location}>
      {NAV_ITEMS.map((item) => (
        <Route key={item.path} path={item.path.slice(1)} element={screenOf(item)} />
      ))}
      {/* 메뉴에 없는 상세 화면. 목록에서 링크로 열리며 탭 이름은 화면이 useTabTitle로 채운다. */}
      <Route path="tables/:id" element={<TableDetailPage />} />
      <Route path="codes/:id" element={<CodeGroupDetailPage />} />
      <Route path="me" element={<MyPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
