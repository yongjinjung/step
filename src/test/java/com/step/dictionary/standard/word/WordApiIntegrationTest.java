package com.step.dictionary.standard.word;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WordApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("단어를 등록하면 201과 Location을 반환하고 영문약어는 대문자로 저장된다")
    void create() {
        given()
                .contentType(ContentType.JSON)
                .body(wordJson("고객", "cust", "Customer", false))
        .when()
                .post("/api/words")
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/words/\\d+"))
                .body("name", equalTo("고객"))
                .body("abbreviation", equalTo("CUST"))
                .body("classifier", equalTo(false));
    }

    @Test
    @DisplayName("영문약어가 중복되면 409를 반환한다")
    void createDuplicateAbbreviation() {
        createWord("고객", "CUST", "Customer", false);

        given()
                .contentType(ContentType.JSON)
                .body(wordJson("손님", "cust", "Guest", false))
        .when()
                .post("/api/words")
        .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_WORD_ABBREVIATION"));
    }

    @Test
    @DisplayName("입력값이 올바르지 않으면 400과 필드 오류를 반환한다")
    void createInvalid() {
        given()
                .contentType(ContentType.JSON)
                .body(wordJson("", "1abc", "Customer", false))
        .when()
                .post("/api/words")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT"))
                .body("fieldErrors", hasSize(2));
    }

    @Test
    @DisplayName("키워드, 분류어 여부, 페이지로 단어를 검색한다")
    void search() {
        createWord("고객", "CUST", "Customer", false);
        createWord("번호", "NO", "Number", true);
        createWord("일자", "DT", "Date", true);

        given().queryParam("keyword", "cust")
        .when().get("/api/words")
        .then()
                .statusCode(200)
                .body("content.name", equalTo(java.util.List.of("고객")));

        given().queryParam("classifier", true).queryParam("page", 0).queryParam("size", 1)
        .when().get("/api/words")
        .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].name", equalTo("번호"))
                .body("totalElements", equalTo(2))
                .body("totalPages", equalTo(2));
    }

    @Test
    @DisplayName("단어를 수정하면 변경된 값으로 조회된다")
    void update() {
        long id = createWord("고객", "CUST", "Customer", false);

        given()
                .contentType(ContentType.JSON)
                .body(wordJson("고객", "cst", "Client", false))
        .when()
                .put("/api/words/{id}", id)
        .then()
                .statusCode(200)
                .body("abbreviation", equalTo("CST"));

        given()
        .when().get("/api/words/{id}", id)
        .then()
                .statusCode(200)
                .body("englishName", equalTo("Client"));
    }

    @Test
    @DisplayName("단어를 삭제하면 이후 조회 시 404를 반환한다")
    void delete() {
        long id = createWord("고객", "CUST", "Customer", false);

        given().when().delete("/api/words/{id}", id).then().statusCode(204);

        given()
        .when().get("/api/words/{id}", id)
        .then()
                .statusCode(404)
                .body("code", equalTo("WORD_NOT_FOUND"));
    }

    private static long createWord(String name, String abbreviation, String englishName, boolean classifier) {
        return given()
                .contentType(ContentType.JSON)
                .body(wordJson(name, abbreviation, englishName, classifier))
                .when()
                .post("/api/words")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static String wordJson(String name, String abbreviation, String englishName, boolean classifier) {
        return """
                {"name": "%s", "abbreviation": "%s", "englishName": "%s", "description": null, "classifier": %s}
                """.formatted(name, abbreviation, englishName, classifier);
    }
}
