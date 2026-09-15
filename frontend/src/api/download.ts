import { loadToken } from '@/auth/token'
import { handleUnauthorized } from './client'
import type { ApiError } from './errors'

/**
 * 인증이 필요한 파일(엑셀) 다운로드.
 *
 * `<a href="/api/excel/words">`로는 Authorization 헤더를 보낼 수 없어 401이 난다.
 * 그래서 fetch로 파일을 받아 임시 URL을 만든 뒤 링크를 눌러 저장한다.
 * 파일 이름은 서버의 Content-Disposition(`filename*=UTF-8''...`)에서 한글 그대로 읽는다.
 *
 * @param path   API 경로 (예: `/api/excel/words`)
 * @param params 쿼리 파라미터. 비어 있는 값은 보내지 않는다
 * @throws ApiError 서버가 오류를 응답했을 때 (예: EXCEL_TOO_MANY_ROWS)
 */
export async function downloadFile(
  path: string,
  params: Record<string, string | number | boolean | null | undefined> = {},
): Promise<void> {
  const url = new URL(path, window.location.origin)
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value))
    }
  }

  const token = loadToken()
  const response = await fetch(url, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
  if (response.status === 401) {
    handleUnauthorized()
  }
  if (!response.ok) {
    const fallback: ApiError = { code: 'DOWNLOAD_FAILED', message: '파일을 내려받지 못했습니다.', fieldErrors: [] }
    throw await response.json().catch(() => fallback)
  }

  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = parseFileName(response.headers.get('Content-Disposition')) ?? 'download.xlsx'
  link.click()
  // 바로 해제하면 일부 브라우저에서 저장이 취소되므로 잠시 뒤에 해제한다.
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
}

/** Content-Disposition에서 파일 이름을 읽는다. RFC 5987 인코딩(filename*)을 우선한다. */
function parseFileName(disposition: string | null): string | null {
  if (!disposition) {
    return null
  }
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (encoded) {
    return decodeURIComponent(encoded[1])
  }
  const plain = /filename="?([^";]+)"?/i.exec(disposition)
  return plain ? plain[1] : null
}
