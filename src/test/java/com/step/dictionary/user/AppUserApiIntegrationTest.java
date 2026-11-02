package com.step.dictionary.user;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppUserApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("관리자가 계정을 만들면 201을 반환하고 새 계정으로 로그인할 수 있다")
    void create() {
        given()
                .contentType(ContentType.JSON)
                .body(createJson("Hong", "password1!", userRole.getId()))
        .when()
                .post("/api/users")
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/users/\\d+"))
                .body("username", equalTo("hong"))
                .body("roles.code", equalTo(List.of("USER")))
                .body("enabled", equalTo(true))
                .body("$", not(hasKey("password")));

        authenticate(null);
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"username": "hong", "password": "password1!"}
                        """)
        .when().post("/api/auth/login")
        .then().statusCode(200);
    }

    @Test
    @DisplayName("아이디가 대소문자만 다르게 중복되면 409를 반환한다")
    void createDuplicate() {
        given()
                .contentType(ContentType.JSON)
                .body(createJson("ADMIN", "password1!", adminRole.getId()))
        .when()
                .post("/api/users")
        .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_USERNAME"));
    }

    @Test
    @DisplayName("비밀번호가 규칙보다 짧으면 400과 필드 오류를 반환한다")
    void createInvalidPassword() {
        given()
                .contentType(ContentType.JSON)
                .body(createJson("hong", "short", userRole.getId()))
        .when()
                .post("/api/users")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT"))
                .body("fieldErrors.field", equalTo(List.of("password")));
    }

    @Test
    @DisplayName("키워드와 역할로 계정을 검색한다")
    void search() {
        given().contentType(ContentType.JSON).body(createJson("hong", "password1!", userRole.getId()))
        .when().post("/api/users").then().statusCode(201);

        given().queryParam("roleId", userRole.getId())
        .when().get("/api/users")
        .then()
                .statusCode(200)
                .body("content.username", equalTo(List.of("hong", "user")));

        given().queryParam("keyword", "adm")
        .when().get("/api/users")
        .then()
                .statusCode(200)
                .body("content.username", equalTo(List.of("admin")));
    }

    @Test
    @DisplayName("마지막 활성 관리자는 강등할 수 없고, 다른 관리자가 생기면 강등할 수 있다")
    void lastAdmin() {
        long adminId = findUserId(ADMIN_USERNAME);

        given().contentType(ContentType.JSON).body(updateJson("관리자", userRole.getId(), true))
        .when().put("/api/users/{id}", adminId)
        .then()
                .statusCode(409)
                .body("code", equalTo("LAST_ADMIN_REQUIRED"));

        given().contentType(ContentType.JSON).body(createJson("admin2", "password1!", adminRole.getId()))
        .when().post("/api/users").then().statusCode(201);

        given().contentType(ContentType.JSON).body(updateJson("관리자", userRole.getId(), true))
        .when().put("/api/users/{id}", adminId)
        .then()
                .statusCode(200)
                .body("roles.code", equalTo(List.of("USER")));
    }

    @Test
    @DisplayName("관리자가 비밀번호를 초기화하면 새 비밀번호로 로그인할 수 있다")
    void resetPassword() {
        long userId = findUserId(USER_USERNAME);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"password": "resetpass1!"}
                        """)
        .when().put("/api/users/{id}/password", userId)
        .then().statusCode(204);

        authenticate(null);
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"username": "user", "password": "resetpass1!"}
                        """)
        .when().post("/api/auth/login")
        .then().statusCode(200);
    }

    @Test
    @DisplayName("없는 계정을 조회하면 404를 반환한다")
    void findByIdNotFound() {
        given()
        .when().get("/api/users/{id}", 999_999)
        .then()
                .statusCode(404)
                .body("code", equalTo("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("일반 사용자는 사용자 관리 API를 쓸 수 없다")
    void forbiddenForUser() {
        authenticate(userToken);

        given()
        .when().get("/api/users")
        .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"));
    }

    private static long findUserId(String username) {
        return given().queryParam("keyword", username)
                .when().get("/api/users")
                .then().statusCode(200)
                .extract().jsonPath().getLong("content.find { it.username == '%s' }.id".formatted(username));
    }

    private static String createJson(String username, String password, Long roleId) {
        return """
                {"username": "%s", "password": "%s", "name": "%s", "roleIds": [%d]}
                """.formatted(username, password, username, roleId);
    }

    private static String updateJson(String name, Long roleId, boolean enabled) {
        return """
                {"name": "%s", "roleIds": [%d], "enabled": %s}
                """.formatted(name, roleId, enabled);
    }
}
