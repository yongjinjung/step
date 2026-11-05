import type { components } from '@/api/schema'

/** 불출 목록 한 줄 (GET /api/issues 의 content 항목) */
export type IssueSummaryResponse = components['schemas']['IssueSummaryResponse']

/** 불출 상세 (품목 포함, GET /api/issues/{id}) */
export type IssueResponse = components['schemas']['IssueResponse']

/** 불출 품목 한 줄. returnedQuantity는 지금까지 반납한 합계 */
export type IssueItemResponse = components['schemas']['IssueItemResponse']

/** 불출 등록 요청 (POST /api/issues). 품목은 보내지 않는다(조제 품목을 그대로 불출) */
export type IssueRequest = components['schemas']['IssueRequest']
