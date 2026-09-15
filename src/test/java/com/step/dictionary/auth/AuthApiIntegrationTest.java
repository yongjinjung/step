package com.step.dictionary.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("로그인하면 토큰을 발급하고 그 토큰으로 내 정보를 조회할 수 있다")
    void loginAndMe() {
        authenticate(null);

        String token = login("ADMIN", PASSWORD)
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", equalTo(3600))
                .extract().jsonPath().getString("accessToken");

        authenticate(token);
        given()
        .when().get("/api/auth/me")
        .then()
                .statusCode(200)
                .body("username", equalTo(ADMIN_USERNAME))
                .body("role", equalTo("ADMIN"))
                .body("$", not(hasKey("password")));
    }

    @Test
    @DisplayName("비밀번호가 틀리거나 아이디가 없으면 같은 401 오류를 준다")
    void loginFailure() {
        authenticate(null);

        login(ADMIN_USERNAME, "wrong-password").statusCode(401).body("code", equalTo("INVALID_CREDENTIALS"));
        login("nobody", PASSWORD).statusCode(401).body("code", equalTo("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("비활성화된 계정은 로그인할 수 없다")
    void loginDisabled() {
        long userId = given().queryParam("keyword", USER_USERNAME)
                .when().get("/api/users")
                .then().statusCode(200)
                .extract().jsonPath().getLong("content.find { it.username == 'user' }.id");
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "user", "role": "USER", "enabled": false}
                        """)
        .when().put("/api/users/{id}", userId)
        .then().statusCode(200);

        authenticate(null);
        login(USER_USERNAME, PASSWORD).statusCode(403).body("code", equalTo("USER_DISABLED"));
    }

    @Test
    @DisplayName("로그인 입력값이 비어 있으면 400을 반환한다")
    void loginInvalid() {
        authenticate(null);

        login("", PASSWORD).statusCode(400).body("code", equalTo("INVALID_INPUT"));
    }

    @Test
    @DisplayName("토큰 없이 내 정보를 조회하면 401을 반환한다")
    void meWithoutToken() {
        authenticate(null);

        given()
        .when().get("/api/auth/me")
        .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("내 비밀번호를 바꾸면 새 비밀번호로만 로그인할 수 있다")
    void changePassword() {
        authenticate(userToken);
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"currentPassword": "%s", "newPassword": "newpass12!"}
                        """.formatted(PASSWORD))
        .when().put("/api/auth/me/password")
        .then().statusCode(204);

        authenticate(null);
        login(USER_USERNAME, PASSWORD).statusCode(401);
        login(USER_USERNAME, "newpass12!").statusCode(200);
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 비밀번호를 바꿀 수 없다")
    void changePasswordWrongCurrent() {
        authenticate(userToken);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"currentPassword": "wrong-pass", "newPassword": "newpass12!"}
                        """)
        .when().put("/api/auth/me/password")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_CURRENT_PASSWORD"));
    }

    private static ValidatableResponse login(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"username": "%s", "password": "%s"}
                        """.formatted(username, password))
                .when().post("/api/auth/login")
                .then();
    }
}
