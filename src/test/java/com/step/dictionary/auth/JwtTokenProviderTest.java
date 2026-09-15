package com.step.dictionary.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.step.dictionary.auth.dto.TokenResponse;
import com.step.dictionary.global.config.SecurityConfig;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.Role;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/** 실제 설정({@link SecurityConfig})의 인코더/디코더로 발급과 검증을 함께 확인한다. */
class JwtTokenProviderTest {

    private static final JwtProperties PROPERTIES =
            new JwtProperties("unit-test-jwt-secret-0123456789abcdef", Duration.ofMinutes(30), "dictionary");

    private final SecurityConfig securityConfig = new SecurityConfig();
    private final JwtDecoder decoder = securityConfig.jwtDecoder(PROPERTIES);
    private final AppUser user = new AppUser("hong", "hash", "홍길동", Role.USER);

    @Test
    @DisplayName("토큰에 아이디, 역할, 발급자, 만료 시각을 담고 같은 키로 검증할 수 있다")
    void issueAndDecode() {
        TokenResponse response = provider(PROPERTIES, Clock.systemUTC()).issue(user);

        Jwt jwt = decoder.decode(response.accessToken());
        assertThat(jwt.getSubject()).isEqualTo("hong");
        assertThat(jwt.getClaimAsStringList(JwtTokenProvider.ROLES_CLAIM)).containsExactly("USER");
        // getIssuer()는 iss를 URL로 변환하려 하므로 문자열 발급자는 클레임으로 직접 읽는다.
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("dictionary");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1800);
    }

    @Test
    @DisplayName("만료된 토큰은 검증에 실패한다")
    void rejectExpired() {
        Clock twoHoursAgo = Clock.offset(Clock.systemUTC(), Duration.ofHours(-2));
        String token = provider(PROPERTIES, twoHoursAgo).issue(user).accessToken();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("다른 비밀키로 서명한 토큰은 검증에 실패한다")
    void rejectOtherSecret() {
        JwtProperties other = new JwtProperties("other-jwt-secret-0123456789abcdefghij", Duration.ofMinutes(30), "dictionary");
        String token = provider(other, Clock.systemUTC()).issue(user).accessToken();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("발급자가 다른 토큰은 검증에 실패한다")
    void rejectOtherIssuer() {
        JwtProperties otherIssuer = new JwtProperties(PROPERTIES.secret(), Duration.ofMinutes(30), "someone-else");
        String token = provider(otherIssuer, Clock.systemUTC()).issue(user).accessToken();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private JwtTokenProvider provider(JwtProperties properties, Clock clock) {
        return new JwtTokenProvider(securityConfig.jwtEncoder(properties), properties, clock);
    }
}
