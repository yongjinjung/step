package com.step.dictionary.user.dto;

import com.step.dictionary.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 계정 수정 요청 본문 (관리자 전용). 아이디와 비밀번호는 여기서 바꾸지 않는다.
 * 비밀번호 초기화는 {@link PasswordResetRequest}로 따로 한다.
 *
 * @param name    표시 이름. 필수, 최대 100자
 * @param role    역할. 필수
 * @param enabled 활성 여부. 필수 (false면 로그인 불가)
 */
public record UserUpdateRequest(
        @NotBlank @Size(max = 100)
        String name,

        @NotNull
        Role role,

        @NotNull
        Boolean enabled
) {
}
