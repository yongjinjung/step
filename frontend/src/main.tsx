import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { App as AntdApp, ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router'
import { isApiError } from '@/api/errors'
import { AuthProvider } from '@/auth/AuthContext'
import { router } from '@/router'
import './index.css'

/**
 * 서버 데이터 캐시.
 * - 백엔드 업무 오류(4xx 등 ErrorResponse)는 다시 시도해도 같으므로 재시도하지 않는다. 네트워크 오류만 2번까지 재시도한다.
 * - 목록은 30초 동안 새것으로 보고, 창을 다시 볼 때마다 재조회하지 않는다.
 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => !isApiError(error) && failureCount < 2,
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
  },
})

// 감싸는 순서: 테마·로케일(ConfigProvider) → 알림 API(AntdApp) → 서버 캐시 → 로그인 상태 → 라우터.
// AuthProvider가 로그아웃 때 캐시를 비우므로 QueryClientProvider 안에 둔다.
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider locale={koKR} theme={{ token: { colorPrimary: '#1f6feb', borderRadius: 6 } }}>
      <AntdApp>
        <QueryClientProvider client={queryClient}>
          <AuthProvider>
            <RouterProvider router={router} />
          </AuthProvider>
          <ReactQueryDevtools initialIsOpen={false} buttonPosition="bottom-left" />
        </QueryClientProvider>
      </AntdApp>
    </ConfigProvider>
  </StrictMode>,
)
