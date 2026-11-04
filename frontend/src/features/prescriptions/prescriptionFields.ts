import type { components } from '@/api/schema'

// ---- 1) 타입: 백엔드 API 문서에서 생성된 타입을 꺼내 이름을 붙인다 ----

/** 처방 목록 한 줄 (GET /api/prescriptions 의 content 항목) */
export type PrescriptionSummaryResponse = components['schemas']['PrescriptionSummaryResponse']

/** 처방 상태: 'ORDERED' | 'DISPENSED' | 'CANCELLED' */
export type PrescriptionStatus = NonNullable<PrescriptionSummaryResponse['status']>

/** 처방 구분: 'OUTPATIENT' | 'INPATIENT' | 'DISCHARGE' | 'EMERGENCY' */
export type PrescriptionType = NonNullable<PrescriptionSummaryResponse['prescriptionType']>

// ---- 2) 코드값 → 화면 표시 (한글 이름 + Ant Design Tag 색) ----

interface TagSpec {
    label: string
    color: string
}

/** 처방 상태 (백엔드 PrescriptionStatus). 조제 전(ORDERED)에만 취소할 수 있다 */
export const PRESCRIPTION_STATUSES: Record<PrescriptionStatus, TagSpec> = {
    ORDERED: { label: '처방', color: 'processing' },
    DISPENSED: { label: '조제됨', color: 'success' },
    CANCELLED: { label: '취소', color: 'default' },
}

/** 처방 구분 (백엔드 PrescriptionType) */
export const PRESCRIPTION_TYPES: Record<PrescriptionType, TagSpec> = {
    OUTPATIENT: { label: '외래', color: 'blue' },
    INPATIENT: { label: '입원', color: 'purple' },
    DISCHARGE: { label: '퇴원', color: 'cyan' },
    EMERGENCY: { label: '응급', color: 'red' },
}

// ------ 3) 상세 (GET /api/prescriptions/{id}) -----
/** 처방 상세 (품목 포함) */
export type PrescriptionResponse = components['schemas']['PrescriptionResponse']

/** 처방 목록 한 출 */
export type PrescriptionItemResponse = components['schemas']['PrescriptionItemResponse']

/** 수량 표시 서버는 소수 3자리(30.000)로 주지만 화면에는 필요한 자리만 쉼표와 함께 보여준다.*/
export function formatQuantity(value: number | undefined): string{
    return value === undefined ? '-' : value.toLocaleString('ko-KR',{
        maximumFractionDigits: 3
    })
}

// ------ 4) 등록 (POST /api/prescriptions) -----

/** 처방 등록 요청 (품목 포함) */
export type PrescriptionRequest = components['schemas']['PrescriptionRequest']

/** 처방 품목 한 줄 요청 */
export type PrescriptionItemRequest = components['schemas']['PrescriptionItemRequest']

/** 처방 구분 선택 박스 항목 (외래·입원·퇴원·응급 ) */
export const PRESCRIPTION_TYPE_OPTIONS = Object.entries(PRESCRIPTION_TYPES).map(([value, {label}])=> ({value, label}))

/** 업무 오류코드 → 그 오류를 보여줄 폼 필드 */
export const PRESCRIPTION_ERROR_FIELDS: Record<string, string> = {DUPLICATE_PRESCRIPTION_NO : 'prescriptionNo'}

/** 지금 시각을 <input type="datetime-local"> 값 형식 (yyyy-MM-ddTHH:mm, 내 PC 시간대)으로*/
export function nowForDateTimeInput(): string {
    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${now.getFullYear()}-${now.getMonth()+1}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
}




