package com.step.dictionary.user.dto;

import com.step.dictionary.role.Role;
import com.step.dictionary.user.AppUser;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 사용자 응답. 비밀번호 해시는 어떤 경우에도 포함하지 않는다.
 *
 * @param id         사용자 ID
 * @param username   아이디 (소문자)
 * @param name       표시 이름
 * @param roles      부여된 역할 (코드 오름차순)
 * @param superAdmin 전권 역할을 가졌는지 (화면에서 표시용)
 * @param enabled    활성 여부
 * @param createdAt  생성 일시
 * @param updatedAt  최종 수정 일시
 */
public record UserResponse(
        Long id,
        String username,
        String name,
        List<UserRole> roles,
        boolean superAdmin,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 사용자에게 부여된 역할 (표시용 최소 정보).
     *
     * @param id   역할 ID
     * @param code 역할 코드
     * @param name 역할명
     */
    public record UserRole(Long id, String code, String name) {
    }

    /** 엔티티를 응답으로 변환한다. 역할을 읽으므로 트랜잭션 안에서 호출한다. */
    public static UserResponse from(AppUser user) {
        List<UserRole> roles = user.getRoles().stream()
                .sorted(Comparator.comparing(Role::getCode))
                .map(role -> new UserRole(role.getId(), role.getCode(), role.getName()))
                .toList();
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                roles,
                user.hasSuperAdminRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
