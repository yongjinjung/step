package com.step.dictionary.role.dto;

import com.step.dictionary.role.Role;
import java.time.LocalDateTime;

/**
 * 역할 응답.
 *
 * @param id          역할 ID
 * @param code        역할 코드 (대문자)
 * @param name        역할명
 * @param description 설명 (null 가능)
 * @param superAdmin  전권 여부
 * @param used        사용 여부
 * @param builtIn     기본 제공 역할 여부 (삭제·코드 변경 불가)
 * @param createdAt   생성 일시
 * @param updatedAt   최종 수정 일시
 */
public record RoleResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean superAdmin,
        boolean used,
        boolean builtIn,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 엔티티를 응답으로 변환한다. */
    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.isSuperAdmin(),
                role.isUsed(),
                role.isBuiltIn(),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }
}
