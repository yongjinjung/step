import { Result, Skeleton } from 'antd'
import type { ReactElement } from 'react'
import { Route, Routes, useLocation } from 'react-router'
import { CodeGroupDetailPage } from '@/features/codes/CodeGroupDetailPage'
import { CodeGroupListPage } from '@/features/codes/CodeGroupListPage'
import { DomainListPage } from '@/features/domains/DomainListPage'
import { HistoryListPage } from '@/features/histories/HistoryListPage'
import { MenuManagePage } from '@/features/menus/MenuManagePage'
import { MenuPermissionPage } from '@/features/menus/MenuPermissionPage'
import { RequestListPage } from '@/features/requests/RequestListPage'
import { RoleListPage } from '@/features/roles/RoleListPage'
import { AntdSamplePage } from '@/features/samples/AntdSamplePage'
import { BackendGuideSamplePage } from '@/features/samples/BackendGuideSamplePage'
import { DataFetchingSamplePage } from '@/features/samples/DataFetchingSamplePage'
import { FormsSamplePage } from '@/features/samples/FormsSamplePage'
import { ReactSamplePage } from '@/features/samples/ReactSamplePage'
import { RouterParamDemoPage, RouterSamplePage } from '@/features/samples/RouterSamplePage'
import { TypeScriptSamplePage } from '@/features/samples/TypeScriptSamplePage'
import { ViteSamplePage } from '@/features/samples/ViteSamplePage'
import { NonStandardColumnListPage } from '@/features/tables/NonStandardColumnListPage'
import { TableDetailPage } from '@/features/tables/TableDetailPage'
import { TableListPage } from '@/features/tables/TableListPage'
import { TermListPage } from '@/features/terms/TermListPage'
import { UserListPage } from '@/features/users/UserListPage'
import { WordListPage } from '@/features/words/WordListPage'
import { MyPage } from '@/pages/MyPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'
import { useMenus } from './MenuContext'
import { PrescriptionListPage } from '@/features/prescriptions/PrescriptionListPage'
import { DispensingListPage } from '@/features/dispensings/DispensingListPage'
import { InspectionListPage } from '@/features/inspections/InspectionListPage'
import { IssueListPage } from '@/features/issues/IssueListPage'
import { ReturnListPage } from '@/features/returns/ReturnListPage'

/**
 * 화면 경로 → 화면.
 *
 * 메뉴는 DB에서 관리하므로(관리자가 경로를 정한다) 메뉴의 경로가 여기 등록된 경로와 같아야 실제 화면이 나온다.
 * 등록되지 않은 경로의 메뉴는 "준비 중" 화면을 보여준다. 새 화면을 만들면 여기에 추가한다.
 *
 * 경로는 권한의 기준이기도 하다 (백엔드 {@code MenuPaths}). 그 메뉴의 조회 권한이 없으면 주소로 들어와도 막는다.
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
  '/users': <UserListPage />,
  '/roles': <RoleListPage />,
  '/menus': <MenuManagePage />,
  '/menu-permissions': <MenuPermissionPage />,
  // 약제 업무: 처방 → 조제 → 검수 → 불출 → 반납
  '/prescriptions': <PrescriptionListPage />,
  '/dispensings': <DispensingListPage />,
  '/inspections': <InspectionListPage />,
  '/issues': <IssueListPage />,
  '/returns': <ReturnListPage />,
    // 개발 참고용 샘플 화면. 업무 데이터를 다루지 않고 화면 안에서만 동작한다.
  '/react': <ReactSamplePage />,
  '/typescript': <TypeScriptSamplePage />,
  '/vite': <ViteSamplePage />,
  '/antdesign': <AntdSamplePage />,
  '/router': <RouterSamplePage />,
  '/data-fetching': <DataFetchingSamplePage />,
  '/forms': <FormsSamplePage />,
  '/backend-guide': <BackendGuideSamplePage />,
}

/** 화면이 있는 경로 목록. 메뉴 관리 화면에서 경로를 고를 때 보여준다. */
export const SCREEN_PATHS = Object.keys(PAGES)

/**
 * 메뉴의 조회 권한이 있어야 화면을 그린다.
 *
 * 메뉴에서 안 보이는 화면이라도 주소를 직접 치면 열리므로 여기서 한 번 더 막는다.
 * 서버도 API마다 같은 권한을 검사하므로 이 검사는 화면 안내용이다.
 */
function RequireMenu({ path, children }: { path: string; children: ReactElement }) {
  const { can, loading } = useMenus()

  if (loading) {
    return <Skeleton active className="workspace-pane" />
  }
  if (!can(path, 'read')) {
    return (
      <Result
        status="403"
        title="권한이 없습니다"
        subTitle="이 화면을 볼 권한이 없습니다. 필요하면 관리자에게 역할을 요청하세요."
      />
    )
  }
  return children
}

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
        <Route key={path} path={path.slice(1)} element={<RequireMenu path={path}>{PAGES[path]}</RequireMenu>} />
      ))}
      {/* 메뉴에 없는 상세 화면. 목록에서 링크로 열리며 권한은 상위 메뉴 기준이다. */}
      <Route
        path="tables/:id"
        element={
          <RequireMenu path="/tables">
            <TableDetailPage />
          </RequireMenu>
        }
      />
      <Route
        path="codes/:id"
        element={
          <RequireMenu path="/codes">
            <CodeGroupDetailPage />
          </RequireMenu>
        }
      />
      {/* 라우팅 샘플의 useParams 데모. 상세 라우트가 메뉴 목록(PAGES) 없이 어떻게 붙는지 보여준다. */}
      <Route
        path="router-demo/:id"
        element={
          <RequireMenu path="/router">
            <RouterParamDemoPage />
          </RequireMenu>
        }
      />
      {/* 내 정보는 메뉴와 무관하게 누구나 본다. */}
      <Route path="me" element={<MyPage />} />
      <Route path="*" element={<UnknownScreen />} />
    </Routes>
  )
}
