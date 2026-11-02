package com.step.dictionary.role.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 역할의 메뉴 권한 저장 요청. 역할의 권한을 통째로 교체한다.
 *
 * @param permissions 메뉴별 권한 목록. 네 권한이 모두 false인 항목은 저장하지 않는다(권한 없음).
 *                    목록에 없는 메뉴도 권한이 없는 것으로 본다
 */
public record RoleMenuPermissionRequest(
        @NotNull @Valid
        List<RoleMenuPermission> permissions
) {
}
