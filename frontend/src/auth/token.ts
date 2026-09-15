/**
 * 액세스 토큰 보관과 해석.
 *
 * 토큰은 sessionStorage에 둔다. 새로고침해도 로그인이 유지되고, 브라우저 탭을 닫으면 사라진다.
 * 백엔드에 리프레시 토큰이 없어 만료(기본 1시간)되면 다시 로그인해야 한다.
 * 토큰 안의 역할(roles)은 메뉴·버튼 표시에만 쓰며, 실제 권한 검사는 서버가 한다.
 */

const STORAGE_KEY = 'dictionary.accessToken'

/** 서버 역할. JWT roles 클레임 값과 같다. */
export type Role = 'ADMIN' | 'USER'

/** 토큰에서 읽은 로그인 사용자. */
export interface AuthUser {
  /** 아이디 (JWT subject, 소문자) */
  username: string
  roles: Role[]
  isAdmin: boolean
  /** 만료 시각 (epoch 밀리초) */
  expiresAt: number
}

export function saveToken(token: string): void {
  sessionStorage.setItem(STORAGE_KEY, token)
}

export function loadToken(): string | null {
  return sessionStorage.getItem(STORAGE_KEY)
}

export function clearToken(): void {
  sessionStorage.removeItem(STORAGE_KEY)
}

/**
 * JWT의 payload를 읽는다. 서명은 검증하지 않는다(검증은 서버 몫). 형식이 잘못됐으면 null.
 * payload는 base64url이라 atob가 읽을 수 있게 문자와 패딩을 바꾼다.
 */
export function decodeToken(token: string): AuthUser | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) {
      return null
    }
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(payload.length / 4) * 4, '=')
    const claims = JSON.parse(atob(base64)) as { sub?: string; roles?: unknown; exp?: number }
    if (!claims.sub || typeof claims.exp !== 'number') {
      return null
    }
    const roles = (Array.isArray(claims.roles) ? claims.roles : []).filter(
      (role): role is Role => role === 'ADMIN' || role === 'USER',
    )
    return { username: claims.sub, roles, isAdmin: roles.includes('ADMIN'), expiresAt: claims.exp * 1000 }
  } catch {
    return null
  }
}

/** 저장된 토큰이 있고 아직 만료되지 않았으면 사용자 정보를, 아니면 토큰을 지우고 null을 돌려준다. */
export function loadValidUser(): AuthUser | null {
  const token = loadToken()
  const user = token ? decodeToken(token) : null
  if (!user || user.expiresAt <= Date.now()) {
    clearToken()
    return null
  }
  return user
}
