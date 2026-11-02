package com.step.dictionary.auth;

import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 요청을 보낸 로그인 사용자. 서비스가 Spring Security 타입({@link Jwt})에 의존하지 않도록 필요한 값만 옮겨 담는다.
 *
 * <p>기능 권한은 여기 담긴 역할 코드가 아니라 DB의 역할별 메뉴 권한으로 판단한다
 * ({@code PermissionService}). 토큰에 담긴 역할은 발급 시점 값이라 최신이 아닐 수 있기 때문이다.
 *
 * @param username  아이디 (토큰 subject, 소문자)
 * @param roleCodes 토큰에 담긴 역할 코드 (표시·참고용)
 */
public record CurrentUser(String username, List<String> roleCodes) {

    /** 검증된 토큰에서 만든다. 컨트롤러에서 {@code @AuthenticationPrincipal Jwt}와 함께 쓴다. */
    public static CurrentUser from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(JwtTokenProvider.ROLES_CLAIM);
        return new CurrentUser(jwt.getSubject(), roles == null ? List.of() : List.copyOf(roles));
    }
}
