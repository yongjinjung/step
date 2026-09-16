import { Result } from 'antd'
import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './AuthContext'

interface RequireAuthProps {
  /** true면 ADMIN만 들어갈 수 있다 */
  adminOnly?: boolean
  /** 보호할 화면. 넘기지 않으면 중첩 라우트(Outlet)를 그린다 */
  children?: ReactNode
}

/**
 * 보호된 라우트. 로그인하지 않았으면 로그인 화면으로 보내고, 로그인 후 원래 가려던 주소로 돌아오게 한다.
 * 역할 검사는 화면 표시용이다. 서버가 같은 규칙으로 막으므로 이 검사를 우회해도 데이터는 바뀌지 않는다.
 *
 * 라우트 가드(`<Route element={<RequireAuth />}>`)와 화면 감싸기(`<RequireAuth adminOnly>{화면}</RequireAuth>`,
 * 탭 화면 정의인 `layouts/screens.tsx`에서 쓴다) 두 가지로 쓴다.
 */
export function RequireAuth({ adminOnly = false, children }: RequireAuthProps) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  if (adminOnly && !user.isAdmin) {
    return <Result status="403" title="권한이 없습니다" subTitle="관리자만 사용할 수 있는 화면입니다." />
  }
  return <>{children ?? <Outlet />}</>
}
