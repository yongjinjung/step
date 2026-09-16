import type { components, paths } from '@/api/schema'

export type ChangeRequestResponse = components['schemas']['ChangeRequestResponse']
export type RequestStatus = NonNullable<ChangeRequestResponse['status']>
export type RequestTargetType = NonNullable<ChangeRequestResponse['targetType']>
export type RequestType = NonNullable<ChangeRequestResponse['requestType']>

interface TagSpec {
  label: string
  color: string
}

/** 신청 상태 (백엔드 RequestStatus). 대기만 처리할 수 있다 */
export const REQUEST_STATUSES: Record<RequestStatus, TagSpec> = {
  REQUESTED: { label: '대기', color: 'processing' },
  APPROVED: { label: '승인', color: 'success' },
  REJECTED: { label: '반려', color: 'error' },
  CANCELED: { label: '취소', color: 'default' },
}

/** 신청 대상 (백엔드 TargetType) */
export const REQUEST_TARGET_TYPES: Record<RequestTargetType, TagSpec> = {
  WORD: { label: '단어', color: 'geekblue' },
  DOMAIN: { label: '도메인', color: 'purple' },
  TERM: { label: '용어', color: 'cyan' },
}

/** 신청 종류 (백엔드 RequestType). 변경 이력의 등록·수정·삭제와 같은 색을 쓴다 */
export const REQUEST_TYPES: Record<RequestType, TagSpec> = {
  CREATE: { label: '등록', color: 'green' },
  UPDATE: { label: '수정', color: 'blue' },
  DELETE: { label: '삭제', color: 'red' },
}

/**
 * 승인으로 대상이 바뀌면 다시 불러올 조회 경로.
 * 도메인·용어는 용어 목록과 컬럼 비표준 검출에도 영향을 준다(각 폼 모달의 무효화와 같다).
 */
export const TARGET_QUERY_PATHS: Record<RequestTargetType, (keyof paths)[]> = {
  WORD: ['/api/words', '/api/words/{id}'],
  DOMAIN: ['/api/domains', '/api/domains/{id}', '/api/terms', '/api/tables/{tableId}/columns', '/api/columns/non-standard'],
  TERM: ['/api/terms', '/api/terms/{id}', '/api/tables/{tableId}/columns', '/api/columns/non-standard'],
}

/**
 * 신청 내용(payload)을 일반 객체로 꺼낸다.
 * 생성 타입은 JsonNode 클래스 모양이지만 실제 응답은 대상별 요청 본문 JSON이다. 삭제 신청은 null이다.
 */
export function payloadOf(request: ChangeRequestResponse): Record<string, unknown> | undefined {
  const payload = request.payload as unknown
  return payload && typeof payload === 'object' ? (payload as Record<string, unknown>) : undefined
}

/**
 * 목록에 보여줄 대상 요약. 응답에는 대상 이름이 없으므로 대상 번호와 신청 내용의 이름으로 만든다.
 * 예: `#12 고객`(단어 수정), `#12`(삭제), `단어 2개 조합`(용어 등록 대기, 이름은 승인 시 생성)
 */
export function summarizeTarget(request: ChangeRequestResponse): string {
  const payload = payloadOf(request)
  const name = typeof payload?.name === 'string' ? payload.name : undefined
  const parts = [request.targetId !== undefined && request.targetId !== null ? `#${request.targetId}` : undefined, name]
  if (!name && request.targetType === 'TERM' && Array.isArray(payload?.wordIds)) {
    parts.push(`단어 ${payload.wordIds.length}개 조합`)
  }
  return parts.filter(Boolean).join(' ') || '-'
}
