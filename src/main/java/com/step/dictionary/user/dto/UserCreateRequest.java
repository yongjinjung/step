package com.step.dictionary.user.dto;

import com.step.dictionary.user.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 계정 생성 요청 본문 (관리자 전용). 검증 실패 시 400(INVALID_INPUT).
 *
 * @param username 아이디. 필수, 4~50자, 영문/숫자로 시작하고 영문/숫자/'.'/'_'/'-'. 저장 시 소문자로 바뀐다
 * @param password 초기 비밀번호. 필수, 8~64자, 공백을 제외한 ASCII 문자 ({@link PasswordPolicy})
 * @param name     표시 이름. 필수, 최대 100자
 * @param roleIds  부여할 역할 ID. 하나 이상 (역할이 없으면 아무 화면도 쓸 수 없다)
 */
public record UserCreateRequest(
        @NotBlank @Size(min = 4, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$",
                message = "영문/숫자로 시작하고 영문, 숫자, '.', '_', '-'만 사용할 수 있습니다.")
        String username,

        @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH)
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.PATTERN_MESSAGE)
        String password,

        @NotBlank @Size(max = 100)
        String name,

        @NotEmpty(message = "역할을 하나 이상 지정해야 합니다.")
        List<Long> roleIds
) {
}
