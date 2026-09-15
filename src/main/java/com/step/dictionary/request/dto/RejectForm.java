package com.step.dictionary.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 반려 요청 본문. 신청자가 무엇을 고쳐 다시 신청할지 알 수 있도록 의견을 필수로 받는다.
 *
 * @param comment 반려 의견. 필수, 최대 1000자
 */
public record RejectForm(
        @NotBlank @Size(max = 1000)
        String comment
) {
}
