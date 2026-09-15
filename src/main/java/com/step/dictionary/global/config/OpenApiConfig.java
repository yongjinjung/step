package com.step.dictionary.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * springdoc-openapi 문서 설정.
 * Swagger UI는 {@code /swagger-ui/index.html}, OpenAPI JSON은 {@code /v3/api-docs}에서 볼 수 있다 (둘 다 인증 없이 열람 가능).
 * 개별 API 설명은 각 컨트롤러의 {@code @Tag}, {@code @Operation}에 적는다.
 *
 * <p>Swagger UI에서 API를 호출하려면 {@code POST /api/auth/login}으로 토큰을 받은 뒤,
 * 오른쪽 위 Authorize 버튼에 토큰 값만 붙여 넣는다 ("Bearer " 접두어는 UI가 붙인다).
 *
 * <p>프론트엔드({@code frontend/})는 이 문서로 API 클라이언트 코드를 생성하므로, operationId와 응답 타입이 곧 생성 코드의 이름과 타입이 된다.
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

    /**
     * operationId를 "컨트롤러 이름(Controller 제외) + 메서드 이름"으로 만든다. 예: {@code WordController.search} → {@code wordSearch}.
     *
     * <p>springdoc 기본값은 메서드 이름이라 여러 컨트롤러의 {@code create}가 {@code create_1}, {@code create_2}처럼
     * 등록 순서에 따라 번호가 붙는다. 번호는 컨트롤러가 늘면 바뀔 수 있어, 이 값으로 이름을 만드는 프론트 생성 코드가 흔들린다.
     * 컨트롤러 안에서 메서드 이름은 겹치지 않으므로 이 규칙이면 전체에서 유일하고 안정적이다.
     */
    @Bean
    public OperationCustomizer operationIdCustomizer() {
        return (operation, handlerMethod) -> {
            String controller = handlerMethod.getBeanType().getSimpleName().replaceFirst("Controller$", "");
            operation.setOperationId(StringUtils.uncapitalize(controller)
                    + StringUtils.capitalize(handlerMethod.getMethod().getName()));
            return operation;
        };
    }
}
