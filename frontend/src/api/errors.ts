import type { FormInstance } from 'antd'

/**
 * 백엔드 오류 응답 처리.
 *
 * 백엔드는 모든 오류를 `{ code, message, fieldErrors }` 형식으로 준다(ErrorResponse).
 * 오류 응답은 API 문서에 선언돼 있지 않아 생성 타입에 없으므로 여기서 직접 정의한다.
 */

export interface FieldErrorDetail {
  /** 필드 경로. 요청 본문 필드명(`abbreviation`, `payload.name`) 또는 엑셀 위치(`3행 영문약어`) */
  field: string
  reason: string
}

export interface ApiError {
  /** ErrorCode 이름. 화면 분기는 메시지 대신 이 값으로 한다 (예: INVALID_CREDENTIALS) */
  code: string
  message: string
  fieldErrors: FieldErrorDetail[]
}

export function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ApiError).code === 'string' &&
    typeof (value as ApiError).message === 'string'
  )
}

/** 사용자에게 보여줄 메시지. 백엔드 오류면 그 메시지, 네트워크 오류 등이면 기본 메시지. */
export function errorMessage(error: unknown, fallback = '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'): string {
  return isApiError(error) ? error.message : fallback
}

/**
 * fieldErrors를 Ant Design Form의 필드 오류로 표시한다.
 * 필드 경로 `payload.name`, `wordIds[0]`은 폼의 이름 경로 `['payload', 'name']`, `['wordIds', 0]`로 바꾼다.
 *
 * @returns 표시한 필드 오류가 있으면 true. false면 호출한 쪽이 전체 메시지를 보여준다
 */
export function applyFieldErrors(form: FormInstance, error: unknown): boolean {
  if (!isApiError(error) || !error.fieldErrors?.length) {
    return false
  }
  form.setFields(
    error.fieldErrors.map(({ field, reason }) => ({
      name: field
        .split(/[.[\]]/)
        .filter(Boolean)
        .map((part) => (/^\d+$/.test(part) ? Number(part) : part)),
      errors: [reason],
    })),
  )
  return true
}
