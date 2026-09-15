package com.step.dictionary.user;

import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.user.dto.PasswordResetRequest;
import com.step.dictionary.user.dto.UserCreateRequest;
import com.step.dictionary.user.dto.UserResponse;
import com.step.dictionary.user.dto.UserSearchCondition;
import com.step.dictionary.user.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 계정 관리 REST API ({@code /api/users}). 전체가 ADMIN 전용이다 ({@code SecurityConfig}).
 * 삭제 API는 두지 않는다. 계정은 {@code enabled=false}로 비활성화한다.
 * 로그인한 사용자 본인의 정보/비밀번호 변경은 {@code AuthController}({@code /api/auth/me})에 있다.
 */
@Tag(name = "사용자 관리", description = "계정 생성, 역할/활성 변경, 비밀번호 초기화 (관리자 전용)")
@RestController
@RequestMapping("/api/users")
public class AppUserController {

    private final AppUserService userService;

    public AppUserController(AppUserService userService) {
        this.userService = userService;
    }

    /** 계정 생성. 201 Created + Location 헤더. */
    @Operation(summary = "계정 생성", description = "아이디는 소문자로 저장한다. 생성 직후 활성 상태다.")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + response.id())).body(response);
    }

    /** 계정 단건 조회. */
    @Operation(summary = "계정 단건 조회")
    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    /** 계정 검색 ({@code ?keyword=&role=&enabled=&page=&size=}). 기본 20건. */
    @Operation(summary = "계정 검색", description = "keyword는 아이디/이름에 부분 일치. 아이디 오름차순 정렬.")
    @GetMapping
    public PageResponse<UserResponse> search(
            @ParameterObject UserSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return userService.search(condition, pageable);
    }

    /** 이름, 역할, 활성 여부 수정. 마지막 활성 관리자를 없애는 수정은 409. */
    @Operation(summary = "계정 수정", description = "활성 관리자가 한 명도 남지 않게 되는 수정은 거부한다.")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    /** 비밀번호 초기화. 성공 시 204. */
    @Operation(summary = "비밀번호 초기화", description = "현재 비밀번호 확인 없이 새 비밀번호로 바꾼다.")
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
