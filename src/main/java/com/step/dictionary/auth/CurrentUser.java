package com.step.dictionary.auth;

import com.step.dictionary.user.Role;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 요청을 보낸 로그인 사용자. 서비스가 Spring Security 타입({@link Jwt})에 의존하지 않도록 필요한 값만 옮겨 담는다.
 *
 * @param username 아이디 (토큰 subject, 소문자)
 * @param admin    ADMIN 역할 여부 (토큰 roles 클레임 기준)
 */
public record CurrentUser(String username, boolean admin) {

    /** 검증된 토큰에서 만든다. 컨트롤러에서 {@code @AuthenticationPrincipal Jwt}와 함께 쓴다. */
    public static CurrentUser from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(JwtTokenProvider.ROLES_CLAIM);
        return new CurrentUser(jwt.getSubject(), roles != null && roles.contains(Role.ADMIN.name()));
    }
}
