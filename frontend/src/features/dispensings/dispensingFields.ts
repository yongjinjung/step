import type { components } from '@/api/schema'

/** 조제 목록 한 줄 (GET /api/dispensings 의 content 항목) */
export type DispensingSummaryResponse = components['schemas']['DispensingSummaryResponse']

/** 조제 상세 (품목 포함, GET /api/dispensings/{id}) */
export type DispensingResponse = components['schemas']['DispensingResponse']

/** 조제 품목 한 줄 (처방량 prescribedQuantity와 조제량 quantity를 함께 준다) */
export type DispensingItemResponse = components['schemas']['DispensingItemResponse']

/** 조제 등록 요청 (POST /api/dispensings) */
export type DispensingRequest = components['schemas']['DispensingRequest']
