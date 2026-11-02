package com.step.dictionary.global.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.step.dictionary.auth.JwtProperties;
import com.step.dictionary.auth.JwtTokenProvider;
import com.step.dictionary.global.config.SecurityConfig;
import com.step.dictionary.support.IntegrationTest;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.support.TestRoles;
import io.restassured.http.ContentType;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwtEncoder;

/** URL 권한 규칙과 401/403 응답 형식을 확인한다 ({@link SecurityConfig}). */
class SecurityApiIntegrationTest extends IntegrationTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    @DisplayName("토큰 없이 보호된 API를 호출하면 401 UNAUTHORIZED를 JSON으로 반환한다")
    void withoutToken() {
        authenticate(null);

        given()
        .when().get("/api/words")
        .then()
                .statusCode(401)
                .header("WWW-Authenticate", "Bearer")
                .contentType(ContentType.JSON)
                .body("code", equalTo("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 401 INVALID_TOKEN을 반환한다")
    void malformedToken() {
        authenticate("not-a-jwt");

        given()
        .when().get("/api/words")
        .then()
                .statusCode(401)
                .body("code", equalTo("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("만료된 토큰은 401 INVALID_TOKEN을 반환한다")
    void expiredToken() {
        Clock twoHoursAgo = Clock.offset(Clock.systemUTC(), Duration.ofHours(-2));
        JwtTokenProvider pastProvider = new JwtTokenProvider(jwtEncoder, jwtProperties, twoHoursAgo);
        authenticate(pastProvider.issue(new AppUser(ADMIN_USERNAME, "hash", "관리자", TestRoles.adminOnly())).accessToken());

        given()
        .when().get("/api/words")
        .then()
                .statusCode(401)
                .body("code", equalTo("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("다른 비밀키로 서명한 토큰은 401 INVALID_TOKEN을 반환한다")
    void tokenSignedWithOtherKey() {
        JwtProperties other = new JwtProperties(
                "attacker-jwt-secret-0123456789abcdefghij", jwtProperties.expiration(), jwtProperties.issuer());
        JwtTokenProvider forgedProvider =
                new JwtTokenProvider(new SecurityConfig().jwtEncoder(other), other, Clock.systemUTC());
        authenticate(forgedProvider.issue(new AppUser(ADMIN_USERNAME, "hash", "관리자", TestRoles.adminOnly())).accessToken());

        given()
        .when().get("/api/words")
        .then()
                .statusCode(401)
                .body("code", equalTo("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("일반 사용자는 표준 단어/도메인/용어와 공통 코드를 조회할 수 있다")
    void userCanReadStandardData() {
        authenticate(userToken);

        given().when().get("/api/words").then().statusCode(200);
        given().when().get("/api/domains").then().statusCode(200);
        given().when().get("/api/terms").then().statusCode(200);
        given().when().get("/api/code-groups").then().statusCode(200);
    }

    @Test
    @DisplayName("일반 사용자가 표준 단어/도메인/용어와 공통 코드를 쓰려 하면 403 FORBIDDEN을 반환한다")
    void userCannotWriteStandardData() {
        authenticate(userToken);

        given().contentType(ContentType.JSON).body("{}")
        .when().post("/api/words")
        .then()
                .statusCode(403)
                .contentType(ContentType.JSON)
                .body("code", equalTo("FORBIDDEN"));
        given().contentType(ContentType.JSON).body("{}")
        .when().put("/api/domains/{id}", 1)
        .then().statusCode(403);
        given()
        .when().delete("/api/terms/{id}", 1)
        .then().statusCode(403);
        given().contentType(ContentType.JSON).body("{}")
        .when().post("/api/code-groups")
        .then().statusCode(403);
        given().contentType(ContentType.JSON).body("{}")
        .when().post("/api/code-groups/{groupId}/values", 1)
        .then().statusCode(403);
    }

    @Test
    @DisplayName("일반 사용자도 테이블 정의를 등록할 수 있다")
    void userCanWriteMeta() {
        authenticate(userToken);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"physicalName": "TB_CUSTOMER", "logicalName": "고객", "description": null}
                        """)
        .when().post("/api/tables")
        .then().statusCode(201);
    }
}
