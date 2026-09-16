package com.step.dictionary.global.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.step.dictionary.auth.JwtProperties;
import com.step.dictionary.auth.JwtTokenProvider;
import com.step.dictionary.global.security.SecurityErrorHandler;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 인증/권한 설정 (5단계).
 *
 * <p>인증 흐름:
 * <ol>
 *   <li>{@code POST /api/auth/login}으로 아이디/비밀번호를 보내면 {@code AuthService}가 JWT를 발급한다.</li>
 *   <li>이후 요청은 {@code Authorization: Bearer <토큰>} 헤더를 보낸다.</li>
 *   <li>OAuth2 Resource Server 필터가 {@link JwtDecoder}로 서명·만료·발급자를 검증하고,
 *       {@code roles} 클레임을 {@code ROLE_ADMIN}/{@code ROLE_USER} 권한으로 바꿔 인증 객체를 만든다.</li>
 *   <li>아래 URL 규칙으로 권한을 검사한다. 서버는 세션을 만들지 않는다(STATELESS).</li>
 * </ol>
 *
 * <p>URL 권한 규칙 (위에서부터 먼저 일치하는 규칙 적용):
 * <table>
 *   <tr><th>대상</th><th>권한</th></tr>
 *   <tr><td>POST /api/auth/login, Swagger 문서, /error</td><td>공개</td></tr>
 *   <tr><td>/api/users/**</td><td>ADMIN</td></tr>
 *   <tr><td>POST /api/requests/{id}/approve, /reject (신청 승인·반려)</td><td>ADMIN</td></tr>
 *   <tr><td>GET 단어/도메인/용어/공통 코드</td><td>로그인</td></tr>
 *   <tr><td>그 밖의 단어/도메인/용어/공통 코드 (등록·수정·삭제)</td><td>ADMIN</td></tr>
 *   <tr><td>나머지 전부 (테이블/컬럼 정의 쓰기, 변경 신청·조회·취소 포함)</td><td>로그인</td></tr>
 * </table>
 */
@Configuration
public class SecurityConfig {

    /**
     * 표준 데이터 API. 조회는 로그인한 모든 사용자, 쓰기는 ADMIN만.
     * 단어/도메인/용어는 USER가 신청(6단계)으로 바꾸고, 공통 코드(9단계)는 신청 대상이 아니라 ADMIN만 바꾼다.
     * 코드값은 코드 그룹의 하위 경로({@code /api/code-groups/{groupId}/values})라 같은 규칙에 포함된다.
     * 컬럼에 코드 그룹을 매핑하는 것은 컬럼 정의 API라 USER도 할 수 있다.
     */
    private static final String[] STANDARD_DATA_PATHS =
            {"/api/words/**", "/api/domains/**", "/api/terms/**", "/api/code-groups/**"};

    /** 인증 없이 열어 두는 경로. /error는 예외 발생 시 내부 포워드되는 경로라 막으면 원래 오류가 401로 가려진다. */
    private static final String[] PUBLIC_PATHS = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error"};

    /**
     * 보안 필터 체인.
     *
     * @param errorHandler 401/403을 {@code ErrorResponse} JSON으로 응답하는 처리기
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errorHandler)
            throws Exception {
        http
                // 쿠키 기반 인증이 아니므로 CSRF 공격 대상이 아니다. 브라우저가 Authorization 헤더를 자동으로 붙이지 않는다.
                .csrf(AbstractHttpConfigurer::disable)
                // 폼 로그인, HTTP Basic, 기본 로그아웃 페이지는 쓰지 않는다. 로그인은 JSON API로만 한다.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // 요청마다 토큰으로 인증하므로 HTTP 세션을 만들지도, 쓰지도 않는다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        // 신청 승인/반려는 ADMIN. 신청·조회·취소는 아래 anyRequest(로그인)에 해당한다.
                        .requestMatchers(HttpMethod.POST, "/api/requests/*/approve", "/api/requests/*/reject")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, STANDARD_DATA_PATHS).authenticated()
                        .requestMatchers(STANDARD_DATA_PATHS).hasRole("ADMIN")
                        .anyRequest().authenticated())
                // Bearer 토큰 검증 필터를 켠다. 토큰 검증 실패도 같은 JSON 형식으로 응답하도록 처리기를 지정한다.
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                // 토큰 없이 보호된 경로에 접근한 경우(필터 체인 끝의 인가 실패)에도 같은 처리기를 쓴다.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
        return http.build();
    }

    /**
     * 비밀번호 인코더.
     * DelegatingPasswordEncoder는 해시 앞에 {@code {bcrypt}} 같은 알고리즘 접두어를 붙인다.
     * 나중에 알고리즘을 바꿔도 기존 해시를 그대로 검증할 수 있다. 현재 기본은 BCrypt다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** 토큰 서명기. HS256 대칭키({@code app.jwt.secret})로 서명한다. */
    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(properties.secretKey()));
    }

    /**
     * 토큰 검증기. 서명 알고리즘을 HS256으로 고정하고(다른 알고리즘 토큰 거부),
     * 기본 검증(만료 {@code exp}, 발급 전 사용 {@code nbf}, 60초 시계 오차 허용)에 발급자({@code iss}) 검증을 더한다.
     */
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    /** 현재 시각 공급자. 토큰 발급 시각 계산에 쓰며, 테스트에서는 고정/이동한 시계로 바꿔 끼울 수 있다. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /**
     * 검증된 JWT를 인증 객체로 바꾸는 변환기.
     * 기본 설정은 {@code scope} 클레임을 {@code SCOPE_*} 권한으로 바꾸므로,
     * 우리 토큰의 {@code roles} 클레임을 {@code ROLE_*} 권한으로 읽도록 바꾼다. 인증 주체 이름은 {@code sub}(아이디)이다.
     */
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(JwtTokenProvider.ROLES_CLAIM);
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
