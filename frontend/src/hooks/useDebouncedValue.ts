import { useEffect, useState } from 'react'

/**
 * 값이 바뀐 뒤 `delay` 동안 더 바뀌지 않으면 그 값을 돌려준다.
 * 검색어를 입력하는 동안 글자마다 API를 부르지 않도록 할 때 쓴다.
 */
export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delay)
    return () => window.clearTimeout(timer)
  }, [value, delay])

  return debounced
}
