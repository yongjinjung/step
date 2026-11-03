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