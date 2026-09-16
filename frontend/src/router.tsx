import { createBrowserRouter, Navigate } from 'react-router'
import { RequireAuth } from '@/auth/RequireAuth'
import { AppLayout } from '@/layouts/AppLayout'
import { NAV_ITEMS } from '@/layouts/navigation'
import { LoginPage } from '@/pages/LoginPage'

/**
 * 라우트 구성.
 *
 * /login은 공개, 나머지는 RequireAuth 아래에서 로그인이 필요하다.
 * 로그인 후 모든 주소는 AppLayout 하나가 받는다. 어떤 화면을 그릴지는 화면 탭이 정한다
 * (`layouts/screens.tsx`의 ScreenRoutes). 열어 둔 탭을 동시에 그려야 해서 Outlet(주소 하나 = 화면 하나)을 쓰지 않는다.
 * 새 화면은 `layouts/screens.tsx`에 등록한다.
 */
export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      // 첫 주소는 메뉴의 첫 화면으로 보낸다. 탭은 그 뒤에 열린다.
      { index: true, element: <Navigate to={NAV_ITEMS[0].path} replace /> },
      { path: '*', element: <AppLayout /> },
    ],
  },
])
