package com.step.dictionary.menu;

import com.step.dictionary.auth.CurrentUser;
import com.step.dictionary.menu.dto.MenuRequest;
import com.step.dictionary.menu.dto.MenuResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메뉴 REST API ({@code /api/menus}).
 *
 * <p>권한 ({@code SecurityConfig}): 화면 표시용 조회는 로그인한 사용자, 전체 조회와 등록·수정·삭제는 ADMIN이다.
 */
@Tag(name = "메뉴", description = "화면 메뉴(상위/하위) 조회와 관리")
@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /** 화면에 그릴 메뉴 트리. 로그인한 사용자의 역할에 맞는 메뉴만 나온다. */
    @Operation(summary = "메뉴 트리 조회",
            description = "화면 표시용. 사용 중이면서 조회 권한이 있는 메뉴만, 하위가 없는 상위 메뉴는 제외한다. "
                    + "사용자별 메뉴 지정이 있으면 그중에서 더 좁힌다. 메뉴마다 그 사용자의 권한(canRead 등)이 함께 나온다.")
    @GetMapping
    public List<MenuResponse> findNavigation(@AuthenticationPrincipal Jwt jwt) {
        return menuService.findNavigation(CurrentUser.from(jwt).username());
    }

    /** 관리 화면용 전체 메뉴 트리 (미사용 메뉴 포함). */
    @Operation(summary = "전체 메뉴 트리 조회", description = "메뉴 관리 화면용. 미사용 메뉴와 권한이 없는 메뉴도 모두 나온다.")
    @GetMapping("/all")
    public List<MenuResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return menuService.findAll(CurrentUser.from(jwt).username());
    }

    /** 메뉴 단건 조회. 하위 메뉴는 담기지 않는다. */
    @Operation(summary = "메뉴 단건 조회")
    @GetMapping("/{id}")
    public MenuResponse findById(@PathVariable Long id) {
        return menuService.findById(id);
    }

    /** 메뉴 등록. 201 Created + Location 헤더. 상위 메뉴로 만들려면 parentId를 비운다. */
    @Operation(summary = "메뉴 등록", description = "parentId가 없으면 상위(TOP) 메뉴, 있으면 그 아래 하위(LEFT) 메뉴가 된다.")
    @PostMapping
    public ResponseEntity<MenuResponse> create(@Valid @RequestBody MenuRequest request) {
        MenuResponse response = menuService.create(request);
        return ResponseEntity.created(URI.create("/api/menus/" + response.id())).body(response);
    }

    /** 메뉴 수정 (전체 교체). 상위 메뉴를 바꾸면 다른 묶음으로 옮겨진다. */
    @Operation(summary = "메뉴 수정")
    @PutMapping("/{id}")
    public MenuResponse update(@PathVariable Long id, @Valid @RequestBody MenuRequest request) {
        return menuService.update(id, request);
    }

    /** 메뉴 삭제. 성공 시 204, 하위 메뉴가 있으면 409. */
    @Operation(summary = "메뉴 삭제", description = "하위 메뉴가 있으면 삭제할 수 없다. 하위 메뉴를 먼저 옮기거나 지운다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
