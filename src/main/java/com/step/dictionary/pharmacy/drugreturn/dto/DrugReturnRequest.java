package com.step.dictionary.pharmacy.drugreturn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 반납 등록 요청. 반납자와 반납 일시는 서버가 채운다.
 *
 * @param issueId 반납할 불출 ID
 * @param reason  반납 사유. 필수, 최대 500자
 * @param remark  비고. 선택, 최대 1000자
 * @param items   반납 품목. 불출 품목 중 일부만 보내도 되지만 같은 품목을 두 번 보낼 수 없다
 */
public record DrugReturnRequest(
        @NotNull Long issueId,
        @NotBlank @Size(max = 500) String reason,
        @Size(max = 1000) String remark,
        @NotEmpty @Size(max = 100) List<@Valid @NotNull DrugReturnItemRequest> items
) {
}
