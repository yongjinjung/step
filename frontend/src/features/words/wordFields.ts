import type { components } from '@/api/schema'

export type WordRequest = components['schemas']['WordRequest']
export type WordResponse = components['schemas']['WordResponse']

/** 단어 필드의 화면 표시 이름. 변경 이력의 필드 이름도 이 값으로 보여준다. */
export const WORD_FIELD_LABELS: Record<string, string> = {
  name: '단어명',
  abbreviation: '영문약어',
  englishName: '영문명',
  classifier: '분류어 여부',
  description: '설명',
}

/**
 * 업무 오류 코드를 폼 필드에 연결한다. 해당 필드 아래에 서버 메시지를 보여준다.
 * 여기에 없는 코드는 화면 위 알림으로 보여준다.
 */
export const WORD_ERROR_FIELDS: Record<string, keyof WordRequest> = {
  DUPLICATE_WORD_NAME: 'name',
  DUPLICATE_WORD_ABBREVIATION: 'abbreviation',
  WORD_USED_BY_TERM: 'name',
  CLASSIFIER_WORD_IN_USE: 'classifier',
}

/** 백엔드 WordRequest 검증과 같은 규칙 (영문약어: 영문자로 시작하는 영문/숫자). */
export const ABBREVIATION_PATTERN = /^[A-Za-z][A-Za-z0-9]*$/
