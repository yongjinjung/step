package com.step.dictionary.meta.column;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MetaColumnApiIntegrationTest extends IntegrationTest {

    private long customerId;
    private long numberId;
    private long numberDomainId;
    private long termId;
    private long tableId;

    /** 고객번호 / CUST_NO / 번호V20(VARCHAR 20) 용어와 TB_CUSTOMER 테이블을 준비한다. */
    @BeforeEach
    void setUp() {
        customerId = createWord("고객", "CUST", false);
        numberId = createWord("번호", "NO", true);
        numberDomainId = post("/api/domains", domainJson("번호V20", 20));
        termId = post("/api/terms", termJson(customerId, numberId));
        tableId = createTable("TB_CUSTOMER");
    }

    @Test
    @DisplayName("용어 규칙대로 컬럼을 등록하면 표준 컬럼으로 판정한다")
    void createStandard() {
        given()
                .contentType(ContentType.JSON)
                .body(columnJson("cust_no", termId, "VARCHAR", 20, false, true, 1))
        .when()
                .post("/api/tables/{tableId}/columns", tableId)
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/tables/\\d+/columns/\\d+"))
                .body("tablePhysicalName", equalTo("TB_CUSTOMER"))
                .body("physicalName", equalTo("CUST_NO"))
                .body("termName", equalTo("고객번호"))
                .body("expected.physicalName", equalTo("CUST_NO"))
                .body("expected.dataLength", equalTo(20))
                .body("standard", equalTo(true))
                .body("issues", hasSize(0));
    }

    @Test
    @DisplayName("용어 규칙과 다른 컬럼은 사유와 함께 비표준으로 판정한다")
    void createNonStandard() {
        given()
                .contentType(ContentType.JSON)
                .body(columnJson("CUSTOMER_NUMBER", termId, "CHAR", 10, false, false, 1))
        .when()
                .post("/api/tables/{tableId}/columns", tableId)
        .then()
                .statusCode(201)
                .body("standard", equalTo(false))
                .body("issues", equalTo(List.of("NAME_MISMATCH", "TYPE_MISMATCH", "LENGTH_MISMATCH")));

        given()
                .contentType(ContentType.JSON)
                .body(columnJson("ETC_TXT", null, "VARCHAR", 100, true, false, 2))
        .when()
                .post("/api/tables/{tableId}/columns", tableId)
        .then()
                .statusCode(201)
                .body("issues", equalTo(List.of("NO_TERM")))
                .body("expected", nullValue());
    }

    @Test
    @DisplayName("기본키 컬럼이 NULL을 허용하면 400을 반환한다")
    void createPrimaryKeyNullable() {
        given()
                .contentType(ContentType.JSON)
                .body(columnJson("CUST_NO", termId, "VARCHAR", 20, true, true, 1))
        .when()
                .post("/api/tables/{tableId}/columns", tableId)
        .then()
                .statusCode(400)
                .body("code", equalTo("PRIMARY_KEY_NULLABLE"));
    }

    @Test
    @DisplayName("같은 테이블에 물리명이 중복되면 409를 반환한다")
    void createDuplicate() {
        createColumn(tableId, "CUST_NO", termId, "VARCHAR", 20, 1);

        given()
                .contentType(ContentType.JSON)
                .body(columnJson("cust_no", null, "VARCHAR", 20, true, false, 2))
        .when()
                .post("/api/tables/{tableId}/columns", tableId)
        .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_COLUMN_NAME"));
    }

    @Test
    @DisplayName("테이블 컬럼 목록은 순서대로 검출 결과와 함께 조회된다")
    void findAll() {
        createColumn(tableId, "ETC_TXT", null, "VARCHAR", 100, 2);
        createColumn(tableId, "CUST_NO", termId, "VARCHAR", 20, 1);

        given()
        .when().get("/api/tables/{tableId}/columns", tableId)
        .then()
                .statusCode(200)
                .body("physicalName", equalTo(List.of("CUST_NO", "ETC_TXT")))
                .body("standard", equalTo(List.of(true, false)));
    }

    @Test
    @DisplayName("다른 테이블 경로로 컬럼을 조회하면 404를 반환한다")
    void findColumnOfOtherTable() {
        long columnId = createColumn(tableId, "CUST_NO", termId, "VARCHAR", 20, 1);
        long otherTableId = createTable("TB_ORDER");

        given()
        .when().get("/api/tables/{tableId}/columns/{columnId}", otherTableId, columnId)
        .then()
                .statusCode(404)
                .body("code", equalTo("COLUMN_NOT_FOUND"));
    }

    @Test
    @DisplayName("전체 비표준 컬럼을 사유와 테이블로 검색한다")
    void searchNonStandard() {
        long orderTableId = createTable("TB_ORDER");
        createColumn(tableId, "CUST_NO", termId, "VARCHAR", 20, 1);
        createColumn(tableId, "ETC_TXT", null, "VARCHAR", 100, 2);
        createColumn(orderTableId, "CUSTOMER_NO", termId, "VARCHAR", 20, 1);

        given()
        .when().get("/api/columns/non-standard")
        .then()
                .statusCode(200)
                .body("content.physicalName", equalTo(List.of("ETC_TXT", "CUSTOMER_NO")))
                .body("content.tablePhysicalName", equalTo(List.of("TB_CUSTOMER", "TB_ORDER")))
                .body("totalElements", equalTo(2));

        given().queryParam("issue", "NAME_MISMATCH")
        .when().get("/api/columns/non-standard")
        .then()
                .statusCode(200)
                .body("content.physicalName", equalTo(List.of("CUSTOMER_NO")));

        given().queryParam("tableId", tableId)
        .when().get("/api/columns/non-standard")
        .then()
                .statusCode(200)
                .body("content.physicalName", equalTo(List.of("ETC_TXT")));
    }

    @Test
    @DisplayName("매핑된 용어의 구성이나 도메인 길이가 바뀌면 컬럼이 비표준으로 검출된다")
    void detectsStandardChange() {
        long columnId = createColumn(tableId, "CUST_NO", termId, "VARCHAR", 20, 1);
        long accountId = createWord("계좌", "ACCT", false);

        given()
                .contentType(ContentType.JSON)
                .body(domainJson("번호V30", 30))
        .when()
                .put("/api/domains/{id}", numberDomainId)
        .then()
                .statusCode(200);

        given()
        .when().get("/api/tables/{tableId}/columns/{columnId}", tableId, columnId)
        .then()
                .statusCode(200)
                .body("issues", equalTo(List.of("LENGTH_MISMATCH")));

        given()
                .contentType(ContentType.JSON)
                .body(termJson(customerId, accountId, numberId))
        .when()
                .put("/api/terms/{id}", termId)
        .then()
                .statusCode(200);

        given()
        .when().get("/api/tables/{tableId}/columns/{columnId}", tableId, columnId)
        .then()
                .statusCode(200)
                .body("expected.physicalName", equalTo("CUST_ACCT_NO"))
                .body("issues", equalTo(List.of("NAME_MISMATCH", "LENGTH_MISMATCH")));
    }

    @Test
    @DisplayName("비표준 컬럼을 표준 규칙대로 수정하면 표준으로 판정한다")
    void updateToStandard() {
        long columnId = createColumn(tableId, "CUSTOMER_NO", null, "CHAR", 10, 1);

        given()
                .contentType(ContentType.JSON)
                .body(columnJson("CUST_NO", termId, "VARCHAR", 20, false, true, 1))
        .when()
                .put("/api/tables/{tableId}/columns/{columnId}", tableId, columnId)
        .then()
                .statusCode(200)
                .body("standard", equalTo(true));
    }

    @Test
    @DisplayName("컬럼을 삭제하면 이후 조회 시 404를 반환한다")
    void delete() {
        long columnId = createColumn(tableId, "CUST_NO", termId, "VARCHAR", 20, 1);

        given().when().delete("/api/tables/{tableId}/columns/{columnId}", tableId, columnId).then().statusCode(204);

        given()
        .when().get("/api/tables/{tableId}/columns/{columnId}", tableId, columnId)
        .then()
                .statusCode(404)
                .body("code", equalTo("COLUMN_NOT_FOUND"));
    }

    @Test
    @DisplayName("컬럼에 매핑된 용어를 삭제하면 409를 반환한다")
    void deleteTermInUse() {
        createColumn(tableId, "CUST_NO", termId, "VARCHAR", 20, 1);

        given()
        .when().delete("/api/terms/{id}", termId)
        .then()
                .statusCode(409)
                .body("code", equalTo("TERM_IN_USE"));
    }

    private static long createWord(String name, String abbreviation, boolean classifier) {
        return post("/api/words", """
                {"name": "%s", "abbreviation": "%s", "englishName": "%s", "description": null, "classifier": %s}
                """.formatted(name, abbreviation, abbreviation, classifier));
    }

    private static long createTable(String physicalName) {
        return post("/api/tables", """
                {"physicalName": "%s", "logicalName": "%s", "description": null}
                """.formatted(physicalName, physicalName));
    }

    private static long createColumn(long tableId, String physicalName, Long termId, String dataType,
            int dataLength, int seq) {
        return post("/api/tables/" + tableId + "/columns",
                columnJson(physicalName, termId, dataType, dataLength, false, false, seq));
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

    private String domainJson(String name, int dataLength) {
        return """
                {"name": "%s", "classifierWordId": %d, "dataType": "VARCHAR", "dataLength": %d, "dataScale": null, "description": null}
                """.formatted(name, numberId, dataLength);
    }

    private String termJson(long... wordIds) {
        String ids = java.util.Arrays.stream(wordIds)
                .mapToObj(String::valueOf)
                .collect(java.util.stream.Collectors.joining(", "));
        return """
                {"wordIds": [%s], "domainId": %d, "description": null}
                """.formatted(ids, numberDomainId);
    }

    private static String columnJson(String physicalName, Long termId, String dataType, int dataLength,
            boolean nullable, boolean primaryKey, int seq) {
        return """
                {"physicalName": "%s", "logicalName": "%s", "termId": %s, "dataType": "%s", "dataLength": %d,
                 "dataScale": null, "nullable": %s, "primaryKey": %s, "seq": %d}
                """.formatted(physicalName, physicalName, termId, dataType, dataLength, nullable, primaryKey, seq);
    }
}
