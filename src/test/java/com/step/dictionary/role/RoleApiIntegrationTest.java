package com.step.dictionary.role;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 역할 API와, 역할 권한이 실제 요청 검사에 쓰이는지 확인한다.
 * 기본 역할(ADMIN 전권, USER)과 기본 메뉴가 들어 있는 상태에서 시작한다({@link IntegrationTest}).
 */
class RoleApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("기본 역할이 목록에 나오고 새 역할을 등록·수정·삭제할 수 있다")
    void crud() {
        given()
        .when().get("/api/roles")
        .then()
                .statusCode(200)
                .body("code", equalTo(List.of("ADMIN", "USER")))
                .body("find { it.code == 'ADMIN' }.superAdmin", equalTo(true))
                .body("find { it.code == 'USER' }.builtIn", equalTo(true));

        long roleId = given()
                .contentType(ContentType.JSON)
                .body(roleJson("std_manager", "표준 관리자", false, true))
        .when()
                .post("/api/roles")
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/roles/\\d+"))
                .body("code", equalTo("STD_MANAGER"))
                .body("builtIn", equalTo(false))
                .extract().jsonPath().getLong("id");

        given()
                .contentType(ContentType.JSON)
                .body(roleJson("STD_MANAGER", "표준 데이터 관리자", false, false))
        .when()
                .put("/api/roles/{id}", roleId)
        .then()
                .statusCode(200)
                .body("name", equalTo("표준 데이터 관리자"))
                .body("used", equalTo(false));

        given().when().delete("/api/roles/{id}", roleId).then().statusCode(204);
        given().when().get("/api/roles/{id}", roleId).then().statusCode(404).body("code", equalTo("ROLE_NOT_FOUND"));
    }

    @Test
    @DisplayName("역할 코드 중복과 기본 제공 역할 삭제는 거부한다")
    void invalidRequests() {
        given()
                .contentType(ContentType.JSON)
                .body(roleJson("admin", "중복", false, true))
        .when()
                .post("/api/roles")
        .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_ROLE_CODE"));

        given()
        .when().delete("/api/roles/{id}", adminRole.getId())
        .then()
                .statusCode(400)
                .body("code", equalTo("BUILT_IN_ROLE"));
    }

    @Test
    @DisplayName("역할에 준 메뉴 권한이 화면 메뉴와 API 허용에 그대로 적용된다")
    void permissionsApply() {
        // 일반 사용자는 기본 권한으로 단어를 조회만 할 수 있다.
        authenticate(userToken);
        given().when().get("/api/words").then().statusCode(200);
        given().contentType(ContentType.JSON).body(wordJson()).when().post("/api/words").then().statusCode(403);

        // 단어 등록 권한을 가진 역할을 만들어 사용자에게 더해 준다.
        authenticate(adminToken);
        long roleId = given()
                .contentType(ContentType.JSON)
                .body(roleJson("word_manager", "단어 관리자", false, true))
        .when().post("/api/roles")
        .then().statusCode(201)
        .extract().jsonPath().getLong("id");
        long wordMenuId = menuIdOf("/words");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"permissions": [
                          {"menuId": %d, "canRead": true, "canCreate": true, "canUpdate": false, "canDelete": false}]}
                        """.formatted(wordMenuId))
        .when()
                .put("/api/roles/{id}/menus", roleId)
        .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].menuPath", equalTo("/words"))
                .body("[0].canCreate", equalTo(true));

        long userId = given().queryParam("keyword", USER_USERNAME)
                .when().get("/api/users")
                .then().statusCode(200)
                .extract().jsonPath().getLong("content[0].id");
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "user", "roleIds": [%d, %d], "enabled": true}
                        """.formatted(userRole.getId(), roleId))
        .when().put("/api/users/{id}", userId)
        .then().statusCode(200).body("roles.code", hasItem("WORD_MANAGER"));

        // 권한은 요청마다 다시 계산하므로 토큰을 새로 받지 않아도 바로 반영된다.
        authenticate(userToken);
        given().contentType(ContentType.JSON).body(wordJson()).when().post("/api/words").then().statusCode(201);
        // 수정 권한은 주지 않았으므로 여전히 막힌다.
        given().contentType(ContentType.JSON).body(wordJson()).when().put("/api/words/{id}", 1).then().statusCode(403);
        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                .body("find { it.name == '표준 사전' }.children.find { it.path == '/words' }.canCreate", equalTo(true));
    }

    @Test
    @DisplayName("역할 관리 권한이 없는 사용자는 역할 API를 쓸 수 없다")
    void userCannotManageRoles() {
        authenticate(userToken);

        given().when().get("/api/roles").then().statusCode(403);
        given().contentType(ContentType.JSON).body(roleJson("etc", "기타", false, true))
        .when().post("/api/roles").then().statusCode(403);
    }

    /** 기본 메뉴에서 경로로 메뉴 ID를 찾는다. */
    private static long menuIdOf(String path) {
        return given()
                .when().get("/api/menus/all")
                .then().statusCode(200)
                .extract().jsonPath()
                .getLong("children.flatten().find { it.path == '" + path + "' }.id");
    }

    private static String roleJson(String code, String name, boolean superAdmin, boolean used) {
        return """
                {"code": "%s", "name": "%s", "description": null, "superAdmin": %s, "used": %s}
                """.formatted(code, name, superAdmin, used);
    }

    private static String wordJson() {
        return """
                {"name": "고객", "abbreviation": "CUST", "englishName": "Customer", "description": null, "classifier": false}
                """;
    }
}
