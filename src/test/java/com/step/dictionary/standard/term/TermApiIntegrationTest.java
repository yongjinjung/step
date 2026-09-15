package com.step.dictionary.standard.term;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TermApiIntegrationTest extends IntegrationTest {

    private long customerId;
    private long accountId;
    private long numberId;
    private long numberDomainId;

    @BeforeEach
    void setUp() {
        customerId = createWord("고객", "CUST", false);
        accountId = createWord("계좌", "ACCT", false);
        numberId = createWord("번호", "NO", true);
        numberDomainId = createDomain("번호V20", numberId, 20);
    }

    @Test
    @DisplayName("용어를 등록하면 201과 생성된 용어명, 영문약어, 구성 단어, 도메인 규칙을 반환한다")
    void create() {
        given()
                .contentType(ContentType.JSON)
                .body(termJson(numberDomainId, customerId, numberId))
        .when()
                .post("/api/terms")
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/terms/\\d+"))
                .body("name", equalTo("고객번호"))
                .body("abbreviation", equalTo("CUST_NO"))
                .body("words.name", equalTo(List.of("고객", "번호")))
                .body("domainName", equalTo("번호V20"))
                .body("dataType", equalTo("VARCHAR"))
                .body("dataLength", equalTo(20));
    }

    @Test
    @DisplayName("마지막 단어가 분류어가 아니면 400을 반환한다")
    void createLastWordNotClassifier() {
        given()
                .contentType(ContentType.JSON)
                .body(termJson(numberDomainId, numberId, customerId))
        .when()
                .post("/api/terms")
        .then()
                .statusCode(400)
                .body("code", equalTo("TERM_LAST_WORD_NOT_CLASSIFIER"));
    }

    @Test
    @DisplayName("마지막 단어와 도메인의 분류어가 다르면 400을 반환한다")
    void createDomainMismatch() {
        long nameId = createWord("명", "NM", true);
        long nameDomainId = createDomain("명V100", nameId, 100);

        given()
                .contentType(ContentType.JSON)
                .body(termJson(nameDomainId, customerId, numberId))
        .when()
                .post("/api/terms")
        .then()
                .statusCode(400)
                .body("code", equalTo("TERM_DOMAIN_MISMATCH"));
    }

    @Test
    @DisplayName("같은 단어 조합의 용어를 다시 등록하면 409를 반환한다")
    void createDuplicate() {
        createTerm(numberDomainId, customerId, numberId);

        given()
                .contentType(ContentType.JSON)
                .body(termJson(numberDomainId, customerId, numberId))
        .when()
                .post("/api/terms")
        .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_TERM_NAME"));
    }

    @Test
    @DisplayName("구성 단어가 없으면 400을 반환한다")
    void createWithoutWords() {
        given()
                .contentType(ContentType.JSON)
                .body(termJson(numberDomainId))
        .when()
                .post("/api/terms")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT"));
    }

    @Test
    @DisplayName("키워드, 구성 단어, 도메인으로 용어를 검색한다")
    void search() {
        createTerm(numberDomainId, customerId, numberId);
        createTerm(numberDomainId, accountId, numberId);
        createTerm(numberDomainId, customerId, accountId, numberId);

        given().queryParam("keyword", "acct")
        .when().get("/api/terms")
        .then()
                .statusCode(200)
                .body("content.name", equalTo(List.of("계좌번호", "고객계좌번호")));

        given().queryParam("wordId", customerId)
        .when().get("/api/terms")
        .then()
                .statusCode(200)
                .body("content.name", equalTo(List.of("고객계좌번호", "고객번호")))
                .body("content[0].words.abbreviation", equalTo(List.of("CUST", "ACCT", "NO")));

        given().queryParam("domainId", numberDomainId).queryParam("page", 0).queryParam("size", 1)
        .when().get("/api/terms")
        .then()
                .statusCode(200)
                .body("content.name", equalTo(List.of("계좌번호")))
                .body("totalElements", equalTo(3));
    }

    @Test
    @DisplayName("구성 단어를 바꾸면 용어명과 영문약어가 다시 생성된다")
    void update() {
        long id = createTerm(numberDomainId, customerId, numberId);

        given()
                .contentType(ContentType.JSON)
                .body(termJson(numberDomainId, customerId, accountId, numberId))
        .when()
                .put("/api/terms/{id}", id)
        .then()
                .statusCode(200)
                .body("name", equalTo("고객계좌번호"))
                .body("abbreviation", equalTo("CUST_ACCT_NO"));

        given()
                .contentType(ContentType.JSON)
                .body(termJson(numberDomainId, accountId, numberId))
        .when()
                .put("/api/terms/{id}", id)
        .then()
                .statusCode(200);

        given()
        .when().get("/api/terms/{id}", id)
        .then()
                .statusCode(200)
                .body("abbreviation", equalTo("ACCT_NO"))
                .body("words.name", equalTo(List.of("계좌", "번호")));
    }

    @Test
    @DisplayName("용어를 삭제하면 이후 조회 시 404를 반환한다")
    void delete() {
        long id = createTerm(numberDomainId, customerId, numberId);

        given().when().delete("/api/terms/{id}", id).then().statusCode(204);

        given()
        .when().get("/api/terms/{id}", id)
        .then()
                .statusCode(404)
                .body("code", equalTo("TERM_NOT_FOUND"));
    }

    @Test
    @DisplayName("용어에서 사용 중인 단어를 삭제하면 409를 반환한다")
    void deleteWordInUse() {
        createTerm(numberDomainId, customerId, numberId);

        given()
        .when().delete("/api/words/{id}", customerId)
        .then()
                .statusCode(409)
                .body("code", equalTo("WORD_IN_USE"));
    }

    @Test
    @DisplayName("용어에서 사용 중인 단어는 단어명을 바꿀 수 없지만 영문명은 바꿀 수 있다")
    void renameWordInUse() {
        createTerm(numberDomainId, customerId, numberId);

        given()
                .contentType(ContentType.JSON)
                .body(wordJson("손님", "CUST", "Customer", false))
        .when()
                .put("/api/words/{id}", customerId)
        .then()
                .statusCode(409)
                .body("code", equalTo("WORD_USED_BY_TERM"));

        given()
                .contentType(ContentType.JSON)
                .body(wordJson("고객", "CUST", "Client", false))
        .when()
                .put("/api/words/{id}", customerId)
        .then()
                .statusCode(200)
                .body("englishName", equalTo("Client"));
    }

    @Test
    @DisplayName("용어에서 사용 중인 도메인을 삭제하면 409를 반환한다")
    void deleteDomainInUse() {
        createTerm(numberDomainId, customerId, numberId);

        given()
        .when().delete("/api/domains/{id}", numberDomainId)
        .then()
                .statusCode(409)
                .body("code", equalTo("DOMAIN_IN_USE"));
    }

    @Test
    @DisplayName("용어에서 사용 중인 도메인은 분류어를 바꿀 수 없지만 길이는 바꿀 수 있고 용어에 반영된다")
    void changeDomainInUse() {
        long termId = createTerm(numberDomainId, customerId, numberId);
        long codeId = createWord("코드", "CD", true);

        given()
                .contentType(ContentType.JSON)
                .body(domainJson("번호V20", codeId, 20))
        .when()
                .put("/api/domains/{id}", numberDomainId)
        .then()
                .statusCode(409)
                .body("code", equalTo("DOMAIN_CLASSIFIER_IN_USE"));

        given()
                .contentType(ContentType.JSON)
                .body(domainJson("번호V30", numberId, 30))
        .when()
                .put("/api/domains/{id}", numberDomainId)
        .then()
                .statusCode(200);

        given()
        .when().get("/api/terms/{id}", termId)
        .then()
                .statusCode(200)
                .body("domainName", equalTo("번호V30"))
                .body("dataLength", equalTo(30));
    }

    private static long createWord(String name, String abbreviation, boolean classifier) {
        return given()
                .contentType(ContentType.JSON)
                .body(wordJson(name, abbreviation, abbreviation, classifier))
                .when()
                .post("/api/words")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static long createDomain(String name, long classifierWordId, int dataLength) {
        return given()
                .contentType(ContentType.JSON)
                .body(domainJson(name, classifierWordId, dataLength))
                .when()
                .post("/api/domains")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static long createTerm(long domainId, long... wordIds) {
        return given()
                .contentType(ContentType.JSON)
                .body(termJson(domainId, wordIds))
                .when()
                .post("/api/terms")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static String wordJson(String name, String abbreviation, String englishName, boolean classifier) {
        return """
                {"name": "%s", "abbreviation": "%s", "englishName": "%s", "description": null, "classifier": %s}
                """.formatted(name, abbreviation, englishName, classifier);
    }

    private static String domainJson(String name, long classifierWordId, int dataLength) {
        return """
                {"name": "%s", "classifierWordId": %d, "dataType": "VARCHAR", "dataLength": %d, "dataScale": null, "description": null}
                """.formatted(name, classifierWordId, dataLength);
    }

    private static String termJson(long domainId, long... wordIds) {
        String ids = Arrays.stream(wordIds).mapToObj(String::valueOf).collect(Collectors.joining(", "));
        return """
                {"wordIds": [%s], "domainId": %d, "description": null}
                """.formatted(ids, domainId);
    }
}
