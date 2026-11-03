package com.step.dictionary.pharmacy.prescription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 처방 품목 하나. 순번은 목록 순서대로 서버가 매긴다.
 * 수량은 {@code DECIMAL(12,3)}에 맞춰 정수 9자리, 소수 3자리까지 받는다.
 *
 * @param drugCode      약품코드. 필수, 최대 30자 (저장 시 대문자)
 * @param drugName      약품명. 필수, 최대 200자
 * @param dose          1회 투여량. 0보다 크다
 * @param doseUnit      투여 단위. 예: MG (저장 시 대문자)
 * @param route         투여 경로. 예: PO (저장 시 대문자)
 * @param frequency     투여 빈도. 예: TID (저장 시 대문자)
 * @param days          투여 일수. 1~365
 * @param totalQuantity 총 처방량. 0보다 크다
 * @param usageNote     용법 설명. 선택, 최대 500자
 */
public record PrescriptionItemRequest(
        @NotBlank @Size(max = 30) String drugCode,
        @NotBlank @Size(max = 200) String drugName,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3) BigDecimal dose,
        @NotBlank @Size(max = 20) String doseUnit,
        @NotBlank @Size(max = 20) String route,
        @NotBlank @Size(max = 20) String frequency,
        @NotNull @Min(1) @Max(365) Integer days,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3)
        BigDecimal totalQuantity,
        @Size(max = 500) String usageNote
) {
}
