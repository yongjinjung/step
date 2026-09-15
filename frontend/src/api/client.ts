import createFetchClient, { type Middleware } from 'openapi-fetch'
import createQueryHooks from 'openapi-react-query'
import { clearToken, loadToken } from '@/auth/token'
import type { paths } from './schema'

/**
 * 백엔드 API 클라이언트.
 *
 * - `apiClient`: 타입이 붙은 fetch 클라이언트 (`apiClient.GET('/api/words', { params: { query } })`).
 * - `$api`: 같은 클라이언트로 만든 TanStack Query 훅 (`$api.useQuery('get', '/api/auth/me')`).
 *
 * 경로·파라미터·요청 본문·응답 타입은 모두 `schema.d.ts`(백엔드 OpenAPI 문서에서 생성)에서 온다.
 * 백엔드 API가 바뀌면 `npm run api:generate`로 타입을 다시 만들고, 어긋난 곳은 타입 검사로 찾는다.
 * 호출이 실패하면 응답 본문(백엔드 ErrorResponse)이 오류로 던져진다 (`@/api/errors` 참고).
 */

/** 401 응답을 받았을 때 실행할 함수. AuthProvider가 로그아웃 처리를 등록한다. */
let unauthorizedHandler: (() => void) | null = null

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler
}

/** 토큰이 만료·무효가 된 경우의 공통 처리. 파일 다운로드처럼 이 클라이언트를 거치지 않는 요청도 쓴다. */
export function handleUnauthorized(): void {
  clearToken()
  unauthorizedHandler?.()
}

/** 모든 요청에 Bearer 토큰을 붙이고, 401이면 로그아웃시킨다. */
const authMiddleware: Middleware = {
  onRequest({ request }) {
    const token = loadToken()
    if (token) {
      request.headers.set('Authorization', `Bearer ${token}`)
    }
    return request
  },
  onResponse({ request, response }) {
    // 로그인 요청의 401은 "아이디 또는 비밀번호 오류"라서 로그인 화면이 직접 보여준다.
    const isLoginRequest = new URL(request.url).pathname === '/api/auth/login'
    if (response.status === 401 && !isLoginRequest) {
      handleUnauthorized()
    }
    return response
  },
}

// baseUrl을 비워 현재 출처(개발 중에는 Vite 서버)로 요청한다. /api는 Vite 프록시가 백엔드로 넘긴다.
export const apiClient = createFetchClient<paths>({ baseUrl: '' })
apiClient.use(authMiddleware)

export const $api = createQueryHooks(apiClient)
