import type { components } from '@/api/schema'

export type UserResponse = components['schemas']['UserResponse']
export type UserCreateRequest = components['schemas']['UserCreateRequest']
export type UserUpdateRequest = components['schemas']['UserUpdateRequest']
/** 사용자에게 부여된 역할 (표시용 최소 정보) */
export type UserRole = NonNullable<UserResponse['roles']>[number]

/** 사용자 필드의 화면 표시 이름 */
export const USER_FIELD_LABELS: Record<string, string> = {
  username: '아이디',
  name: '이름',
  roleIds: '역할',
  enabled: '활성 여부',
  password: '비밀번호',
}

/** 백엔드 UserCreateRequest와 같은 규칙: 영문/숫자로 시작하는 영문/숫자/./_/- (저장 시 소문자) */
export const USERNAME_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._-]*$/

/** 백엔드 PasswordPolicy와 같은 규칙: 8~64자, 공백 없는 ASCII */
export const PASSWORD_RULES = [
  { required: true, message: '비밀번호를 입력하세요.' },
  { min: 8, max: 64, message: '8~64자로 입력하세요.' },
  { pattern: /^[!-~]+$/, message: '영문, 숫자, 특수문자만 사용할 수 있습니다 (공백 불가).' },
]
