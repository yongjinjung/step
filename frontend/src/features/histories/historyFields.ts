import type { components } from '@/api/schema'
import { CODE_GROUP_FIELD_LABELS, CODE_VALUE_FIELD_LABELS } from '@/features/codes/codeFields'
import { DOMAIN_FIELD_LABELS } from '@/features/domains/domainFields'
import { COLUMN_FIELD_LABELS, TABLE_FIELD_LABELS } from '@/features/tables/tableFields'
import { TERM_FIELD_LABELS } from '@/features/terms/termFields'
import { WORD_FIELD_LABELS } from '@/features/words/wordFields'

export type ChangeHistoryResponse = components['schemas']['ChangeHistoryResponse']
export type HistoryTargetType = NonNullable<ChangeHistoryResponse['targetType']>
export type HistoryChangeType = NonNullable<ChangeHistoryResponse['changeType']>

interface TagSpec {
  label: string
  color: string
}

/** 이력 대상 (백엔드 HistoryTargetType). 표준 데이터 색은 신청 화면과 같다 */
export const HISTORY_TARGET_TYPES: Record<HistoryTargetType, TagSpec> = {
  WORD: { label: '단어', color: 'geekblue' },
  DOMAIN: { label: '도메인', color: 'purple' },
  TERM: { label: '용어', color: 'cyan' },
  TABLE: { label: '테이블', color: 'gold' },
  COLUMN: { label: '컬럼', color: 'lime' },
  CODE_GROUP: { label: '코드 그룹', color: 'magenta' },
  CODE_VALUE: { label: '코드값', color: 'volcano' },
}

/** 변경 종류. 신청 종류·이력 타임라인과 같은 색을 쓴다 */
export const HISTORY_CHANGE_TYPES: Record<HistoryChangeType, TagSpec> = {
  CREATE: { label: '등록', color: 'green' },
  UPDATE: { label: '수정', color: 'blue' },
  DELETE: { label: '삭제', color: 'red' },
}

/** 대상별 스냅샷 필드 표시 이름. 각 기능의 필드 정의를 그대로 쓴다 */
export const HISTORY_FIELD_LABELS: Record<HistoryTargetType, Record<string, string>> = {
  WORD: WORD_FIELD_LABELS,
  DOMAIN: DOMAIN_FIELD_LABELS,
  TERM: TERM_FIELD_LABELS,
  TABLE: TABLE_FIELD_LABELS,
  COLUMN: COLUMN_FIELD_LABELS,
  CODE_GROUP: CODE_GROUP_FIELD_LABELS,
  CODE_VALUE: CODE_VALUE_FIELD_LABELS,
}

/**
 * 이력의 before/after 스냅샷을 일반 객체로 꺼낸다.
 * 생성 타입은 JsonNode 클래스 모양이지만 실제 응답은 대상 스냅샷 JSON 객체다. 등록의 before, 삭제의 after는 null이다.
 */
export function snapshotOf(value: unknown): Record<string, unknown> | undefined {
  return value && typeof value === 'object' ? (value as Record<string, unknown>) : undefined
}

/**
 * 목록에 보여줄 대상 이름. 삭제된 대상도 이름이 보이도록 조회하지 않고 스냅샷에서 꺼낸다.
 * 단어·도메인·용어는 이름, 테이블은 물리명, 컬럼은 `테이블.컬럼` 물리명, 코드 그룹은 코드ID, 코드값은 `코드ID.코드값`이다.
 */
export function summarizeHistoryTarget(history: ChangeHistoryResponse): string {
  const snapshot = snapshotOf(history.after) ?? snapshotOf(history.before)
  if (!snapshot) {
    return '-'
  }
  const text = (key: string) => (typeof snapshot[key] === 'string' ? (snapshot[key] as string) : undefined)
  switch (history.targetType) {
    case 'TABLE':
      return text('physicalName') ?? '-'
    case 'COLUMN':
      return [text('tablePhysicalName'), text('physicalName')].filter(Boolean).join('.') || '-'
    case 'CODE_GROUP':
      return text('codeId') ?? '-'
    case 'CODE_VALUE':
      return [text('codeId'), text('codeValue')].filter(Boolean).join('.') || '-'
    default:
      return text('name') ?? '-'
  }
}
