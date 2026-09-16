package com.step.dictionary.code;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 공통 코드 그룹·코드값 API. 컬럼 쪽 매핑 규칙은 {@code MetaColumnApiIntegrationTest}에서 확인한다. */
class CodeApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("코드 그룹을 등록하면 201과 Location을 반환하고 코드ID는 대문자로 저장된다")
    void createGroup() {
        given()
                .contentType(ContentType.JSON)
                .body(groupJson("order_stat", "주문상태", true))
        .when()
                .post("/api/code-groups")
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/code-groups/\\d+"))
                .body("codeId", equalTo("ORDER_STAT"))
                .body("codeName", equalTo("주문상태"))
                .body("used", equalTo(true));
    }

    @Test
    @DisplayName("코드ID가 대소문자만 다르게 중복되면 409, 형식이 틀리면 400을 반환한다")
    void createGroupInvalid() {
        createGroup("ORDER_STAT", "주문상태", true);

        given()
                .contentType(ContentType.JSON)
                .body(groupJson("order_stat", "주문 상태", true))
        .when()
                .post("/api/code-groups")
        .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_CODE_ID"));

        given()
                .contentType(ContentType.JSON)
                .body(groupJson("1ORDER", "", true))
        .when()
                .post("/api/code-groups")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT"))
                .body("fieldErrors", hasSize(2));
    }

    @Test
    @DisplayName("키워드와 사용 여부, 페이지로 코드 그룹을 검색한다")
    void searchGroups() {
        createGroup("PAY_TYPE", "결제수단", true);
        createGroup("ORDER_STAT", "주문상태", true);
        createGroup("OLD_TYPE", "옛 구분", false);

        given().queryParam("keyword", "결제")
        .when().get("/api/code-groups")
        .then()
                .statusCode(200)
                .body("content.codeId", equalTo(List.of("PAY_TYPE")));

        given().queryParam("used", true).queryParam("page", 0).queryParam("size", 1)
        .when().get("/api/code-groups")
        .then()
                .statusCode(200)
                .body("content.codeId", equalTo(List.of("ORDER_STAT")))
                .body("totalElements", equalTo(2));
    }

    @Test
    @DisplayName("코드 그룹을 수정하면 변경된 값으로 조회된다")
    void updateGroup() {
        long groupId = createGroup("ORDER_STAT", "주문상태", true);

        given()
                .contentType(ContentType.JSON)
                .body(groupJson("order_status", "주문 상태", false))
        .when()
                .put("/api/code-groups/{id}", groupId)
        .then()
                .statusCode(200)
                .body("codeId", equalTo("ORDER_STATUS"))
                .body("used", equalTo(false));

        given()
        .when().get("/api/code-groups/{id}", groupId)
        .then()
                .statusCode(200)
                .body("codeName", equalTo("주문 상태"));
    }

    @Test
    @DisplayName("코드값은 그룹 안에서만 유일하고 정렬 순서대로 조회된다")
    void values() {
        long orderId = createGroup("ORDER_STAT", "주문상태", true);
        long payId = createGroup("PAY_TYPE", "결제수단", true);
        createValue(orderId, "02", "결제완료", 2);

        given()
                .contentType(ContentType.JSON)
                .body(valueJson("a1", "접수", 1))
        .when()
                .post("/api/code-groups/{groupId}/values", orderId)
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/code-groups/\\d+/values/\\d+"))
                .body("codeValue", equalTo("A1"))
                .body("groupId", equalTo((int) orderId))
                .body("codeId", equalTo("ORDER_STAT"));

        given()
                .contentType(ContentType.JSON)
                .body(valueJson("02", "중복", 3))
        .when()
                .post("/api/code-groups/{groupId}/values", orderId)
        .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_CODE_VALUE"));

        // 다른 그룹에는 같은 코드값을 둘 수 있다.
        createValue(payId, "02", "계좌이체", 1);

        given()
        .when().get("/api/code-groups/{groupId}/values", orderId)
        .then()
                .statusCode(200)
                .body("codeValue", equalTo(List.of("A1", "02")))
                .body("valueName", equalTo(List.of("접수", "결제완료")));
    }

    @Test
    @DisplayName("코드값을 수정·삭제하고, 다른 그룹 경로로 접근하면 404를 반환한다")
    void updateAndDeleteValue() {
        long orderId = createGroup("ORDER_STAT", "주문상태", true);
        long payId = createGroup("PAY_TYPE", "결제수단", true);
        long valueId = createValue(orderId, "01", "접수", 1);

        given()
                .contentType(ContentType.JSON)
                .body(valueJson("01", "주문접수", 1))
        .when()
                .put("/api/code-groups/{groupId}/values/{valueId}", orderId, valueId)
        .then()
                .statusCode(200)
                .body("valueName", equalTo("주문접수"));

        given()
        .when().get("/api/code-groups/{groupId}/values/{valueId}", payId, valueId)
        .then()
                .statusCode(404)
                .body("code", equalTo("CODE_VALUE_NOT_FOUND"));

        given().when().delete("/api/code-groups/{groupId}/values/{valueId}", orderId, valueId).then().statusCode(204);

        given()
        .when().get("/api/code-groups/{groupId}/values", orderId)
        .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    @Test
    @DisplayName("코드 그룹을 삭제하면 코드값도 함께 삭제된다")
    void deleteGroupWithValues() {
        long groupId = createGroup("ORDER_STAT", "주문상태", true);
        createValue(groupId, "01", "접수", 1);
        createValue(groupId, "02", "결제완료", 2);

        given().when().delete("/api/code-groups/{id}", groupId).then().statusCode(204);

        given()
        .when().get("/api/code-groups/{groupId}/values", groupId)
        .then()
                .statusCode(404)
                .body("code", equalTo("CODE_GROUP_NOT_FOUND"));
    }

    @Test
    @DisplayName("컬럼에 매핑된 코드 그룹은 매핑 컬럼 목록에 테이블 순으로 나오고, 삭제하면 409를 반환한다")
    void mappedColumns() {
        long groupId = createGroup("ORDER_STAT", "주문상태", true);
        long orderTableId = createTable("TB_ORDER");
        long historyTableId = createTable("TB_ORDER_HIST");
        createColumn(historyTableId, "ORDER_STAT_CD", groupId);
        createColumn(orderTableId, "ORDER_STAT_CD", groupId);
        createColumn(orderTableId, "ETC_CD", null);

        given()
        .when().get("/api/code-groups/{id}/columns", groupId)
        .then()
                .statusCode(200)
                .body("tablePhysicalName", equalTo(List.of("TB_ORDER", "TB_ORDER_HIST")))
                .body("physicalName", equalTo(List.of("ORDER_STAT_CD", "ORDER_STAT_CD")))
                .body("tableId", equalTo(List.of((int) orderTableId, (int) historyTableId)));

        given()
        .when().delete("/api/code-groups/{id}", groupId)
        .then()
                .statusCode(409)
                .body("code", equalTo("CODE_GROUP_IN_USE"));
    }

    private static long createGroup(String codeId, String codeName, boolean used) {
        return post("/api/code-groups", groupJson(codeId, codeName, used));
    }

    private static long createValue(long groupId, String codeValue, String valueName, int sortOrder) {
        return post("/api/code-groups/" + groupId + "/values", valueJson(codeValue, valueName, sortOrder));
    }

    private static long createTable(String physicalName) {
        return post("/api/tables", """
                {"physicalName": "%s", "logicalName": "%s", "description": null}
                """.formatted(physicalName, physicalName));
    }

    private static void createColumn(long tableId, String physicalName, Long codeGroupId) {
        post("/api/tables/" + tableId + "/columns", """
                {"physicalName": "%s", "logicalName": "%s", "codeGroupId": %s, "dataType": "CHAR", "dataLength": 2,
                 "nullable": false, "primaryKey": false, "seq": 1}
                """.formatted(physicalName, physicalName, codeGroupId));
    }

    private static long post(String path, String body) {
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(path)
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static String groupJson(String codeId, String codeName, boolean used) {
        return """
                {"codeId": "%s", "codeName": "%s", "description": null, "used": %s}
                """.formatted(codeId, codeName, used);
    }

    private static String valueJson(String codeValue, String valueName, int sortOrder) {
        return """
                {"codeValue": "%s", "valueName": "%s", "description": null, "sortOrder": %d, "used": true}
                """.formatted(codeValue, valueName, sortOrder);
    }
}
