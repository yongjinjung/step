package com.step.dictionary.user.dto;

import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.Role;
import java.time.LocalDateTime;

/**
 * 사용자 응답. 비밀번호 해시는 어떤 경우에도 포함하지 않는다.
 *
 * @param id        사용자 ID
 * @param username  아이디 (소문자)
 * @param name      표시 이름
 * @param role      역할
 * @param enabled   활성 여부
 * @param createdAt 생성 일시
 * @param updatedAt 최종 수정 일시
 */
public record UserResponse(
        Long id,
        String username,
        String name,
        Role role,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 엔티티를 응답으로 변환한다. */
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
