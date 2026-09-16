import type { components } from '@/api/schema'

export type DomainRequest = components['schemas']['DomainRequest']
export type DomainResponse = components['schemas']['DomainResponse']

/** 도메인 필드의 화면 표시 이름. 변경 이력의 필드 이름(DomainSnapshot)도 이 값으로 보여준다. */
export const DOMAIN_FIELD_LABELS: Record<string, string> = {
  name: '도메인명',
  classifierWordId: '분류어 단어 ID',
  classifierWordName: '분류어',
  dataType: '데이터 타입',
  dataLength: '길이',
  dataScale: '소수점',
  description: '설명',
}

/**
 * 업무 오류 코드 → 폼 필드. 해당 필드 아래에 서버 메시지를 보여준다.
 * 여기에 없는 코드(DOMAIN_IN_USE, REQUEST_ALREADY_PENDING 등)는 알림으로 보여준다.
 */
export const DOMAIN_ERROR_FIELDS: Record<string, keyof DomainRequest> = {
  DUPLICATE_DOMAIN_NAME: 'name',
  INVALID_DOMAIN_SPEC: 'dataLength',
  NOT_CLASSIFIER_WORD: 'classifierWordId',
  WORD_NOT_FOUND: 'classifierWordId',
  DOMAIN_CLASSIFIER_IN_USE: 'classifierWordId',
}
