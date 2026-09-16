/**
 * 백엔드 LocalDateTime 문자열("2026-09-15T13:14:23.53444")을 "2026-09-15 13:14:23"으로 보여준다.
 * 서버가 시간대 없이 서버 시각을 주므로 브라우저 시간대로 변환하지 않고 글자만 다듬는다.
 */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

/**
 * 변경 이력 스냅샷의 값 하나를 글자로 보여준다.
 * 빈 값은 "(없음)", 여부 값은 Y/N, 목록(용어의 구성 단어 ID)은 쉼표로 잇는다.
 *
 * @param maxLength 이보다 길면 잘라서 "…"를 붙인다. 타임라인은 짧게, 이력 화면의 펼친 행은 길게 쓴다
 */
export function formatSnapshotValue(value: unknown, maxLength = 100): string {
  if (value === null || value === undefined || value === '') {
    return '(없음)'
  }
  if (typeof value === 'boolean') {
    return value ? 'Y' : 'N'
  }
  const text = Array.isArray(value) ? value.join(', ') : typeof value === 'object' ? JSON.stringify(value) : String(value)
  return text.length > maxLength ? `${text.slice(0, maxLength)}…` : text
}
