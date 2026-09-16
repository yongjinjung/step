package com.step.dictionary.menu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 메뉴 API. 통합 테스트는 매번 모든 테이블을 비우므로 기동 시 만들어진 기본 메뉴는 없는 상태에서 시작한다
 * (기본 메뉴 생성은 {@link MenuInitializerTest}에서 확인한다).
 */
class MenuApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("상위 메뉴와 하위 메뉴를 등록하면 트리로 조회된다")
    void createAndFindTree() {
        long topId = given()
                .contentType(ContentType.JSON)
                .body(menuJson(null, "표준 사전", null, 1, true, false))
        .when()
                .post("/api/menus")
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/menus/\\d+"))
                .body("path", nullValue())
                .body("children", hasSize(0))
                .extract().jsonPath().getLong("id");
        createMenu(topId, "표준 단어", "/words", 1);
        createMenu(topId, "표준 도메인", "/domains", 2);

        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                .body("name", equalTo(List.of("표준 사전")))
                .body("[0].children.name", equalTo(List.of("표준 단어", "표준 도메인")))
                .body("[0].children.path", equalTo(List.of("/words", "/domains")))
                .body("[0].children[0].parentId", equalTo((int) topId));
    }

    @Test
    @DisplayName("상위 메뉴에 경로를 주거나 하위 메뉴에 경로가 없으면 400을 반환한다")
    void invalidPath() {
        long topId = createMenu(null, "표준 사전", null, 1);

        given()
                .contentType(ContentType.JSON)
                .body(menuJson(null, "잘못된 상위", "/words", 2, true, false))
        .when()
                .post("/api/menus")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_MENU_PATH"));

        given()
                .contentType(ContentType.JSON)
                .body(menuJson(topId, "경로 없는 하위", null, 1, true, false))
        .when()
                .post("/api/menus")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_MENU_PATH"));
    }

    @Test
    @DisplayName("경로가 중복되거나 하위 메뉴 아래에 만들려 하면 거부한다")
    void duplicatePathAndDepth() {
        long topId = createMenu(null, "표준 사전", null, 1);
        long subId = createMenu(topId, "표준 단어", "/words", 1);

        given()
                .contentType(ContentType.JSON)
                .body(menuJson(topId, "단어 복사", "/words", 2, true, false))
        .when()
                .post("/api/menus")
        .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_MENU_PATH"));

        given()
                .contentType(ContentType.JSON)
                .body(menuJson(subId, "더 아래", "/words/more", 1, true, false))
        .when()
                .post("/api/menus")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_MENU_PARENT"));
    }

    @Test
    @DisplayName("하위 메뉴를 다른 상위 메뉴로 옮기고 이름·순서를 바꾼다")
    void moveToOtherTopMenu() {
        long standardId = createMenu(null, "표준 사전", null, 1);
        long metaId = createMenu(null, "메타 관리", null, 2);
        long subId = createMenu(standardId, "테이블 정의", "/tables", 1);

        given()
                .contentType(ContentType.JSON)
                .body(menuJson(metaId, "테이블 정의", "/tables", 3, true, false))
        .when()
                .put("/api/menus/{id}", subId)
        .then()
                .statusCode(200)
                .body("parentId", equalTo((int) metaId))
                .body("sortOrder", equalTo(3));

        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                // 옮긴 뒤 표준 사전에는 하위가 없어 화면 메뉴에서 빠진다.
                .body("name", equalTo(List.of("메타 관리")))
                .body("[0].children.name", equalTo(List.of("테이블 정의")));
    }

    @Test
    @DisplayName("하위 메뉴가 있는 메뉴는 삭제할 수 없고, 하위를 지운 뒤에는 삭제된다")
    void deleteMenu() {
        long topId = createMenu(null, "표준 사전", null, 1);
        long subId = createMenu(topId, "표준 단어", "/words", 1);

        given()
        .when().delete("/api/menus/{id}", topId)
        .then()
                .statusCode(409)
                .body("code", equalTo("MENU_HAS_CHILDREN"));

        given().when().delete("/api/menus/{id}", subId).then().statusCode(204);
        given().when().delete("/api/menus/{id}", topId).then().statusCode(204);

        given()
        .when().get("/api/menus/{id}", topId)
        .then()
                .statusCode(404)
                .body("code", equalTo("MENU_NOT_FOUND"));
    }

    @Test
    @DisplayName("일반 사용자는 자기 메뉴만 조회하고, 전체 조회와 메뉴 관리는 할 수 없다")
    void userPermission() {
        long standardId = createMenu(null, "표준 사전", null, 1);
        createMenu(standardId, "표준 단어", "/words", 1);
        createMenu(standardId, "숨긴 화면", "/hidden", 2, false, false);
        long adminTopId = createMenu(null, "관리", null, 2, true, true);
        createMenu(adminTopId, "사용자 관리", "/users", 1, true, true);

        authenticate(userToken);

        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                // 관리자 전용 상위 메뉴와 미사용 하위 메뉴는 빠진다.
                .body("name", equalTo(List.of("표준 사전")))
                .body("[0].children.name", equalTo(List.of("표준 단어")));

        given().when().get("/api/menus/all").then().statusCode(403);

        given()
                .contentType(ContentType.JSON)
                .body(menuJson(standardId, "임의 메뉴", "/etc", 3, true, false))
        .when()
                .post("/api/menus")
        .then().statusCode(403);

        authenticate(adminToken);

        given()
        .when().get("/api/menus/all")
        .then()
                .statusCode(200)
                .body("name", equalTo(List.of("표준 사전", "관리")))
                .body("[0].children.name", equalTo(List.of("표준 단어", "숨긴 화면")));
    }

    private static long createMenu(Long parentId, String name, String path, int sortOrder) {
        return createMenu(parentId, name, path, sortOrder, true, false);
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
