
import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './AuthContext'

interface RequireAuthProps {
  /** 보호할 화면. 넘기지 않으면 중첩 라우트(Outlet)를 그린다 */
  children?: ReactNode
}

/**
 * 보호된 라우트. 로그인하지 않았으면 로그인 화면으로 보내고, 로그인 후 원래 가려던 주소로 돌아오게 한다.
 *
 * 화면별 권한은 여기서 보지 않는다. 메뉴의 조회 권한으로 `layouts/screens.tsx`의 `RequireMenu`가 막고,
 * 실제 기능 권한은 서버가 API마다 검사한다.
 */
export function RequireAuth({ children }: RequireAuthProps) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  return <>{children ?? <Outlet />}</>
}
