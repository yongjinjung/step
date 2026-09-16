import type { components } from '@/api/schema'
import type { TermResponse } from '@/features/terms/termFields'
import { normalizeScale, type DataType } from '@/utils/dataType'

export type MetaTableRequest = components['schemas']['MetaTableRequest']
export type MetaTableResponse = components['schemas']['MetaTableResponse']
export type MetaColumnRequest = components['schemas']['MetaColumnRequest']
export type MetaColumnResponse = components['schemas']['MetaColumnResponse']
export type ColumnIssue = NonNullable<MetaColumnResponse['issues']>[number]

/** 테이블·컬럼 물리명 형식 (백엔드 PhysicalNames.PATTERN). 저장 시 대문자로 바뀐다 */
export const PHYSICAL_NAME_PATTERN = /^[A-Za-z][A-Za-z0-9_]*$/
/** 물리명 최대 길이: MySQL 식별자 한도 (백엔드 PhysicalNames.MAX_LENGTH) */
export const PHYSICAL_NAME_MAX_LENGTH = 64

/** 비표준 검출 사유의 화면 이름 (백엔드 ColumnIssue) */
export const COLUMN_ISSUE_LABELS: Record<ColumnIssue, string> = {
  NO_TERM: '용어 미매핑',
  NAME_MISMATCH: '물리명 불일치',
  TYPE_MISMATCH: '타입 불일치',
  LENGTH_MISMATCH: '길이 불일치',
}

/** 테이블 필드 표시 이름. 변경 이력(TableSnapshot)도 이 값으로 보여준다. */
export const TABLE_FIELD_LABELS: Record<string, string> = {
  physicalName: '물리명',
  logicalName: '논리명',
  description: '설명',
}

/** 컬럼 필드 표시 이름. 변경 이력(ColumnSnapshot)도 이 값으로 보여준다. */
export const COLUMN_FIELD_LABELS: Record<string, string> = {
  tableId: '테이블 ID',
  tablePhysicalName: '테이블',
  seq: '순서',
  physicalName: '물리명',
  logicalName: '논리명',
  termId: '용어 ID',
  termName: '매핑 용어',
  codeGroupId: '코드 그룹 ID',
  codeId: '공통 코드',
  dataType: '데이터 타입',
  dataLength: '길이',
  dataScale: '소수점',
  nullable: 'NULL 허용',
  primaryKey: '기본키',
}

/** 업무 오류 코드 → 테이블 폼 필드. 없는 코드는 알림으로 보여준다. */
export const TABLE_ERROR_FIELDS: Record<string, keyof MetaTableRequest> = {
  DUPLICATE_TABLE_NAME: 'physicalName',
}

/** 업무 오류 코드 → 컬럼 폼 필드. 없는 코드(TABLE_NOT_FOUND 등)는 알림으로 보여준다. */
export const COLUMN_ERROR_FIELDS: Record<string, keyof MetaColumnRequest> = {
  DUPLICATE_COLUMN_NAME: 'physicalName',
  INVALID_COLUMN_SPEC: 'dataLength',
  PRIMARY_KEY_NULLABLE: 'nullable',
  TERM_NOT_FOUND: 'termId',
  CODE_GROUP_NOT_FOUND: 'codeGroupId',
  CODE_GROUP_NOT_USED: 'codeGroupId',
}

interface ColumnSpec {
  physicalName?: string
  dataType?: DataType
  dataLength?: number | null
  dataScale?: number | null
}

/**
 * 입력 중인 컬럼 값의 비표준 사유를 계산한다 (저장 전 미리보기용).
 *
 * 백엔드 `MetaColumn.inspect()`와 같은 규칙이어야 한다. 규칙을 바꾸면 백엔드 두 곳(inspect, 검색 조건)과 함께 고친다.
 * - 용어가 없으면 NO_TERM만 돌려준다.
 * - 물리명은 대문자로 바꿔 용어 영문약어와 비교한다 (서버가 대문자로 저장한다).
 * - 길이·소수점은 없는 값끼리 같다고 보고, DECIMAL 소수점을 비우면 0으로 본다.
 */
export function inspectColumn(column: ColumnSpec, term: TermResponse | undefined): ColumnIssue[] {
  if (!term) {
    return ['NO_TERM']
  }
  const issues: ColumnIssue[] = []
  if ((column.physicalName ?? '').trim().toUpperCase() !== term.abbreviation) {
    issues.push('NAME_MISMATCH')
  }
  if (column.dataType !== term.dataType) {
    issues.push('TYPE_MISMATCH')
  }
  const sameLength = (column.dataLength ?? null) === (term.dataLength ?? null)
  const sameScale = normalizeScale(column.dataType, column.dataScale) === (term.dataScale ?? null)
  if (!sameLength || !sameScale) {
    issues.push('LENGTH_MISMATCH')
  }
  return issues
}
