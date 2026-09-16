import type { ReactElement } from 'react'
import { Route, Routes, useLocation } from 'react-router'
import { RequireAuth } from '@/auth/RequireAuth'
import { CodeGroupDetailPage } from '@/features/codes/CodeGroupDetailPage'
import { CodeGroupListPage } from '@/features/codes/CodeGroupListPage'
import { DomainListPage } from '@/features/domains/DomainListPage'
import { HistoryListPage } from '@/features/histories/HistoryListPage'
import { MenuManagePage } from '@/features/menus/MenuManagePage'
import { MenuPermissionPage } from '@/features/menus/MenuPermissionPage'
import { RequestListPage } from '@/features/requests/RequestListPage'
import { NonStandardColumnListPage } from '@/features/tables/NonStandardColumnListPage'
import { TableDetailPage } from '@/features/tables/TableDetailPage'
import { TableListPage } from '@/features/tables/TableListPage'
import { TermListPage } from '@/features/terms/TermListPage'
import { WordListPage } from '@/features/words/WordListPage'
import { MyPage } from '@/pages/MyPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'
import { useMenus } from './MenuContext'

/**
 * 화면 경로 → 화면.
 *
 * 메뉴는 DB에서 관리하므로(관리자가 경로를 정한다) 메뉴의 경로가 여기 등록된 경로와 같아야 실제 화면이 나온다.
 * 등록되지 않은 경로의 메뉴는 "준비 중" 화면을 보여준다. 새 화면을 만들면 여기에 추가한다.
 */
const PAGES: Record<string, ReactElement> = {
  '/words': <WordListPage />,
  '/domains': <DomainListPage />,
  '/terms': <TermListPage />,
  '/codes': <CodeGroupListPage />,
  '/tables': <TableListPage />,
  '/non-standard-columns': <NonStandardColumnListPage />,
  '/requests': <RequestListPage />,
  '/histories': <HistoryListPage />,
  // 메뉴 관리는 관리자만 쓰는 화면이라 주소로 바로 들어와도 막는다 (서버도 같은 규칙으로 막는다).
  '/menus': (
    <RequireAuth adminOnly>
      <MenuManagePage />
    </RequireAuth>
  ),
  '/menu-permissions': (
    <RequireAuth adminOnly>
      <MenuPermissionPage />
    </RequireAuth>
  ),
}

/** 화면이 있는 경로 목록. 메뉴 관리 화면에서 경로를 고를 때 보여준다. */
export const SCREEN_PATHS = Object.keys(PAGES)

/**
 * 등록된 화면이 없는 주소. 메뉴에 있는 경로면 "준비 중", 아니면 "없는 화면"이다.
 * 메뉴만 먼저 만들어 두고 화면을 나중에 붙일 수 있다.
 */
function UnknownScreen() {
  const { pathname } = useLocation()
  const { findByPath } = useMenus()
  const match = findByPath(pathname)

  return match ? <PlaceholderPage title={match.menu.name ?? '준비 중'} /> : <NotFoundPage />
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
      {SCREEN_PATHS.map((path) => (
        <Route key={path} path={path.slice(1)} element={PAGES[path]} />
      ))}
      {/* 메뉴에 없는 상세 화면. 목록에서 링크로 열리며 탭 이름은 화면이 useTabTitle로 채운다. */}
      <Route path="tables/:id" element={<TableDetailPage />} />
      <Route path="codes/:id" element={<CodeGroupDetailPage />} />
      <Route path="me" element={<MyPage />} />
      <Route path="*" element={<UnknownScreen />} />
    </Routes>
  )
}
