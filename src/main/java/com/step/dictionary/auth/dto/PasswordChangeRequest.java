package com.step.dictionary.auth.dto;

import com.step.dictionary.user.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 내 비밀번호 변경 요청 본문. 토큰을 탈취당해도 비밀번호까지 바뀌지 않도록 현재 비밀번호를 확인한다.
 *
 * @param currentPassword 현재 비밀번호
 * @param newPassword     새 비밀번호. 8~64자, 공백을 제외한 ASCII 문자
 */
public record PasswordChangeRequest(
        @NotBlank @Size(max = PasswordPolicy.MAX_LENGTH)
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.PATTERN_MESSAGE)
        String currentPassword,

        @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH)
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.PATTERN_MESSAGE)
        String newPassword
) {
}
