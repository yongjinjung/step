package com.step.dictionary.auth.dto;

import com.step.dictionary.user.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 로그인 요청 본문.
 *
 * <p>비밀번호에도 {@link PasswordPolicy}의 길이/문자 제한을 건다. 규칙을 벗어난 값은 어차피 등록될 수 없고,
 * 72바이트를 넘는 값이 BCrypt 비교에 들어가면 예외(500)가 나기 때문이다.
 *
 * @param username 아이디. 대소문자 구분 없음 (소문자로 정규화해 조회)
 * @param password 비밀번호
 */
public record LoginRequest(
        @NotBlank @Size(max = 50)
        String username,

        @NotBlank @Size(max = PasswordPolicy.MAX_LENGTH)
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.PATTERN_MESSAGE)
        String password
) {
}
