package com.step.dictionary.pharmacy.drugreturn.dto;

import com.step.dictionary.pharmacy.drugreturn.ReturnDisposition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 반납 품목 하나.
 *
 * @param issueItemId 반납할 불출 품목 ID. 그 불출의 품목이어야 한다
 * @param quantity    반납 수량. 0보다 크고, 이전 반납과 합쳐 불출 수량 이하
 * @param disposition 처리 방법 (재입고/폐기)
 */
public record DrugReturnItemRequest(
        @NotNull Long issueItemId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        @NotNull ReturnDisposition disposition
) {
}
