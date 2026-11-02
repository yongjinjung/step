package com.step.dictionary.menu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 사용자별 메뉴 지정(좁히기) API.
 *
 * <p>역할 권한이 먼저이고, 이 지정은 그중에서 더 좁히는 설정이다.
 * 기본 메뉴와 일반 사용자 기본 권한이 들어 있는 상태에서 시작한다({@link IntegrationTest}).
 */
class MenuPermissionApiIntegrationTest extends IntegrationTest {

    private long userId;
    private long wordMenuId;
    private long domainMenuId;
    private long usersMenuId;

    @BeforeEach
    void setUpIds() {
        userId = given().queryParam("keyword", USER_USERNAME)
                .when().get("/api/users")
                .then().statusCode(200)
                .extract().jsonPath().getLong("content[0].id");
        wordMenuId = menuIdOf("/words");
        domainMenuId = menuIdOf("/domains");
        usersMenuId = menuIdOf("/users");
    }

    @Test
    @DisplayName("메뉴를 지정하면 그 사용자는 지정한 메뉴만 보고, 기본으로 되돌리면 역할 권한대로 본다")
    void restrictAndRestore() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"useDefault": false, "menuIds": [%d]}
                        """.formatted(domainMenuId))
        .when()
                .put("/api/users/{userId}/menus", userId)
        .then()
                .statusCode(200)
                .body("useDefault", equalTo(false))
                .body("menuIds", hasSize(1))
                .body("username", equalTo(USER_USERNAME));

        authenticate(userToken);
        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                .body("find { it.name == '표준 사전' }.children.name", equalTo(List.of("표준 도메인")))
                .body("name", not(hasItem("메타 관리")));

        // 관리자는 자기 지정이 없으므로 그대로 모두 본다.
        authenticate(adminToken);
        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                .body("find { it.name == '표준 사전' }.children.name", hasItem("표준 단어"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"useDefault": true, "menuIds": []}
                        """)
        .when()
                .put("/api/users/{userId}/menus", userId)
        .then()
                .statusCode(200)
                .body("useDefault", equalTo(true))
                .body("menuIds", hasSize(0));

        authenticate(userToken);
        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                .body("find { it.name == '표준 사전' }.children.name", hasItem("표준 단어"));
    }

    @Test
    @DisplayName("역할 권한이 없는 메뉴는 지정해도 보이지 않는다 (좁히기만 한다)")
    void permissionStillApplies() {
        setPermission(List.of(wordMenuId, usersMenuId));

        authenticate(userToken);

        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                .body("find { it.name == '표준 사전' }.children.name", equalTo(List.of("표준 단어")))
                // 사용자 관리 메뉴는 일반 사용자 역할에 권한이 없어 지정해도 나오지 않는다.
                .body("name", not(hasItem("관리")));
    }

    @Test
    @DisplayName("지정이 걸린 메뉴를 삭제해도 실패하지 않고 지정에서도 빠진다")
    void deleteMenuWithPermission() {
        long tempTopId = createMenu(null, "ZZ 묶음", null);
        long tempMenuId = createMenu(tempTopId, "ZZ 화면", "/zz-one");
        setPermission(List.of(wordMenuId, tempMenuId));

        given().when().delete("/api/menus/{id}", tempMenuId).then().statusCode(204);

        given()
        .when().get("/api/users/{userId}/menus", userId)
        .then()
                .statusCode(200)
                .body("menuIds", equalTo(List.of((int) wordMenuId)));
    }

    @Test
    @DisplayName("없는 사용자나 없는 메뉴로는 지정할 수 없고, 일반 사용자는 이 API를 쓸 수 없다")
    void invalidRequests() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"useDefault": false, "menuIds": [999999]}
                        """)
        .when()
                .put("/api/users/{userId}/menus", userId)
        .then()
                .statusCode(404)
                .body("code", equalTo("MENU_NOT_FOUND"));

        given()
        .when().get("/api/users/{userId}/menus", 999_999)
        .then()
                .statusCode(404)
                .body("code", equalTo("USER_NOT_FOUND"));

        authenticate(userToken);
        given().when().get("/api/users/{userId}/menus", userId).then().statusCode(403);
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"useDefault": true}
                        """)
        .when().put("/api/users/{userId}/menus", userId)
        .then().statusCode(403);
    }

    /** 기본 메뉴에서 경로로 메뉴 ID를 찾는다. */
    private static long menuIdOf(String path) {
        return given()
                .when().get("/api/menus/all")
                .then().statusCode(200)
                .extract().jsonPath()
                .getLong("children.flatten().find { it.path == '" + path + "' }.id");
    }

    private void setPermission(List<Long> menuIds) {
        String ids = menuIds.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("");
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"useDefault": false, "menuIds": [%s]}
                        """.formatted(ids))
        .when()
                .put("/api/users/{userId}/menus", userId)
        .then().statusCode(200);
    }

    private static long createMenu(Long parentId, String name, String path) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"parentId": %s, "name": "%s", "path": %s, "icon": null, "sortOrder": 90, "used": true}
                        """.formatted(parentId, name, path == null ? "null" : "\"" + path + "\""))
                .when()
                .post("/api/menus")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }
}
