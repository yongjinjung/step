import { fileURLToPath, URL } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

/**
 * Vite 설정.
 *
 * 개발 서버는 /api 요청을 백엔드로 넘긴다(프록시). 브라우저 입장에서는 같은 출처라 백엔드에 CORS 설정이 필요 없다.
 * 백엔드 주소 기본값은 http://localhost:8080이며, 다른 포트로 띄웠다면 .env.local에
 * VITE_API_PROXY_TARGET=http://localhost:8081 처럼 적는다.
 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [react()],
    resolve: {
      // import '@/api/client' 처럼 src 기준 경로를 쓴다 (tsconfig.app.json의 paths와 같게 유지).
      alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': { target: apiTarget, changeOrigin: true },
      },
    },
  }
})
