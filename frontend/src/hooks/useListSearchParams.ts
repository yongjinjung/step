import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router'

/**
 * 목록 화면의 검색 조건과 페이지를 URL 쿼리스트링에 둔다.
 * 새로고침, 뒤로 가기, 주소 공유를 해도 같은 조건의 목록이 보인다.
 *
 * - `page`는 화면 기준 1부터다. API(`page`는 0부터)에 넘길 때는 `page - 1`을 쓴다.
 * - 검색 조건을 바꾸면 첫 페이지로 돌아간다.
 * - 기본값(1페이지, 기본 크기)과 빈 조건은 주소에 남기지 않는다.
 *
 * @param filterKeys 검색 조건 이름 목록. 렌더링마다 새 배열을 만들지 않도록 컴포넌트 밖 상수로 넘긴다
 * @param defaultSize 기본 페이지 크기
 */
export function useListSearchParams<K extends string>(filterKeys: readonly K[], defaultSize = 20) {
  const [searchParams, setSearchParams] = useSearchParams()

  const filters = useMemo(
    () => Object.fromEntries(filterKeys.map((key) => [key, searchParams.get(key) ?? undefined])) as Record<K, string | undefined>,
    [filterKeys, searchParams],
  )
  const page = Math.max(1, Number(searchParams.get('page')) || 1)
  const size = Math.max(1, Number(searchParams.get('size')) || defaultSize)

  /** 검색 조건을 바꾼다. 값이 비어 있으면 주소에서 뺀다. 페이지는 1로 돌아간다. */
  const setFilters = useCallback(
    (next: Partial<Record<K, string | undefined>>) => {
      setSearchParams((previous) => {
        const params = new URLSearchParams(previous)
        for (const key of filterKeys) {
          const value = next[key]?.trim()
          if (value) {
            params.set(key, value)
          } else {
            params.delete(key)
          }
        }
        params.delete('page')
        return params
      })
    },
    [filterKeys, setSearchParams],
  )

  /** 페이지와 크기를 바꾼다 (Table의 pagination onChange에 연결). */
  const setPaging = useCallback(
    (nextPage: number, nextSize: number) => {
      setSearchParams((previous) => {
        const params = new URLSearchParams(previous)
        // 크기를 바꾸면 현재 페이지 번호가 의미 없어지므로 첫 페이지로 간다.
        const sizeChanged = nextSize !== size
        const targetPage = sizeChanged ? 1 : nextPage
        if (targetPage > 1) {
          params.set('page', String(targetPage))
        } else {
          params.delete('page')
        }
        if (nextSize !== defaultSize) {
          params.set('size', String(nextSize))
        } else {
          params.delete('size')
        }
        return params
      })
    },
    [defaultSize, setSearchParams, size],
  )

  return { filters, page, size, setFilters, setPaging }
}
