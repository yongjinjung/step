package com.step.dictionary.user.dto;

import com.step.dictionary.user.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 관리자의 비밀번호 초기화 요청 본문. 현재 비밀번호 확인 없이 새 비밀번호로 바꾼다.
 * 사용자가 직접 바꿀 때는 {@code PasswordChangeRequest}(현재 비밀번호 확인)를 쓴다.
 *
 * @param password 새 비밀번호. 필수, 8~64자, 공백을 제외한 ASCII 문자
 */
public record PasswordResetRequest(
        @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH)
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.PATTERN_MESSAGE)
        String password
) {
}
