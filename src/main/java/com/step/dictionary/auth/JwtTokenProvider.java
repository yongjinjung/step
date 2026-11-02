package com.step.dictionary.auth;

import com.step.dictionary.auth.dto.TokenResponse;
import com.step.dictionary.role.Role;
import com.step.dictionary.user.AppUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * 액세스 토큰(JWT) 발급기.
 *
 * <p>토큰 클레임:
 * <ul>
 *   <li>{@code iss}: 발급자 ({@code app.jwt.issuer})</li>
 *   <li>{@code sub}: 사용자 아이디. 아이디는 바뀌지 않으므로 식별자로 쓴다</li>
 *   <li>{@code iat}, {@code exp}: 발급/만료 시각</li>
 *   <li>{@code roles}: 역할 이름 목록 (예: {@code ["ADMIN"]})</li>
 * </ul>
 * 검증은 {@code SecurityConfig}의 {@code JwtDecoder}가 요청마다 한다 (서명, 만료, 발급자).
 */
@Component
public class JwtTokenProvider {

    /** 역할 클레임 이름. {@code SecurityConfig}가 이 클레임을 권한({@code ROLE_*})으로 바꾼다. */
    public static final String ROLES_CLAIM = "roles";
    /** 응답의 토큰 타입. 클라이언트는 {@code Authorization: Bearer <토큰>}으로 보낸다. */
    private static final String TOKEN_TYPE = "Bearer";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    /** 현재 시각. 테스트에서 만료된 토큰을 만들 수 있도록 주입받는다. */
    private final Clock clock;

    public JwtTokenProvider(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    /** 사용자에게 액세스 토큰을 발급한다. 로그인 성공 후에만 호출한다. */
    public TokenResponse issue(AppUser user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(properties.expiration()))
                // 역할 코드 목록. 기능 권한은 요청마다 DB에서 다시 계산하므로 이 값은 표시·참고용이다.
                .claim(ROLES_CLAIM, user.getRoles().stream().map(Role::getCode).sorted().toList())
                .build();
        // 헤더에 알고리즘을 명시해야 Nimbus가 HMAC 키로 서명한다 (기본값은 RS256).
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, TOKEN_TYPE, properties.expiration().toSeconds());
    }
}
