package com.step.dictionary.request.dto;

import jakarta.validation.constraints.Size;

/**
 * 승인 요청 본문. 본문 자체를 생략할 수 있다.
 *
 * @param comment 승인 의견. 선택, 최대 1000자
 */
public record ApproveForm(
        @Size(max = 1000)
        String comment
) {
}
