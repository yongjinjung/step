import { createBrowserRouter, Navigate, type RouteObject } from 'react-router'
import { RequireAuth } from '@/auth/RequireAuth'
import { AppLayout } from '@/layouts/AppLayout'
import { NAV_ITEMS } from '@/layouts/navigation'
import { LoginPage } from '@/pages/LoginPage'
import { MyPage } from '@/pages/MyPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'

/**
 * 라우트 구성.
 *
 * /login은 공개, 나머지는 RequireAuth 아래에서 로그인이 필요하다.
 * 메뉴 화면은 우선 준비 중 화면으로 연결해 두고, 화면을 만들면 해당 경로의 element만 바꾼다.
 */
const menuRoutes: RouteObject[] = NAV_ITEMS.map((item) => {
  const route: RouteObject = { path: item.path.slice(1), element: <PlaceholderPage item={item} /> }
  return item.adminOnly ? { element: <RequireAuth adminOnly />, children: [route] } : route
})

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <Navigate to="/words" replace /> },
          ...menuRoutes,
          { path: 'me', element: <MyPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])
