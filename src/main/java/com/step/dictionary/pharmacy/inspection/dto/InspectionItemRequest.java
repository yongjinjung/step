package com.step.dictionary.pharmacy.inspection.dto;

import com.step.dictionary.pharmacy.inspection.InspectionResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 검수 품목 하나.
 *
 * @param dispensingItemId 검수할 조제 품목 ID. 그 조제의 품목이어야 한다
 * @param result           품목 검수 결과
 * @param defectReason     불량 사유. 불합격이면 필수, 최대 500자 (합격이면 무시)
 */
public record InspectionItemRequest(
        @NotNull Long dispensingItemId,
        @NotNull InspectionResult result,
        @Size(max = 500) String defectReason
) {
}
