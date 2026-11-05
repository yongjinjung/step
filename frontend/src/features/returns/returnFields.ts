import type { components } from '@/api/schema'
import type { ReturnDisposition } from '@/features/pharmacy/pharmacyFields'

/** 반납 목록 한 줄 (GET /api/returns 의 content 항목) */
export type DrugReturnSummaryResponse = components['schemas']['DrugReturnSummaryResponse']

/** 반납 상세 (품목 포함, GET /api/returns/{id}) */
export type DrugReturnResponse = components['schemas']['DrugReturnResponse']

/** 반납 품목 한 줄 (issuedQuantity: 불출량, quantity: 이번 반납량) */
export type DrugReturnItemResponse = components['schemas']['DrugReturnItemResponse']

/** 반납 등록 요청 (POST /api/returns). 품목은 반납할 것만 보낸다 */
export type DrugReturnRequest = components['schemas']['DrugReturnRequest']

/**
 * 반납 등록 폼의 값. 요청(DrugReturnRequest)과 달리 **불출 품목 전부**를 줄로 두고,
 * 반납량을 비운(또는 0인) 줄은 보낼 때 뺀다. 그래서 quantity가 비어 있을 수 있다.
 */
export interface ReturnFormValues {
    issueId: number
    reason: string
    remark?: string
    items: {
        issueItemId: number
        /** 이번 반납량. 비우면 이 품목은 반납하지 않는다 (InputNumber는 비우면 null) */
        quantity?: number | null
        disposition: ReturnDisposition
    }[]
}
