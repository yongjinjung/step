package com.step.dictionary.menu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 사용자별 메뉴 권한 API와, 권한이 화면 메뉴 조회에 적용되는지 확인한다. */
class MenuPermissionApiIntegrationTest extends IntegrationTest {

    private long userId;
    private long standardId;
    private long wordMenuId;
    private long domainMenuId;
    private long tableMenuId;

    /** 표준 사전(단어·도메인), 메타 관리(테이블) 메뉴를 만들고 일반 사용자 ID를 찾아 둔다. */
    @BeforeEach
    void setUpMenus() {
        standardId = createMenu(null, "표준 사전", null, 1, true, false);
        wordMenuId = createMenu(standardId, "표준 단어", "/words", 1, true, false);
        domainMenuId = createMenu(standardId, "표준 도메인", "/domains", 2, true, false);
        long metaId = createMenu(null, "메타 관리", null, 2, true, false);
        tableMenuId = createMenu(metaId, "테이블 정의", "/tables", 1, true, false);

        userId = given().queryParam("keyword", USER_USERNAME)
                .when().get("/api/users")
                .then().statusCode(200)
                .extract().jsonPath().getLong("content[0].id");
    }

    @Test
    @DisplayName("메뉴 권한을 지정하면 그 사용자는 지정한 메뉴만 보고, 기본으로 되돌리면 다시 모두 본다")
    void restrictAndRestore() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"useDefault": false, "menuIds": [%d, %d]}
                        """.formatted(domainMenuId, tableMenuId))
        .when()
                .put("/api/users/{userId}/menus", userId)
        .then()
                .statusCode(200)
                .body("useDefault", equalTo(false))
                .body("menuIds", hasSize(2))
                .body("username", equalTo(USER_USERNAME));

        authenticate(userToken);
        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                .body("name", equalTo(List.of("표준 사전", "메타 관리")))
                .body("[0].children.name", equalTo(List.of("표준 도메인")))
                .body("[1].children.name", equalTo(List.of("테이블 정의")));

        // 관리자는 자기 권한을 지정하지 않았으므로 그대로 모두 본다.
        authenticate(adminToken);
        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                .body("[0].children.name", equalTo(List.of("표준 단어", "표준 도메인")));

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
                .body("[0].children.name", equalTo(List.of("표준 단어", "표준 도메인")));
    }

    @Test
    @DisplayName("권한을 지정해도 미사용 메뉴는 보이지 않는다")
    void hiddenMenuStaysHidden() {
        // 표준 단어 메뉴를 미사용으로 바꾼다.
        given()
                .contentType(ContentType.JSON)
                .body(menuJson(standardId, "표준 단어", "/words", 1, false, false))
        .when()
                .put("/api/menus/{id}", wordMenuId)
        .then().statusCode(200);
        setPermission(List.of(wordMenuId, domainMenuId));

        authenticate(userToken);

        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                .body("[0].children.name", equalTo(List.of("표준 도메인")));
    }

    @Test
    @DisplayName("권한이 걸린 메뉴를 삭제해도 권한 행 때문에 실패하지 않는다")
    void deleteMenuWithPermission() {
        setPermission(List.of(wordMenuId, domainMenuId));

        given().when().delete("/api/menus/{id}", wordMenuId).then().statusCode(204);

        given()
        .when().get("/api/users/{userId}/menus", userId)
        .then()
                .statusCode(200)
                .body("menuIds", equalTo(List.of((int) domainMenuId)));
    }

    @Test
    @DisplayName("없는 사용자나 없는 메뉴로는 권한을 저장할 수 없고, 일반 사용자는 권한 API를 쓸 수 없다")
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

    private void setPermission(List<Long> menuIds) {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"useDefault": false, "menuIds": [%s]}
                        """.formatted(menuIds.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("")))
        .when()
                .put("/api/users/{userId}/menus", userId)
        .then().statusCode(200);
    }

    private static long createMenu(Long parentId, String name, String path, int sortOrder, boolean used,
            boolean adminOnly) {
        return given()
                .contentType(ContentType.JSON)
                .body(menuJson(parentId, name, path, sortOrder, used, adminOnly))
                .when()
                .post("/api/menus")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static String menuJson(Long parentId, String name, String path, int sortOrder, boolean used,
            boolean adminOnly) {
        return """
                {"parentId": %s, "name": "%s", "path": %s, "icon": null, "sortOrder": %d, "used": %s, "adminOnly": %s}
                """.formatted(parentId, name, path == null ? "null" : "\"" + path + "\"", sortOrder, used, adminOnly);
    }
}
