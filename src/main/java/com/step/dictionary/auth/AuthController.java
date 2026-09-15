package com.step.dictionary.auth;

import com.step.dictionary.auth.dto.LoginRequest;
import com.step.dictionary.auth.dto.PasswordChangeRequest;
import com.step.dictionary.auth.dto.TokenResponse;
import com.step.dictionary.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 REST API ({@code /api/auth}).
 *
 * <ul>
 *   <li>{@code POST /login}: 공개. 토큰 발급</li>
 *   <li>{@code GET /me}, {@code PUT /me/password}: 로그인 필요. 토큰의 사용자 본인에 대해서만 동작한다</li>
 * </ul>
 * 로그아웃 API는 없다. 서버가 토큰을 저장하지 않으므로 클라이언트가 토큰을 버리면 된다.
 */
@Tag(name = "인증", description = "로그인(JWT 발급)과 내 계정")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 로그인. 성공하면 액세스 토큰을 준다.
     * {@code @SecurityRequirements}를 비워 Swagger 문서에서 이 API만 인증 없이 호출하는 것으로 표시한다.
     */
    @Operation(summary = "로그인", description = "아이디/비밀번호를 확인하고 JWT 액세스 토큰을 발급한다.")
    @SecurityRequirements
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * 내 정보 조회.
     * {@code @AuthenticationPrincipal}은 검증을 통과한 토큰({@link Jwt})을 넣어준다. subject가 아이디다.
     */
    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(jwt.getSubject());
    }

    /** 내 비밀번호 변경. 성공 시 204. */
    @Operation(summary = "내 비밀번호 변경", description = "현재 비밀번호를 확인한 뒤 새 비밀번호로 바꾼다.")
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(jwt.getSubject(), request);
        return ResponseEntity.noContent().build();
    }
}
