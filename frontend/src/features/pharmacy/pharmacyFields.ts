import type { components } from '@/api/schema'

/**
 * 약제 업무(처방 → 조제 → 검수 → 불출 → 반납) 화면 공용 정의.
 *
 * - 코드값 → 화면 표시(한글 이름 + Tag 색) 표
 * - 주소(URL)의 검색 조건을 검사하는 함수 (pickCode / pickDate / pickId)
 * - 수량 표시·계산
 */

type Schemas = components['schemas']

// ---- 1) 코드값 → 화면 표시 ----

export interface TagSpec {
    label: string
    color: string
}

/** 조제 상태: 'DISPENSED' | 'INSPECTED' | 'REJECTED' | 'ISSUED' */
export type DispensingStatus = NonNullable<Schemas['DispensingSummaryResponse']['status']>

/** 검수 결과: 'PASS' | 'FAIL' */
export type InspectionResult = NonNullable<Schemas['InspectionSummaryResponse']['result']>

/** 반납 처리 방법: 'RESTOCK' | 'DISPOSE' */
export type ReturnDisposition = NonNullable<Schemas['DrugReturnItemResponse']['disposition']>

/** 조제 상태 (백엔드 DispensingStatus). 조제됨 → 검수 합격/불합격 → 불출됨 */
export const DISPENSING_STATUSES: Record<DispensingStatus, TagSpec> = {
    DISPENSED: { label: '검수 대기', color: 'processing' },
    INSPECTED: { label: '검수 합격', color: 'success' },
    REJECTED: { label: '검수 불합격', color: 'error' },
    ISSUED: { label: '불출됨', color: 'purple' },
}

/** 검수 결과 (백엔드 InspectionResult). 품목 하나라도 불합격이면 검수 전체가 불합격이다 */
export const INSPECTION_RESULTS: Record<InspectionResult, TagSpec> = {
    PASS: { label: '합격', color: 'success' },
    FAIL: { label: '불합격', color: 'error' },
}

/** 반납 처리 방법 (백엔드 ReturnDisposition) */
export const RETURN_DISPOSITIONS: Record<ReturnDisposition, TagSpec> = {
    RESTOCK: { label: '재입고', color: 'blue' },
    DISPOSE: { label: '폐기', color: 'red' },
}

/** 코드표에서 선택 박스 항목 `[{ value, label }]`을 만든다. 표와 선택 박스가 항상 같다. */
export function optionsOf<T extends string>(table: Record<T, TagSpec>): { value: T; label: string }[] {
    return (Object.entries(table) as [T, TagSpec][]).map(([value, { label }]) => ({ value, label }))
}

// ---- 2) 주소의 검색 조건 검사 ----

/** 날짜 입력 값 형식 (yyyy-MM-dd) */
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

/** 주소의 문자열이 표의 코드값이면 그 값을, 아니면 undefined(조건 없음)를 돌려준다. */
export function pickCode<T extends string>(value: string | undefined, table: Record<T, unknown>): T | undefined {
    return value !== undefined && Object.hasOwn(table, value) ? (value as T) : undefined
}

/** 주소의 문자열이 yyyy-MM-dd 형식이면 그 값을, 아니면 undefined를 돌려준다. */
export function pickDate(value: string | undefined): string | undefined {
    return value !== undefined && DATE_PATTERN.test(value) ? value : undefined
}

/** 주소의 문자열이 양의 정수(ID)면 숫자로, 아니면 undefined를 돌려준다. (`?prescriptionId=abc` → 조건 없음) */
export function pickId(value: string | undefined): number | undefined {
    const id = Number(value)
    return value !== undefined && Number.isSafeInteger(id) && id > 0 ? id : undefined
}

// ---- 3) 수량 ----

/** 수량 표시. 서버는 소수 3자리(30.000)로 주지만 화면에는 필요한 자리만 쉼표와 함께 보여준다. */
export function formatQuantity(value: number | undefined): string {
    return value === undefined ? '-' : value.toLocaleString('ko-KR', { maximumFractionDigits: 3 })
}

/**
 * 두 수량의 차 (소수 3자리로 반올림).
 * 0.3 - 0.1 = 0.19999999999999998 같은 부동소수점 오차를 없앤다. (서버 수량은 소수 3자리다)
 */
export function subtractQuantity(a: number | undefined, b: number | undefined): number {
    return Math.round(((a ?? 0) - (b ?? 0)) * 1000) / 1000
}

/** 선택 입력 문자열 정리: 앞뒤 공백을 떼고, 비면 undefined (JSON 본문에서 빠진다) */
export function trimToUndefined(value: string | undefined | null): string | undefined {
    return value?.trim() || undefined
}
