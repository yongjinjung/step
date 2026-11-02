package com.step.dictionary.menu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 메뉴 API.
 *
 * <p>통합 테스트는 기동 초기화와 같은 기본 메뉴({@link MenuInitializer})가 들어 있는 상태에서 시작하므로,
 * 여기서 만드는 메뉴는 이름·경로를 {@code ZZ}로 구분해 확인한다.
 */
class MenuApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("상위 메뉴와 하위 메뉴를 등록하면 트리로 조회된다")
    void createAndFindTree() {
        long topId = given()
                .contentType(ContentType.JSON)
                .body(menuJson(null, "ZZ 묶음", null, 90, true))
        .when()
                .post("/api/menus")
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/menus/\\d+"))
                .body("path", nullValue())
                .body("children", hasSize(0))
                .extract().jsonPath().getLong("id");
        createMenu(topId, "ZZ 화면1", "/zz-one", 1);
        createMenu(topId, "ZZ 화면2", "/zz-two", 2);

        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                .body("name", hasItem("ZZ 묶음"))
                .body("find { it.name == 'ZZ 묶음' }.children.name", equalTo(List.of("ZZ 화면1", "ZZ 화면2")))
                .body("find { it.name == 'ZZ 묶음' }.children.path", equalTo(List.of("/zz-one", "/zz-two")))
                .body("find { it.name == 'ZZ 묶음' }.children[0].parentId", equalTo((int) topId));
    }

    @Test
    @DisplayName("상위 메뉴에 경로를 주거나 하위 메뉴에 경로가 없으면 400을 반환한다")
    void invalidPath() {
        long topId = createMenu(null, "ZZ 묶음", null, 90);

        given()
                .contentType(ContentType.JSON)
                .body(menuJson(null, "잘못된 상위", "/zz-one", 91, true))
        .when()
                .post("/api/menus")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_MENU_PATH"));

        given()
                .contentType(ContentType.JSON)
                .body(menuJson(topId, "경로 없는 하위", null, 1, true))
        .when()
                .post("/api/menus")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_MENU_PATH"));
    }

    @Test
    @DisplayName("경로가 중복되거나 하위 메뉴 아래에 만들려 하면 거부한다")
    void duplicatePathAndDepth() {
        long topId = createMenu(null, "ZZ 묶음", null, 90);
        long subId = createMenu(topId, "ZZ 화면1", "/zz-one", 1);

        given()
                .contentType(ContentType.JSON)
                .body(menuJson(topId, "복사", "/zz-one", 2, true))
        .when()
                .post("/api/menus")
        .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_MENU_PATH"));

        // 기본 메뉴의 경로와 겹쳐도 같은 오류다.
        given()
                .contentType(ContentType.JSON)
                .body(menuJson(topId, "단어 복사", "/words", 3, true))
        .when()
                .post("/api/menus")
        .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_MENU_PATH"));

        given()
                .contentType(ContentType.JSON)
                .body(menuJson(subId, "더 아래", "/zz-deep", 1, true))
        .when()
                .post("/api/menus")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_MENU_PARENT"));
    }

    @Test
    @DisplayName("하위 메뉴를 다른 상위 메뉴로 옮기고 이름·순서를 바꾼다")
    void moveToOtherTopMenu() {
        long firstId = createMenu(null, "ZZ 묶음1", null, 90);
        long secondId = createMenu(null, "ZZ 묶음2", null, 91);
        long subId = createMenu(firstId, "ZZ 화면", "/zz-one", 1);

        given()
                .contentType(ContentType.JSON)
                .body(menuJson(secondId, "ZZ 화면", "/zz-one", 3, true))
        .when()
                .put("/api/menus/{id}", subId)
        .then()
                .statusCode(200)
                .body("parentId", equalTo((int) secondId))
                .body("sortOrder", equalTo(3));

        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                // 옮긴 뒤 첫 묶음에는 하위가 없어 화면 메뉴에서 빠진다.
                .body("name", not(hasItem("ZZ 묶음1")))
                .body("find { it.name == 'ZZ 묶음2' }.children.name", equalTo(List.of("ZZ 화면")));
    }

    @Test
    @DisplayName("하위 메뉴가 있는 메뉴는 삭제할 수 없고, 하위를 지운 뒤에는 삭제된다")
    void deleteMenu() {
        long topId = createMenu(null, "ZZ 묶음", null, 90);
        long subId = createMenu(topId, "ZZ 화면", "/zz-one", 1);

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
    @DisplayName("일반 사용자는 권한이 있는 메뉴만 보고, 전체 조회와 메뉴 관리는 할 수 없다")
    void userPermission() {
        long topId = createMenu(null, "ZZ 묶음", null, 90);
        createMenu(topId, "ZZ 화면", "/zz-one", 1);

        authenticate(userToken);

        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                // 기본 권한이 있는 메뉴는 보이고, 권한을 주지 않은 새 메뉴와 관리 메뉴는 보이지 않는다.
                .body("name", hasItem("표준 사전"))
                .body("find { it.name == '표준 사전' }.children.name", hasItem("표준 단어"))
                .body("name", not(hasItem("ZZ 묶음")))
                .body("name", not(hasItem("관리")));

        given().when().get("/api/menus/all").then().statusCode(403);

        given()
                .contentType(ContentType.JSON)
                .body(menuJson(topId, "임의 메뉴", "/zz-etc", 2, true))
        .when()
                .post("/api/menus")
        .then().statusCode(403);

        authenticate(adminToken);

        given()
        .when().get("/api/menus/all")
        .then()
                .statusCode(200)
                .body("name", hasItem("ZZ 묶음"))
                .body("name", hasItem("관리"));
    }

    @Test
    @DisplayName("메뉴마다 그 사용자의 권한이 함께 나온다")
    void includesPermissions() {
        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                // 관리자는 전권이라 모든 동작이 허용된다.
                .body("find { it.name == '표준 사전' }.children.find { it.path == '/words' }.canDelete", equalTo(true));

        authenticate(userToken);

        given()
        .when().get("/api/menus")
        .then()
                .statusCode(200)
                // 일반 사용자는 표준 단어를 조회만 할 수 있다.
                .body("find { it.name == '표준 사전' }.children.find { it.path == '/words' }.canRead", equalTo(true))
                .body("find { it.name == '표준 사전' }.children.find { it.path == '/words' }.canCreate", equalTo(false))
                // 테이블 정의는 등록·수정·삭제까지 할 수 있다.
                .body("find { it.name == '메타 관리' }.children.find { it.path == '/tables' }.canCreate", equalTo(true));
    }

    private static long createMenu(Long parentId, String name, String path, int sortOrder) {
        return given()
                .contentType(ContentType.JSON)
                .body(menuJson(parentId, name, path, sortOrder, true))
                .when()
                .post("/api/menus")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static String menuJson(Long parentId, String name, String path, int sortOrder, boolean used) {
        return """
                {"parentId": %s, "name": "%s", "path": %s, "icon": null, "sortOrder": %d, "used": %s}
                """.formatted(parentId, name, path == null ? "null" : "\"" + path + "\"", sortOrder, used);
    }
}
