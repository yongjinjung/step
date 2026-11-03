package com.step.dictionary.pharmacy.inspection.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 검수 등록 요청. 검수자와 검수 일시는 서버가 채운다.
 *
 * @param dispensingId 검수할 조제 ID
 * @param remark       비고. 선택, 최대 1000자
 * @param items        검수 품목. 조제 품목마다 정확히 하나씩
 */
public record InspectionRequest(
        @NotNull Long dispensingId,
        @Size(max = 1000) String remark,
        @NotEmpty @Size(max = 100) List<@Valid @NotNull InspectionItemRequest> items
) {
}
