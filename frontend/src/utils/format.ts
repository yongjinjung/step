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
