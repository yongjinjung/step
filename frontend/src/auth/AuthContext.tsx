import { useQueryClient } from '@tanstack/react-query'
import { createContext, use, useCallback, useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import { setUnauthorizedHandler } from '@/api/client'
import { clearToken, decodeToken, loadValidUser, saveToken, type AuthUser } from './token'

interface AuthContextValue {
  /** 로그인한 사용자. 로그인 전이면 null */
  user: AuthUser | null
  /** 로그인 API로 받은 토큰을 저장하고 로그인 상태로 바꾼다 */
  login: (accessToken: string) => void
  /** 토큰과 캐시를 지우고 로그아웃한다. 보호된 화면에 있었다면 로그인 화면으로 이동한다(RequireAuth) */
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

/**
 * 로그인 상태 제공자.
 *
 * - 시작할 때 sessionStorage의 토큰을 읽어 로그인 상태를 복원한다(만료됐으면 버린다).
 * - 토큰 만료 시각이 되면 자동으로 로그아웃한다.
 * - API가 401을 주면(토큰 무효·만료) 로그아웃한다.
 */
export function AuthProvider({ children }: PropsWithChildren) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<AuthUser | null>(() => loadValidUser())

  const logout = useCallback(() => {
    clearToken()
    setUser(null)
    // 다른 사용자로 다시 로그인했을 때 이전 사용자의 조회 결과가 보이지 않게 캐시를 비운다.
    queryClient.clear()
  }, [queryClient])

  const login = useCallback((accessToken: string) => {
    const decoded = decodeToken(accessToken)
    if (!decoded) {
      throw new Error('로그인 토큰을 해석할 수 없습니다.')
    }
    saveToken(accessToken)
    setUser(decoded)
  }, [])

  // 만료 시각에 자동 로그아웃
  useEffect(() => {
    if (!user) {
      return
    }
    const timer = window.setTimeout(logout, Math.max(0, user.expiresAt - Date.now()))
    return () => window.clearTimeout(timer)
  }, [user, logout])

  // API 401 → 로그아웃
  useEffect(() => {
    setUnauthorizedHandler(logout)
    return () => setUnauthorizedHandler(null)
  }, [logout])

  const value = useMemo(() => ({ user, login, logout }), [user, login, logout])
  return <AuthContext value={value}>{children}</AuthContext>
}

export function useAuth(): AuthContextValue {
  const context = use(AuthContext)
  if (!context) {
    throw new Error('useAuth는 AuthProvider 안에서만 쓸 수 있습니다.')
  }
  return context
}
