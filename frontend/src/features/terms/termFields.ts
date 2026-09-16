import type { components } from '@/api/schema'

export type TermRequest = components['schemas']['TermRequest']
export type TermResponse = components['schemas']['TermResponse']

/** 구성 단어 최대 개수 (백엔드 TermRequest @Size(max = 10)) */
export const MAX_TERM_WORDS = 10
/** 용어명 최대 길이 (백엔드 Term.MAX_NAME_LENGTH) */
export const MAX_TERM_NAME_LENGTH = 200
/** 영문약어 최대 길이. 컬럼명의 기준이라 MySQL 식별자 한도를 따른다 (백엔드 Term.MAX_ABBREVIATION_LENGTH) */
export const MAX_TERM_ABBREVIATION_LENGTH = 64

interface WordParts {
  name?: string
  abbreviation?: string
}

/** 용어명 미리보기: 단어명을 이어 붙인다 (고객+번호 → 고객번호). 백엔드 Term.composeName과 같아야 한다. */
export function composeTermName(words: WordParts[]): string {
  return words.map((word) => word.name ?? '').join('')
}

/** 영문약어 미리보기: 단어 약어를 '_'로 잇는다 (CUST+NO → CUST_NO). 백엔드 Term.composeAbbreviation과 같아야 한다. */
export function composeTermAbbreviation(words: WordParts[]): string {
  return words.map((word) => word.abbreviation ?? '').join('_')
}

/**
 * 용어 필드의 화면 표시 이름.
 * 변경 이력의 필드 이름(TermSnapshot: name, abbreviation, wordIds, domainId, domainName, description)도 이 값으로 보여준다.
 */
export const TERM_FIELD_LABELS: Record<string, string> = {
  name: '용어명',
  abbreviation: '영문약어',
  words: '구성 단어',
  wordIds: '구성 단어 ID',
  domainId: '도메인 ID',
  domainName: '도메인',
  dataType: '데이터 타입',
  description: '설명',
}

/**
 * 업무 오류 코드 → 폼 필드. 해당 필드 아래에 서버 메시지를 보여준다.
 * 용어명·영문약어는 구성 단어에서 만들어지므로 이름 관련 오류는 구성 단어 목록 아래에 보여준다.
 * 여기에 없는 코드(TERM_IN_USE, REQUEST_ALREADY_PENDING 등)는 알림으로 보여준다.
 */
export const TERM_ERROR_FIELDS: Record<string, keyof TermRequest> = {
  DUPLICATE_TERM_NAME: 'wordIds',
  DUPLICATE_TERM_ABBREVIATION: 'wordIds',
  TERM_LAST_WORD_NOT_CLASSIFIER: 'wordIds',
  TERM_TOO_LONG: 'wordIds',
  WORD_NOT_FOUND: 'wordIds',
  TERM_DOMAIN_MISMATCH: 'domainId',
  DOMAIN_NOT_FOUND: 'domainId',
}
