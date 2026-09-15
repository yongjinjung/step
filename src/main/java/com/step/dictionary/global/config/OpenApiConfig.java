package com.step.dictionary.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 문서 설정.
 * Swagger UI는 {@code /swagger-ui/index.html}, OpenAPI JSON은 {@code /v3/api-docs}에서 볼 수 있다 (둘 다 인증 없이 열람 가능).
 * 개별 API 설명은 각 컨트롤러의 {@code @Tag}, {@code @Operation}에 적는다.
 *
 * <p>Swagger UI에서 API를 호출하려면 {@code POST /api/auth/login}으로 토큰을 받은 뒤,
 * 오른쪽 위 Authorize 버튼에 토큰 값만 붙여 넣는다 ("Bearer " 접두어는 UI가 붙인다).
 */
@Configuration
public class OpenApiConfig {

    /** 보안 스킴 이름. 문서 안에서 스킴 정의와 요구 사항을 연결하는 키다. */
    private static final String BEARER_SCHEME = "bearerAuth";

    /** API 문서 정보와 JWT Bearer 인증 스킴. 모든 API에 기본으로 인증 요구를 붙이고, 로그인 API만 컨트롤러에서 해제한다. */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("데이터 표준 용어사전 관리 API")
                        .description("표준 단어 / 도메인 / 용어 및 테이블 정의 관리")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
