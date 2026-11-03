package com.step.dictionary.pharmacy.dispensing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 조제 등록 요청. 조제자와 조제 일시는 서버가 채운다(로그인 사용자, 현재 시각).
 *
 * @param prescriptionId 조제할 처방 ID
 * @param remark         비고. 선택, 최대 1000자
 * @param items          조제 품목. 처방 품목마다 정확히 하나씩
 */
public record DispensingRequest(
        @NotNull Long prescriptionId,
        @Size(max = 1000) String remark,
        @NotEmpty @Size(max = 100) List<@Valid @NotNull DispensingItemRequest> items
) {
}
