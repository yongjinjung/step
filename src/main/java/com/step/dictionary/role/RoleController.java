package com.step.dictionary.role;

import com.step.dictionary.role.dto.RoleMenuPermission;
import com.step.dictionary.role.dto.RoleMenuPermissionRequest;
import com.step.dictionary.role.dto.RoleRequest;
import com.step.dictionary.role.dto.RoleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 역할 REST API ({@code /api/roles}).
 *
 * <p>권한: 역할 관리 메뉴({@code /roles})의 권한으로 검사한다 ({@code ApiPermissionRules}).
 * 즉 이 API를 쓰려면 그 메뉴에 대한 조회·등록·수정·삭제 권한이 있어야 한다.
 */
@Tag(name = "역할", description = "역할과 역할별 메뉴 권한 관리")
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /** 전체 역할 목록 (코드 오름차순, 페이징 없음). */
    @Operation(summary = "역할 목록", description = "역할 수가 많지 않아 페이징하지 않는다.")
    @GetMapping
    public List<RoleResponse> findAll() {
        return roleService.findAll();
    }

    /** 역할 단건 조회. */
    @Operation(summary = "역할 단건 조회")
    @GetMapping("/{id}")
    public RoleResponse findById(@PathVariable Long id) {
        return roleService.findById(id);
    }

    /** 역할 등록. 201 Created + Location 헤더. */
    @Operation(summary = "역할 등록", description = "코드는 대문자로 저장한다. 전권 역할은 메뉴 권한과 무관하게 모두 허용된다.")
    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        RoleResponse response = roleService.create(request);
        return ResponseEntity.created(URI.create("/api/roles/" + response.id())).body(response);
    }

    /** 역할 수정. 기본 제공 역할은 코드와 전권 여부를 바꿀 수 없다. */
    @Operation(summary = "역할 수정")
    @PutMapping("/{id}")
    public RoleResponse update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return roleService.update(id, request);
    }

    /** 역할 삭제. 성공 시 204. 기본 제공 역할이거나 사용자에게 부여돼 있으면 거부한다. */
    @Operation(summary = "역할 삭제", description = "기본 제공 역할은 삭제할 수 없고, 사용자에게 부여된 역할도 먼저 빼야 한다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** 역할의 메뉴 권한 목록. 권한이 하나도 없는 메뉴는 목록에 없다. */
    @Operation(summary = "역할 메뉴 권한 조회", description = "메뉴별 조회/등록/수정/삭제 권한. 권한이 없는 메뉴는 나오지 않는다.")
    @GetMapping("/{id}/menus")
    public List<RoleMenuPermission> findPermissions(@PathVariable Long id) {
        return roleService.findPermissions(id);
    }

    /** 역할의 메뉴 권한을 통째로 교체한다. */
    @Operation(summary = "역할 메뉴 권한 저장", description = "보낸 목록으로 권한을 통째로 바꾼다. 권한이 모두 false인 항목은 저장하지 않는다.")
    @PutMapping("/{id}/menus")
    public List<RoleMenuPermission> replacePermissions(
            @PathVariable Long id, @Valid @RequestBody RoleMenuPermissionRequest request) {
        return roleService.replacePermissions(id, request);
    }
}
