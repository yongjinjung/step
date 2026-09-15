package com.step.dictionary.standard.domain;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("분류어 단어로 도메인을 등록하면 201과 분류어 정보를 반환한다")
    void create() {
        long wordId = createWord("번호", "NO", true);

        given()
                .contentType(ContentType.JSON)
                .body(domainJson("번호V20", wordId, "VARCHAR", 20, null))
        .when()
                .post("/api/domains")
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/domains/\\d+"))
                .body("classifierWordId", equalTo((int) wordId))
                .body("classifierWordName", equalTo("번호"))
                .body("dataType", equalTo("VARCHAR"))
                .body("dataLength", equalTo(20))
                .body("dataScale", nullValue());
    }

    @Test
    @DisplayName("DECIMAL 도메인의 소수점 자리수를 생략하면 0으로 저장된다")
    void createDecimal() {
        long wordId = createWord("금액", "AMT", true);

        given()
                .contentType(ContentType.JSON)
                .body(domainJson("금액N15", wordId, "DECIMAL", 15, null))
        .when()
                .post("/api/domains")
        .then()
                .statusCode(201)
                .body("dataScale", equalTo(0));
    }

    @Test
    @DisplayName("분류어가 아닌 단어로 도메인을 등록하면 400을 반환한다")
    void createWithNonClassifierWord() {
        long wordId = createWord("고객", "CUST", false);

        given()
                .contentType(ContentType.JSON)
                .body(domainJson("고객V20", wordId, "VARCHAR", 20, null))
        .when()
                .post("/api/domains")
        .then()
                .statusCode(400)
                .body("code", equalTo("NOT_CLASSIFIER_WORD"));
    }

    @Test
    @DisplayName("데이터 타입에 맞지 않는 길이면 400을 반환한다")
    void createInvalidSpec() {
        long wordId = createWord("일자", "DT", true);

        given()
                .contentType(ContentType.JSON)
                .body(domainJson("일자D8", wordId, "DATE", 8, null))
        .when()
                .post("/api/domains")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_DOMAIN_SPEC"));
    }

    @Test
    @DisplayName("없는 데이터 타입이면 400을 반환한다")
    void createUnknownDataType() {
        long wordId = createWord("번호", "NO", true);

        given()
                .contentType(ContentType.JSON)
                .body(domainJson("번호X20", wordId, "VARCHAR2", 20, null))
        .when()
                .post("/api/domains")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT"));
    }

    @Test
    @DisplayName("키워드, 분류어 단어, 데이터 타입으로 도메인을 검색한다")
    void search() {
        long numberId = createWord("번호", "NO", true);
        long nameId = createWord("명", "NM", true);
        createDomain("번호V20", numberId, "VARCHAR", 20);
        createDomain("번호N10", numberId, "DECIMAL", 10);
        createDomain("명V100", nameId, "VARCHAR", 100);

        given().queryParam("keyword", "v20")
        .when().get("/api/domains")
        .then()
                .statusCode(200)
                .body("content.name", equalTo(List.of("번호V20")));

        given().queryParam("classifierWordId", numberId)
        .when().get("/api/domains")
        .then()
                .statusCode(200)
                .body("content.name", equalTo(List.of("번호N10", "번호V20")))
                .body("content.classifierWordName", equalTo(List.of("번호", "번호")));

        given().queryParam("dataType", "VARCHAR").queryParam("page", 0).queryParam("size", 1)
        .when().get("/api/domains")
        .then()
                .statusCode(200)
                .body("content.name", equalTo(List.of("명V100")))
                .body("totalElements", equalTo(2));
    }

    @Test
    @DisplayName("도메인을 수정하면 변경된 값으로 조회된다")
    void update() {
        long wordId = createWord("번호", "NO", true);
        long id = createDomain("번호V20", wordId, "VARCHAR", 20);

        given()
                .contentType(ContentType.JSON)
                .body(domainJson("번호V30", wordId, "VARCHAR", 30, null))
        .when()
                .put("/api/domains/{id}", id)
        .then()
                .statusCode(200)
                .body("name", equalTo("번호V30"));

        given()
        .when().get("/api/domains/{id}", id)
        .then()
                .statusCode(200)
                .body("dataLength", equalTo(30));
    }

    @Test
    @DisplayName("도메인을 삭제하면 이후 조회 시 404를 반환한다")
    void delete() {
        long wordId = createWord("번호", "NO", true);
        long id = createDomain("번호V20", wordId, "VARCHAR", 20);

        given().when().delete("/api/domains/{id}", id).then().statusCode(204);

        given()
        .when().get("/api/domains/{id}", id)
        .then()
                .statusCode(404)
                .body("code", equalTo("DOMAIN_NOT_FOUND"));
    }

    @Test
    @DisplayName("도메인에서 사용 중인 단어를 삭제하면 409를 반환한다")
    void deleteWordInUse() {
        long wordId = createWord("번호", "NO", true);
        createDomain("번호V20", wordId, "VARCHAR", 20);

        given()
        .when().delete("/api/words/{id}", wordId)
        .then()
                .statusCode(409)
                .body("code", equalTo("WORD_IN_USE"));
    }

    @Test
    @DisplayName("도메인에서 사용 중인 단어의 분류어를 해제하면 409를 반환한다")
    void unsetClassifierInUse() {
        long wordId = createWord("번호", "NO", true);
        createDomain("번호V20", wordId, "VARCHAR", 20);

        given()
                .contentType(ContentType.JSON)
                .body(wordJson("번호", "NO", false))
        .when()
                .put("/api/words/{id}", wordId)
        .then()
                .statusCode(409)
                .body("code", equalTo("CLASSIFIER_WORD_IN_USE"));
    }

    private static long createWord(String name, String abbreviation, boolean classifier) {
        return given()
                .contentType(ContentType.JSON)
                .body(wordJson(name, abbreviation, classifier))
                .when()
                .post("/api/words")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static long createDomain(String name, long classifierWordId, String dataType, Integer dataLength) {
        return given()
                .contentType(ContentType.JSON)
                .body(domainJson(name, classifierWordId, dataType, dataLength, null))
                .when()
                .post("/api/domains")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static String wordJson(String name, String abbreviation, boolean classifier) {
        return """
                {"name": "%s", "abbreviation": "%s", "englishName": "%s", "description": null, "classifier": %s}
                """.formatted(name, abbreviation, abbreviation, classifier);
    }

    private static String domainJson(String name, long classifierWordId, String dataType,
            Integer dataLength, Integer dataScale) {
        return """
                {"name": "%s", "classifierWordId": %d, "dataType": "%s", "dataLength": %s, "dataScale": %s, "description": null}
                """.formatted(name, classifierWordId, dataType, dataLength, dataScale);
    }
}
