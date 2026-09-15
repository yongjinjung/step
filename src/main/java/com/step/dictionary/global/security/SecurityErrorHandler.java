package com.step.dictionary.global.security;

import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 보안 필터에서 난 인증(401)/권한(403) 오류를 {@link ErrorResponse} JSON으로 응답한다.
 *
 * <p>이 오류들은 요청이 컨트롤러에 닿기 전에 필터 체인에서 발생하므로 {@code @RestControllerAdvice}로는 잡을 수 없다.
 * 그대로 두면 Spring Security 기본 응답(본문 없는 401/403)이 나가서 다른 API 오류와 형식이 달라진다.
 * {@code SecurityConfig}가 이 클래스를 진입점(entry point)과 접근 거부 처리기로 등록한다.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    /** 스프링 부트가 등록한 Jackson 3 매퍼. 컨트롤러 응답과 같은 직렬화 설정을 쓰기 위해 주입받는다. */
    private final JsonMapper jsonMapper;

    public SecurityErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * 인증 실패(401).
     * <ul>
     *   <li>토큰이 있는데 검증에 실패(서명 불일치, 만료, 형식 오류): {@link OAuth2AuthenticationException} → INVALID_TOKEN</li>
     *   <li>토큰 없이 보호된 API 호출: 그 밖의 인증 예외 → UNAUTHORIZED</li>
     * </ul>
     * 두 경우를 나눠 클라이언트가 "다시 로그인"과 "로그인 안 함"을 구분할 수 있게 한다.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        ErrorCode errorCode = authException instanceof OAuth2AuthenticationException
                ? ErrorCode.INVALID_TOKEN
                : ErrorCode.UNAUTHORIZED;
        // RFC 6750: 401 응답에는 어떤 인증 방식을 써야 하는지 알려주는 헤더를 붙인다.
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, errorCode);
    }

    /** 인증은 됐지만 권한이 부족함(403). 예: USER 역할로 단어 등록, 사용자 관리 API 호출. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(response, ErrorCode.FORBIDDEN);
    }

    /** 상태 코드와 JSON 본문을 쓴다. */
    private void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.of(errorCode));
    }
}
