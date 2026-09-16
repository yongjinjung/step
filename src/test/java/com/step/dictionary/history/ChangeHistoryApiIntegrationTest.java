package com.step.dictionary.history;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChangeHistoryApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("단어를 등록·수정·삭제하면 최신순으로 이력이 쌓이고, 값이 그대로인 수정은 남지 않는다")
    void wordLifecycle() {
        long wordId = createWord("고객", "CUST", "Customer", false);
        putWord(wordId, "고객", "CUST", "Client");
        putWord(wordId, "고객", "CUST", "Client");
        given().when().delete("/api/words/{id}", wordId).then().statusCode(204);

        given().queryParam("targetType", "WORD").queryParam("targetId", wordId)
        .when().get("/api/histories")
        .then()
                .statusCode(200)
                .body("content.changeType", equalTo(List.of("DELETE", "UPDATE", "CREATE")))
                .body("content.changedByUsername", equalTo(List.of(ADMIN_USERNAME, ADMIN_USERNAME, ADMIN_USERNAME)))
                .body("content[0].after", nullValue())
                .body("content[0].before.englishName", equalTo("Client"))
                .body("content[1].changedFields", equalTo(List.of("englishName")))
                .body("content[1].before.englishName", equalTo("Customer"))
                .body("content[1].after.englishName", equalTo("Client"))
                .body("content[2].before", nullValue())
                .body("content[2].after.abbreviation", equalTo("CUST"))
                .body("content[2].requestId", nullValue());
    }

    @Test
    @DisplayName("신청 승인으로 반영된 변경은 승인한 관리자와 신청 ID가 이력에 남는다")
    void approvedRequest() {
        authenticate(userToken);
        long requestId = given().contentType(ContentType.JSON)
                .body("""
                        {"requestType": "CREATE",
                         "payload": {"name": "고객", "abbreviation": "CUST", "englishName": "Customer", "classifier": false}}
                        """)
        .when().post("/api/requests/words")
        .then().statusCode(201)
        .extract().jsonPath().getLong("id");

        authenticate(adminToken);
        long wordId = given().contentType(ContentType.JSON)
        .when().post("/api/requests/{id}/approve", requestId)
        .then().statusCode(200)
        .extract().jsonPath().getLong("targetId");

        given().queryParam("requestId", requestId)
        .when().get("/api/histories")
        .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("content[0].targetType", equalTo("WORD"))
                .body("content[0].targetId", equalTo((int) wordId))
                .body("content[0].changedByUsername", equalTo(ADMIN_USERNAME))
                .body("content[0].requestId", equalTo((int) requestId));
    }

    @Test
    @DisplayName("업무 규칙 위반으로 실패한 변경은 이력이 남지 않는다")
    void failedChangeLeavesNoHistory() {
        createWord("고객", "CUST", "Customer", false);

        given().contentType(ContentType.JSON).body(wordJson("고객", "CST", "Client"))
        .when().post("/api/words")
        .then().statusCode(409);

        given().queryParam("targetType", "WORD")
        .when().get("/api/histories")
        .then().body("totalElements", equalTo(1));
    }

    @Test
    @DisplayName("일반 사용자가 테이블을 삭제하면 함께 지워진 컬럼마다 삭제 이력이 남는다")
    void tableDeleteRecordsColumns() {
        authenticate(userToken);
        long tableId = given().contentType(ContentType.JSON)
                .body("""
                        {"physicalName": "TB_CUSTOMER", "logicalName": "고객", "description": null}
                        """)
        .when().post("/api/tables")
        .then().statusCode(201)
        .extract().jsonPath().getLong("id");
        createColumn(tableId, "CUST_NO", 1);
        createColumn(tableId, "CUST_NM", 2);

        given().when().delete("/api/tables/{id}", tableId).then().statusCode(204);

        given().queryParam("changeType", "DELETE")
        .when().get("/api/histories")
        .then()
                .statusCode(200)
                .body("content.targetType", equalTo(List.of("TABLE", "COLUMN", "COLUMN")))
                .body("content.changedByUsername", equalTo(List.of(USER_USERNAME, USER_USERNAME, USER_USERNAME)))
                .body("content[1].before.physicalName", equalTo("CUST_NM"))
                .body("content[1].before.tablePhysicalName", equalTo("TB_CUSTOMER"));
    }

    @Test
    @DisplayName("용어 이력에는 구성 단어 ID와 도메인이 남는다")
    void termSnapshot() {
        long customerId = createWord("고객", "CUST", "Customer", false);
        long numberId = createWord("번호", "NO", "Number", true);
        long domainId = given().contentType(ContentType.JSON)
                .body("""
                        {"name": "번호V20", "classifierWordId": %d, "dataType": "VARCHAR", "dataLength": 20}
                        """.formatted(numberId))
        .when().post("/api/domains")
        .then().statusCode(201)
        .extract().jsonPath().getLong("id");
        given().contentType(ContentType.JSON)
                .body("""
                        {"wordIds": [%d, %d], "domainId": %d}
                        """.formatted(customerId, numberId, domainId))
        .when().post("/api/terms")
        .then().statusCode(201);

        given().queryParam("targetType", "TERM")
        .when().get("/api/histories")
        .then()
                .statusCode(200)
                .body("content[0].after.abbreviation", equalTo("CUST_NO"))
                .body("content[0].after.wordIds", equalTo(List.of((int) customerId, (int) numberId)))
                .body("content[0].after.domainName", equalTo("번호V20"));
    }

    @Test
    @DisplayName("일반 사용자도 이력을 조회할 수 있고, 기간 조건과 없는 이력 조회를 처리한다")
    void readAsUser() {
        createWord("고객", "CUST", "Customer", false);
        authenticate(userToken);
        LocalDate today = LocalDate.now();

        given().queryParam("from", today.toString()).queryParam("to", today.toString())
        .when().get("/api/histories")
        .then()
                .statusCode(200)
                .body("totalElements", equalTo(1));
        given().queryParam("from", today.plusDays(1).toString())
        .when().get("/api/histories")
        .then().body("totalElements", equalTo(0));

        given().when().get("/api/histories/{id}", 999_999)
        .then()
                .statusCode(404)
                .body("code", equalTo("HISTORY_NOT_FOUND"));
    }

    @Test
    @DisplayName("코드 그룹을 삭제하면 함께 지워진 코드값마다 삭제 이력이 남는다")
    void codeGroupDeleteRecordsValues() {
        long groupId = given().contentType(ContentType.JSON)
                .body("""
                        {"codeId": "ORDER_STAT", "codeName": "주문상태", "used": true}
                        """)
        .when().post("/api/code-groups")
        .then().statusCode(201)
        .extract().jsonPath().getLong("id");
        createCodeValue(groupId, "01", 1);
        createCodeValue(groupId, "02", 2);

        given().when().delete("/api/code-groups/{id}", groupId).then().statusCode(204);

        given().queryParam("changeType", "DELETE")
        .when().get("/api/histories")
        .then()
                .statusCode(200)
                .body("content.targetType", equalTo(List.of("CODE_GROUP", "CODE_VALUE", "CODE_VALUE")))
                .body("content[0].before.codeName", equalTo("주문상태"))
                .body("content[1].before.codeValue", equalTo("02"))
                .body("content[1].before.codeId", equalTo("ORDER_STAT"));
    }

    private static long createWord(String name, String abbreviation, String englishName, boolean classifier) {
        return given().contentType(ContentType.JSON)
                .body("""
                        {"name": "%s", "abbreviation": "%s", "englishName": "%s", "description": null, "classifier": %s}
                        """.formatted(name, abbreviation, englishName, classifier))
                .when().post("/api/words")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static void putWord(long id, String name, String abbreviation, String englishName) {
        given().contentType(ContentType.JSON).body(wordJson(name, abbreviation, englishName))
                .when().put("/api/words/{id}", id)
                .then().statusCode(200);
    }

    private static String wordJson(String name, String abbreviation, String englishName) {
        return """
                {"name": "%s", "abbreviation": "%s", "englishName": "%s", "description": null, "classifier": false}
                """.formatted(name, abbreviation, englishName);
    }

    private static void createColumn(long tableId, String physicalName, int seq) {
        given().contentType(ContentType.JSON)
                .body("""
                        {"physicalName": "%s", "logicalName": "컬럼", "termId": null, "dataType": "VARCHAR",
                         "dataLength": 20, "dataScale": null, "nullable": false, "primaryKey": false, "seq": %d}
                        """.formatted(physicalName, seq))
                .when().post("/api/tables/{tableId}/columns", tableId)
                .then().statusCode(201);
    }

    private static void createCodeValue(long groupId, String codeValue, int sortOrder) {
        given().contentType(ContentType.JSON)
                .body("""
                        {"codeValue": "%s", "valueName": "값%s", "sortOrder": %d, "used": true}
                        """.formatted(codeValue, codeValue, sortOrder))
                .when().post("/api/code-groups/{groupId}/values", groupId)
                .then().statusCode(201);
    }
}
