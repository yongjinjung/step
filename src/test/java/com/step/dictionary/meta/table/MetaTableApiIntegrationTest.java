package com.step.dictionary.meta.table;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MetaTableApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("테이블을 등록하면 201과 대문자 물리명을 반환한다")
    void create() {
        given()
                .contentType(ContentType.JSON)
                .body(tableJson("tb_customer", "고객"))
        .when()
                .post("/api/tables")
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/tables/\\d+"))
                .body("physicalName", equalTo("TB_CUSTOMER"))
                .body("logicalName", equalTo("고객"));
    }

    @Test
    @DisplayName("물리명이 대소문자만 다르면 중복으로 보고 409를 반환한다")
    void createDuplicate() {
        createTable("TB_CUSTOMER", "고객");

        given()
                .contentType(ContentType.JSON)
                .body(tableJson("tb_customer", "고객2"))
        .when()
                .post("/api/tables")
        .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_TABLE_NAME"));
    }

    @Test
    @DisplayName("물리명 형식이 올바르지 않으면 400을 반환한다")
    void createInvalidName() {
        given()
                .contentType(ContentType.JSON)
                .body(tableJson("1TB-CUSTOMER", "고객"))
        .when()
                .post("/api/tables")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT"));
    }

    @Test
    @DisplayName("물리명 또는 논리명으로 테이블을 검색한다")
    void search() {
        createTable("TB_CUSTOMER", "고객");
        createTable("TB_ORDER", "주문");

        given().queryParam("keyword", "order")
        .when().get("/api/tables")
        .then()
                .statusCode(200)
                .body("content.physicalName", equalTo(List.of("TB_ORDER")));

        given().queryParam("keyword", "고객")
        .when().get("/api/tables")
        .then()
                .statusCode(200)
                .body("content.physicalName", equalTo(List.of("TB_CUSTOMER")));
    }

    @Test
    @DisplayName("테이블을 수정하면 변경된 값으로 조회된다")
    void update() {
        long id = createTable("TB_CUSTOMER", "고객");

        given()
                .contentType(ContentType.JSON)
                .body(tableJson("tb_cust", "고객 기본"))
        .when()
                .put("/api/tables/{id}", id)
        .then()
                .statusCode(200)
                .body("physicalName", equalTo("TB_CUST"));

        given()
        .when().get("/api/tables/{id}", id)
        .then()
                .statusCode(200)
                .body("logicalName", equalTo("고객 기본"));
    }

    @Test
    @DisplayName("컬럼이 있는 테이블을 삭제하면 컬럼도 함께 삭제된다")
    void deleteWithColumns() {
        long id = createTable("TB_CUSTOMER", "고객");
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"physicalName": "ETC_TXT", "logicalName": "기타", "termId": null, "dataType": "VARCHAR",
                         "dataLength": 100, "dataScale": null, "nullable": true, "primaryKey": false, "seq": 1}
                        """)
        .when()
                .post("/api/tables/{id}/columns", id)
        .then()
                .statusCode(201);

        given().when().delete("/api/tables/{id}", id).then().statusCode(204);

        given()
        .when().get("/api/tables/{id}/columns", id)
        .then()
                .statusCode(404)
                .body("code", equalTo("TABLE_NOT_FOUND"));
        given()
        .when().get("/api/columns/non-standard")
        .then()
                .statusCode(200)
                .body("totalElements", equalTo(0));
    }

    private static long createTable(String physicalName, String logicalName) {
        return given()
                .contentType(ContentType.JSON)
                .body(tableJson(physicalName, logicalName))
                .when()
                .post("/api/tables")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static String tableJson(String physicalName, String logicalName) {
        return """
                {"physicalName": "%s", "logicalName": "%s", "description": null}
                """.formatted(physicalName, logicalName);
    }
}
