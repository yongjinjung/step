import type { components } from '@/api/schema'

export type CodeGroupRequest = components['schemas']['CodeGroupRequest']
export type CodeGroupResponse = components['schemas']['CodeGroupResponse']
export type CodeGroupColumnResponse = components['schemas']['CodeGroupColumnResponse']
export type CodeValueRequest = components['schemas']['CodeValueRequest']
export type CodeValueResponse = components['schemas']['CodeValueResponse']

/** 코드 그룹 필드의 화면 표시 이름. 변경 이력(CodeGroupSnapshot)도 이 값으로 보여준다. */
export const CODE_GROUP_FIELD_LABELS: Record<string, string> = {
  codeId: '코드ID',
  codeName: '코드명',
  used: '사용 여부',
  description: '설명',
}

/** 코드값 필드의 화면 표시 이름. 변경 이력(CodeValueSnapshot)도 이 값으로 보여준다. */
export const CODE_VALUE_FIELD_LABELS: Record<string, string> = {
  groupId: '코드 그룹 ID',
  codeId: '코드ID',
  codeValue: '코드값',
  valueName: '코드값명',
  sortOrder: '정렬 순서',
  used: '사용 여부',
  description: '설명',
}

/** 업무 오류 코드 → 코드 그룹 폼 필드. 없는 코드는 알림으로 보여준다. */
export const CODE_GROUP_ERROR_FIELDS: Record<string, keyof CodeGroupRequest> = {
  DUPLICATE_CODE_ID: 'codeId',
}

/** 업무 오류 코드 → 코드값 폼 필드. */
export const CODE_VALUE_ERROR_FIELDS: Record<string, keyof CodeValueRequest> = {
  DUPLICATE_CODE_VALUE: 'codeValue',
}

/** 코드ID 형식 (백엔드 CodeGroupRequest): 영문자로 시작하는 영문/숫자/_. 저장 시 대문자로 바뀐다 */
export const CODE_ID_PATTERN = /^[A-Za-z][A-Za-z0-9_]*$/
/** 코드값 형식 (백엔드 CodeValueRequest): 영문/숫자/_/-. 저장 시 대문자로 바뀐다 */
export const CODE_VALUE_PATTERN = /^[A-Za-z0-9_-]+$/
