package com.step.dictionary.pharmacy.prescription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 처방 취소 요청.
 *
 * @param reason 취소 사유. 필수, 최대 500자
 */
public record PrescriptionCancelRequest(@NotBlank @Size(max = 500) String reason) {
}
