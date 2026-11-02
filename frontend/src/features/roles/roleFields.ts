import type { components } from '@/api/schema'

export type RoleRequest = components['schemas']['RoleRequest']
export type RoleResponse = components['schemas']['RoleResponse']
export type RoleMenuPermission = components['schemas']['RoleMenuPermission']

/** 역할 필드의 화면 표시 이름 */
export const ROLE_FIELD_LABELS: Record<string, string> = {
  code: '역할 코드',
  name: '역할명',
  description: '설명',
  superAdmin: '전권',
  used: '사용 여부',
}

/** 업무 오류 코드 → 역할 폼 필드. 없는 코드는 알림으로 보여준다. */
export const ROLE_ERROR_FIELDS: Record<string, keyof RoleRequest> = {
  DUPLICATE_ROLE_CODE: 'code',
  BUILT_IN_ROLE: 'code',
}

/** 백엔드 RoleRequest 검증과 같은 규칙 (영문자로 시작하는 영문/숫자/_). 저장 시 대문자로 바뀐다 */
export const ROLE_CODE_PATTERN = /^[A-Za-z][A-Za-z0-9_]*$/

/** 권한 매트릭스의 열 (백엔드 PermissionAction) */
export const PERMISSION_COLUMNS = [
  { key: 'canRead', label: '조회' },
  { key: 'canCreate', label: '등록' },
  { key: 'canUpdate', label: '수정' },
  { key: 'canDelete', label: '삭제' },
] as const

export type PermissionKey = (typeof PERMISSION_COLUMNS)[number]['key']
