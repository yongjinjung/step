import { useQueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'
import type { paths } from '@/api/schema'

/**
 * 조회 캐시를 무효화해 다시 불러오게 한다. 등록·수정·삭제·승인 뒤에 쓴다.
 *
 * openapi-react-query의 조회 키는 `['get', 경로, 파라미터]`라서 `['get', 경로]`로 무효화하면
 * 그 경로의 모든 조회(검색 조건·페이지가 다른 목록 전체)가 다시 불러와진다.
 *
 * @example
 * const invalidate = useInvalidateApi()
 * invalidate('/api/words', '/api/words/{id}', '/api/histories')
 */
export function useInvalidateApi() {
  const queryClient = useQueryClient()
  return useCallback(
    (...apiPaths: (keyof paths)[]) => {
      for (const path of apiPaths) {
        void queryClient.invalidateQueries({ queryKey: ['get', path] })
      }
    },
    [queryClient],
  )
}
