package com.step.dictionary.pharmacy.dispensing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 조제 품목 하나.
 *
 * @param prescriptionItemId 조제할 처방 품목 ID. 그 처방의 품목이어야 한다
 * @param quantity           조제 수량. 0보다 크다
 * @param lotNo              제조번호(로트). 선택, 최대 50자
 * @param expiryDate         유효기간 만료일. 선택 (yyyy-MM-dd)
 */
public record DispensingItemRequest(
        @NotNull Long prescriptionItemId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        @Size(max = 50) String lotNo,
        LocalDate expiryDate
) {
}
