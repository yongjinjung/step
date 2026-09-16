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

interface ApplyFieldErrorsOptions {
  /**
   * 필드 경로 앞에서 뗄 접두어. 변경 신청 API는 신청 내용을 `payload`로 감싸 오류 경로가 `payload.name`이 되는데,
   * 폼 필드는 `name`이므로 `'payload.'`를 넘긴다.
   */
  stripPrefix?: string
}

/**
 * fieldErrors를 Ant Design Form의 필드 오류로 표시한다.
 * 필드 경로 `name`, `wordIds[0]`은 폼의 이름 경로 `['name']`, `['wordIds', 0]`로 바꾼다.
 *
 * @returns 표시한 필드 오류가 있으면 true. false면 호출한 쪽이 전체 메시지를 보여준다
 */
export function applyFieldErrors(form: FormInstance, error: unknown, options: ApplyFieldErrorsOptions = {}): boolean {
  if (!isApiError(error) || !error.fieldErrors?.length) {
    return false
  }
  const { stripPrefix } = options
  form.setFields(
    error.fieldErrors.map(({ field, reason }) => {
      const path = stripPrefix && field.startsWith(stripPrefix) ? field.slice(stripPrefix.length) : field
      return {
        name: path
          .split(/[.[\]]/)
          .filter(Boolean)
          .map((part) => (/^\d+$/.test(part) ? Number(part) : part)),
        errors: [reason],
      }
    }),
  )
  return true
}

/**
 * 업무 오류 코드를 특정 폼 필드의 오류로 표시한다 (예: DUPLICATE_WORD_NAME → name 필드).
 *
 * @param codeFields 오류 코드 → 필드 이름
 * @returns 연결된 필드가 있어 표시했으면 true
 */
export function applyCodeError(form: FormInstance, error: unknown, codeFields: Record<string, string>): boolean {
  if (!isApiError(error)) {
    return false
  }
  const field = codeFields[error.code]
  if (!field) {
    return false
  }
  form.setFields([{ name: field, errors: [error.message] }])
  return true
}
