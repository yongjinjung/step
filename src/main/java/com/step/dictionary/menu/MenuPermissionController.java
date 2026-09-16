package com.step.dictionary.menu;

import com.step.dictionary.menu.dto.UserMenuPermissionRequest;
import com.step.dictionary.menu.dto.UserMenuPermissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자별 메뉴 권한 REST API ({@code /api/users/{userId}/menus}).
 *
 * <p>권한: 사용자 관리 경로라 {@code SecurityConfig}의 {@code /api/users/**} 규칙에 따라 ADMIN만 쓸 수 있다.
 */
@Tag(name = "사용자 메뉴 권한", description = "사용자에게 보여줄 메뉴 지정")
@RestController
@RequestMapping("/api/users/{userId}/menus")
public class MenuPermissionController {

    private final MenuPermissionService permissionService;

    public MenuPermissionController(MenuPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /** 사용자의 메뉴 권한 조회. 지정된 메뉴가 없으면 {@code useDefault}가 true다. */
    @Operation(summary = "사용자 메뉴 권한 조회",
            description = "지정된 메뉴 ID 목록. 비어 있으면(useDefault=true) 역할 기준 기본 메뉴를 본다.")
    @GetMapping
    public UserMenuPermissionResponse find(@PathVariable Long userId) {
        return permissionService.findByUser(userId);
    }

    /** 사용자의 메뉴 권한을 통째로 교체한다. */
    @Operation(summary = "사용자 메뉴 권한 저장",
            description = "useDefault가 true면 지정을 모두 지우고 기본 규칙으로 되돌린다. false면 menuIds에 준 메뉴만 보인다.")
    @PutMapping
    public UserMenuPermissionResponse replace(
            @PathVariable Long userId, @Valid @RequestBody UserMenuPermissionRequest request) {
        return permissionService.replace(userId, request);
    }
}
