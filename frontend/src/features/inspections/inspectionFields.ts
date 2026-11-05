import type { components } from '@/api/schema'

/** 검수 목록 한 줄 (GET /api/inspections 의 content 항목) */
export type InspectionSummaryResponse = components['schemas']['InspectionSummaryResponse']

/** 검수 상세 (품목 포함, GET /api/inspections/{id}) */
export type InspectionResponse = components['schemas']['InspectionResponse']

/** 검수 품목 한 줄 */
export type InspectionItemResponse = components['schemas']['InspectionItemResponse']

/** 검수 등록 요청 (POST /api/inspections) */
export type InspectionRequest = components['schemas']['InspectionRequest']
