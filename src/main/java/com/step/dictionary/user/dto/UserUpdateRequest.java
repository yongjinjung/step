package com.step.dictionary.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 계정 수정 요청 본문 (관리자 전용). 아이디와 비밀번호는 여기서 바꾸지 않는다.
 * 비밀번호 초기화는 {@link PasswordResetRequest}로 따로 한다.
 *
 * @param name    표시 이름. 필수, 최대 100자
 * @param roleIds 부여할 역할 ID. 하나 이상. 보낸 목록으로 통째로 교체한다
 * @param enabled 활성 여부. 필수 (false면 로그인 불가)
 */
public record UserUpdateRequest(
        @NotBlank @Size(max = 100)
        String name,

        @NotEmpty(message = "역할을 하나 이상 지정해야 합니다.")
        List<Long> roleIds,

        @NotNull
        Boolean enabled
) {
}
