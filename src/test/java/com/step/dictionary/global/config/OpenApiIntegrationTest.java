package com.step.dictionary.global.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import com.step.dictionary.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("OpenAPI 문서와 Swagger UI는 로그인 없이 제공되고 Bearer 인증 스킴을 포함한다")
    void apiDocs() {
        authenticate(null);

        given()
        .when().get("/v3/api-docs")
        .then()
                .statusCode(200)
                .body("info.title", equalTo("데이터 표준 용어사전 관리 API"))
                .body("paths", hasKey("/api/words"))
                .body("components.securitySchemes.bearerAuth.scheme", equalTo("bearer"))
                // 프론트 코드 생성용: operationId는 "컨트롤러 + 메서드", 응답은 JSON, 엑셀 다운로드는 xlsx
                .body("paths.'/api/words'.get.operationId", equalTo("wordSearch"))
                .body("paths.'/api/words'.post.operationId", equalTo("wordCreate"))
                .body("paths.'/api/terms/{id}'.get.responses.'200'.content", hasKey("application/json"))
                .body("paths.'/api/excel/words'.get.responses.'200'.content",
                        hasKey("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        given()
        .when().get("/swagger-ui/index.html")
        .then()
                .statusCode(200);
    }
}
