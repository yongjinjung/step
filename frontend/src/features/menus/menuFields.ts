import type { components } from '@/api/schema'

export type MenuRequest = components['schemas']['MenuRequest']
export type MenuResponse = components['schemas']['MenuResponse']

/** 메뉴 필드의 화면 표시 이름 */
export const MENU_FIELD_LABELS: Record<string, string> = {
  parentId: '상위 메뉴',
  name: '메뉴명',
  path: '화면 경로',
  icon: '아이콘',
  sortOrder: '정렬 순서',
  used: '사용 여부',
  adminOnly: '관리자 전용',
}

/** 업무 오류 코드 → 메뉴 폼 필드. 없는 코드는 알림으로 보여준다. */
export const MENU_ERROR_FIELDS: Record<string, keyof MenuRequest> = {
  DUPLICATE_MENU_PATH: 'path',
  INVALID_MENU_PATH: 'path',
  INVALID_MENU_PARENT: 'parentId',
  MENU_PARENT_NOT_FOUND: 'parentId',
}

/** 백엔드 MenuRequest 검증과 같은 규칙 (영문/숫자/_/-와 /). 저장할 때 앞의 /가 붙는다 */
export const MENU_PATH_PATTERN = /^\/?[A-Za-z0-9/_-]*$/
