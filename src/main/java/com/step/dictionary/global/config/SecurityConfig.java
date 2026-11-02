package com.step.dictionary.global.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.step.dictionary.auth.JwtProperties;
import com.step.dictionary.auth.JwtTokenProvider;
import com.step.dictionary.global.security.MenuAuthorizationManager;
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
 * 인증/권한 설정.
 *
 * <p>인증 흐름:
 * <ol>
 *   <li>{@code POST /api/auth/login}으로 아이디/비밀번호를 보내면 {@code AuthService}가 JWT를 발급한다.</li>
 *   <li>이후 요청은 {@code Authorization: Bearer <토큰>} 헤더를 보낸다.</li>
 *   <li>OAuth2 Resource Server 필터가 {@link JwtDecoder}로 서명·만료·발급자를 검증하고,
 *       {@code roles} 클레임(역할 코드)을 {@code ROLE_*} 권한으로 바꿔 인증 객체를 만든다.
 *       다만 실제 기능 권한 검사는 이 권한 목록이 아니라 DB의 역할별 메뉴 권한으로 한다.</li>
 *   <li>서버는 세션을 만들지 않는다(STATELESS).</li>
 * </ol>
 *
 * <p>URL 권한 규칙 (위에서부터 먼저 일치하는 규칙 적용):
 * <table>
 *   <tr><th>대상</th><th>권한</th></tr>
 *   <tr><td>POST /api/auth/login, Swagger 문서, /error</td><td>공개</td></tr>
 *   <tr><td>/api/auth/** (내 정보, 비밀번호 변경)</td><td>로그인</td></tr>
 *   <tr><td>GET /api/menus (화면에 그릴 메뉴)</td><td>로그인 (내용은 권한에 맞춰 걸러 나간다)</td></tr>
 *   <tr><td>그 밖의 모든 API</td><td>역할의 메뉴 권한 ({@link MenuAuthorizationManager})</td></tr>
 * </table>
 * 메뉴·동작 대응은 {@code ApiPermissionRules}에 있다. 새 API를 만들면 그 표에 규칙을 추가한다.
 */
@Configuration
public class SecurityConfig {

    /** 인증 없이 열어 두는 경로. /error는 예외 발생 시 내부 포워드되는 경로라 막으면 원래 오류가 401로 가려진다. */
    private static final String[] PUBLIC_PATHS = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error"};

    /**
     * 보안 필터 체인.
     *
     * @param errorHandler         401/403을 {@code ErrorResponse} JSON으로 응답하는 처리기
     * @param permissionAuthorizer 역할의 메뉴 권한으로 요청을 검사하는 처리기
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errorHandler,
            MenuAuthorizationManager permissionAuthorizer) throws Exception {
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
                        // 내 정보·비밀번호 변경은 모든 로그인 사용자가 자기 것만 다룬다 (메뉴 권한과 무관).
                        .requestMatchers("/api/auth/**").authenticated()
                        // 화면에 그릴 메뉴는 로그인만 하면 조회할 수 있다. 볼 수 있는 메뉴만 담겨 나간다.
                        .requestMatchers(HttpMethod.GET, "/api/menus").authenticated()
                        // 나머지 API는 역할의 메뉴 권한으로 검사한다.
                        .anyRequest().access(permissionAuthorizer))
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
     * 우리 토큰의 {@code roles} 클레임(역할 코드)을 {@code ROLE_*} 권한으로 읽도록 바꾼다.
     * 인증 주체 이름은 {@code sub}(아이디)이고, 이 아이디로 DB의 역할 권한을 조회한다.
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
