package com.step.dictionary.request;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChangeRequestApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("일반 사용자가 단어 등록을 신청하면 대기 상태이고, 관리자가 승인해야 단어가 생긴다")
    void createAndApprove() {
        authenticate(userToken);
        long requestId = requestWord("CREATE", null, wordPayload("고객", "cust", "Customer", false))
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/requests/\\d+"))
                .body("status", equalTo("REQUESTED"))
                .body("requesterUsername", equalTo(USER_USERNAME))
                .body("payload.abbreviation", equalTo("cust"))
                .body("targetId", nullValue())
                .extract().jsonPath().getLong("id");

        given().when().get("/api/words").then().statusCode(200).body("totalElements", equalTo(0));

        authenticate(adminToken);
        long wordId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"comment": "확인했습니다"}
                        """)
        .when().post("/api/requests/{id}/approve", requestId)
        .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"))
                .body("reviewerUsername", equalTo(ADMIN_USERNAME))
                .body("reviewComment", equalTo("확인했습니다"))
                .body("reviewedAt", notNullValue())
                .extract().jsonPath().getLong("targetId");

        given().when().get("/api/words/{id}", wordId)
        .then()
                .statusCode(200)
                .body("abbreviation", equalTo("CUST"));
    }

    @Test
    @DisplayName("수정 신청과 삭제 신청을 승인하면 대상에 반영된다")
    void updateAndDelete() {
        long wordId = createWordDirectly("고객", "CUST", "Customer", false);

        authenticate(userToken);
        long updateId = requestWord("UPDATE", wordId, wordPayload("고객", "CUST", "Client", false))
                .statusCode(201).extract().jsonPath().getLong("id");
        authenticate(adminToken);
        approveWithoutBody(updateId).statusCode(200);
        given().when().get("/api/words/{id}", wordId).then().body("englishName", equalTo("Client"));

        authenticate(userToken);
        long deleteId = requestWord("DELETE", wordId, null)
                .statusCode(201)
                .body("payload", nullValue())
                .extract().jsonPath().getLong("id");
        authenticate(adminToken);
        approveWithoutBody(deleteId).statusCode(200).body("targetId", equalTo((int) wordId));
        given().when().get("/api/words/{id}", wordId).then().statusCode(404);
    }

    @Test
    @DisplayName("반려는 의견이 필요하고, 반려된 신청의 대상은 바뀌지 않는다")
    void reject() {
        long wordId = createWordDirectly("고객", "CUST", "Customer", false);
        authenticate(userToken);
        long requestId = requestWord("UPDATE", wordId, wordPayload("고객", "CUST", "Client", false))
                .statusCode(201).extract().jsonPath().getLong("id");

        authenticate(adminToken);
        given().contentType(ContentType.JSON).body("{}")
        .when().post("/api/requests/{id}/reject", requestId)
        .then()
                .statusCode(400)
                .body("fieldErrors.field", equalTo(List.of("comment")));

        given().contentType(ContentType.JSON)
                .body("""
                        {"comment": "영문명은 Customer가 표준입니다"}
                        """)
        .when().post("/api/requests/{id}/reject", requestId)
        .then()
                .statusCode(200)
                .body("status", equalTo("REJECTED"))
                .body("reviewComment", equalTo("영문명은 Customer가 표준입니다"));

        given().when().get("/api/words/{id}", wordId).then().body("englishName", equalTo("Customer"));
    }

    @Test
    @DisplayName("신청 이후 관리자가 대상을 직접 수정했으면 승인이 409로 거부되고 신청은 대기 상태로 남는다")
    void targetChanged() {
        long wordId = createWordDirectly("고객", "CUST", "Customer", false);
        authenticate(userToken);
        long requestId = requestWord("UPDATE", wordId, wordPayload("고객", "CUST", "Client", false))
                .statusCode(201).extract().jsonPath().getLong("id");

        authenticate(adminToken);
        given().contentType(ContentType.JSON).body(wordPayload("고객", "CUST", "Customer", false).replace("null", "\"직접 수정\""))
        .when().put("/api/words/{id}", wordId)
        .then().statusCode(200);

        approveWithoutBody(requestId).statusCode(409).body("code", equalTo("REQUEST_TARGET_CHANGED"));
        given().when().get("/api/requests/{id}", requestId).then().body("status", equalTo("REQUESTED"));
    }

    @Test
    @DisplayName("같은 대상에 대기 중인 신청이 있으면 새 신청은 409를 반환한다")
    void alreadyPending() {
        long wordId = createWordDirectly("고객", "CUST", "Customer", false);
        authenticate(userToken);
        requestWord("UPDATE", wordId, wordPayload("고객", "CUST", "Client", false)).statusCode(201);

        requestWord("DELETE", wordId, null)
                .statusCode(409)
                .body("code", equalTo("REQUEST_ALREADY_PENDING"));
    }

    @Test
    @DisplayName("승인 시 업무 규칙 위반이면 원래 오류를 반환하고 신청은 대기 상태로 남는다")
    void approveFailsOnBusinessRule() {
        authenticate(userToken);
        long requestId = requestWord("CREATE", null, wordPayload("고객", "CUST", "Customer", false))
                .statusCode(201).extract().jsonPath().getLong("id");

        authenticate(adminToken);
        createWordDirectly("고객", "CST", "Client", false);

        approveWithoutBody(requestId).statusCode(409).body("code", equalTo("DUPLICATE_WORD_NAME"));
        given().when().get("/api/requests/{id}", requestId)
        .then()
                .body("status", equalTo("REQUESTED"))
                .body("reviewerUsername", nullValue());
    }

    @Test
    @DisplayName("신청자만 취소할 수 있고, 취소된 신청은 승인할 수 없다")
    void cancel() {
        authenticate(userToken);
        long requestId = requestWord("CREATE", null, wordPayload("고객", "CUST", "Customer", false))
                .statusCode(201).extract().jsonPath().getLong("id");

        authenticate(adminToken);
        given().when().post("/api/requests/{id}/cancel", requestId)
        .then().statusCode(403).body("code", equalTo("REQUEST_NOT_OWNED"));

        authenticate(userToken);
        given().when().post("/api/requests/{id}/cancel", requestId)
        .then().statusCode(200).body("status", equalTo("CANCELED"));

        authenticate(adminToken);
        approveWithoutBody(requestId).statusCode(409).body("code", equalTo("REQUEST_NOT_PENDING"));
    }

    @Test
    @DisplayName("일반 사용자는 자기 신청만 조회하고, 관리자는 모든 신청을 조회한다")
    void visibility() {
        long adminRequestId = requestWord("CREATE", null, wordPayload("번호", "NO", "Number", true))
                .statusCode(201).extract().jsonPath().getLong("id");
        authenticate(userToken);
        requestWord("CREATE", null, wordPayload("고객", "CUST", "Customer", false)).statusCode(201);

        given().queryParam("requester", ADMIN_USERNAME)
        .when().get("/api/requests")
        .then()
                .statusCode(200)
                .body("content.requesterUsername", equalTo(List.of(USER_USERNAME)));
        given().when().get("/api/requests/{id}", adminRequestId)
        .then().statusCode(404).body("code", equalTo("REQUEST_NOT_FOUND"));

        authenticate(adminToken);
        given().when().get("/api/requests").then().body("totalElements", equalTo(2));
        given().queryParam("requester", USER_USERNAME)
        .when().get("/api/requests")
        .then().body("content.requesterUsername", equalTo(List.of(USER_USERNAME)));
    }

    @Test
    @DisplayName("일반 사용자는 승인하거나 반려할 수 없다")
    void userCannotReview() {
        authenticate(userToken);
        long requestId = requestWord("CREATE", null, wordPayload("고객", "CUST", "Customer", false))
                .statusCode(201).extract().jsonPath().getLong("id");

        approveWithoutBody(requestId).statusCode(403).body("code", equalTo("FORBIDDEN"));
        given().contentType(ContentType.JSON).body("""
                        {"comment": "셀프 반려"}
                        """)
        .when().post("/api/requests/{id}/reject", requestId)
        .then().statusCode(403);
    }

    @Test
    @DisplayName("신청 종류와 맞지 않는 입력은 400, 신청 내용의 필드 오류는 payload 경로로 반환한다")
    void invalidForm() {
        authenticate(userToken);

        requestWord("CREATE", 1L, wordPayload("고객", "CUST", "Customer", false))
                .statusCode(400)
                .body("code", equalTo("INVALID_CHANGE_REQUEST"));

        requestWord("CREATE", null, wordPayload("고객", "1abc", "Customer", false))
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT"))
                .body("fieldErrors.field", hasItem("payload.abbreviation"));

        requestWord("UPDATE", 999_999L, wordPayload("고객", "CUST", "Customer", false))
                .statusCode(404)
                .body("code", equalTo("WORD_NOT_FOUND"));
    }

    @Test
    @DisplayName("도메인과 용어도 신청 후 승인으로 등록된다")
    void domainAndTerm() {
        long customerId = createWordDirectly("고객", "CUST", "Customer", false);
        long numberId = createWordDirectly("번호", "NO", "Number", true);

        authenticate(userToken);
        long domainRequestId = given().contentType(ContentType.JSON)
                .body("""
                        {"requestType": "CREATE", "payload":
                          {"name": "번호V20", "classifierWordId": %d, "dataType": "VARCHAR", "dataLength": 20}}
                        """.formatted(numberId))
        .when().post("/api/requests/domains")
        .then().statusCode(201).body("targetType", equalTo("DOMAIN"))
        .extract().jsonPath().getLong("id");

        authenticate(adminToken);
        long domainId = approveWithoutBody(domainRequestId).statusCode(200).extract().jsonPath().getLong("targetId");

        authenticate(userToken);
        long termRequestId = given().contentType(ContentType.JSON)
                .body("""
                        {"requestType": "CREATE", "payload": {"wordIds": [%d, %d], "domainId": %d}}
                        """.formatted(customerId, numberId, domainId))
        .when().post("/api/requests/terms")
        .then().statusCode(201).body("targetType", equalTo("TERM"))
        .extract().jsonPath().getLong("id");

        authenticate(adminToken);
        long termId = approveWithoutBody(termRequestId).statusCode(200).extract().jsonPath().getLong("targetId");
        given().when().get("/api/terms/{id}", termId)
        .then()
                .statusCode(200)
                .body("abbreviation", equalTo("CUST_NO"));
    }

    private static ValidatableResponse requestWord(String requestType, Long targetId, String payloadJson) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"requestType": "%s", "targetId": %s, "payload": %s, "reason": "테스트 신청"}
                        """.formatted(requestType, targetId, payloadJson))
                .when().post("/api/requests/words")
                .then();
    }

    /**
     * 본문 없이 승인한다.
     * REST Assured는 본문 없는 POST에 기본으로 form-urlencoded Content-Type을 붙여 JSON 엔드포인트가 415를 준다.
     * JSON Content-Type을 명시하면 빈 본문이 null로 처리된다.
     */
    private static ValidatableResponse approveWithoutBody(long requestId) {
        return given().contentType(ContentType.JSON).when().post("/api/requests/{id}/approve", requestId).then();
    }

    /** 관리자 권한으로 단어를 직접 등록한다. 호출 시점의 토큰이 관리자여야 한다. */
    private long createWordDirectly(String name, String abbreviation, String englishName, boolean classifier) {
        authenticate(adminToken);
        return given()
                .contentType(ContentType.JSON)
                .body(wordPayload(name, abbreviation, englishName, classifier))
                .when().post("/api/words")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static String wordPayload(String name, String abbreviation, String englishName, boolean classifier) {
        return """
                {"name": "%s", "abbreviation": "%s", "englishName": "%s", "description": null, "classifier": %s}
                """.formatted(name, abbreviation, englishName, classifier).strip();
    }
}
