package com.step.dictionary.pharmacy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;

import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 약제 업무 API: 처방 → 조제 → 검수 → 불출 → 반납 흐름과 단계별 업무 규칙.
 * 기본 토큰은 관리자(전권)이고, 등록자 필드는 로그인 아이디({@value #ADMIN_USERNAME})로 채워진다.
 */
class PharmacyApiIntegrationTest extends IntegrationTest {

    private static final String DEFAULT_PRESCRIBED_AT = "2026-11-02T09:00:00";

    // ---- 처방 ----

    @Test
    @DisplayName("처방을 품목과 함께 등록하면 201과 Location을 반환하고, 순번·대문자 코드가 채워진다")
    void createPrescription() {
        given()
                .contentType(ContentType.JSON)
                .body(prescriptionJson("rx-001", "P001", "홍길동", DEFAULT_PRESCRIBED_AT))
        .when()
                .post("/api/prescriptions")
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/prescriptions/\\d+"))
                .body("prescriptionNo", equalTo("RX-001"))
                .body("departmentCode", equalTo("IM"))
                .body("status", equalTo("ORDERED"))
                .body("items", hasSize(2))
                .body("items[0].lineNo", equalTo(1))
                .body("items[0].drugCode", equalTo("AMXCLN500"))
                .body("items[0].doseUnit", equalTo("MG"))
                .body("items[0].frequency", equalTo("TID"))
                .body("items[1].lineNo", equalTo(2))
                .body("items[1].totalQuantity", equalTo(3.0f));
    }

    @Test
    @DisplayName("처방전번호가 대소문자만 다르게 중복되면 409, 품목이 없으면 400을 반환한다")
    void createPrescriptionInvalid() {
        createPrescription("RX-001");

        given().contentType(ContentType.JSON).body(prescriptionJson("rx-001", "P002", "김철수", DEFAULT_PRESCRIBED_AT))
        .when().post("/api/prescriptions")
        .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_PRESCRIPTION_NO"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"prescriptionNo": "RX-002", "patientNo": "P001", "patientName": "홍길동",
                         "departmentCode": "IM", "prescriberName": "이의사", "prescriptionType": "OUTPATIENT",
                         "prescribedAt": "2026-11-02T09:00:00", "items": []}
                        """)
        .when().post("/api/prescriptions")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT"));
    }

    @Test
    @DisplayName("키워드(앞부분 일치)·상태·기간으로 처방을 검색하고 처방 일시 최신순으로 정렬한다")
    void searchPrescriptions() {
        createPrescription("RX-001", "P001", "홍길동", "2026-11-01T09:00:00");
        createPrescription("RX-002", "P002", "김철수", "2026-11-02T09:00:00");
        long cancelled = createPrescription("RX-003", "P003", "홍길순", "2026-11-03T09:00:00").getLong("id");
        cancel(cancelled).then().statusCode(200);

        given().queryParam("keyword", "홍길")
        .when().get("/api/prescriptions")
        .then()
                .statusCode(200)
                .body("content.prescriptionNo", equalTo(List.of("RX-003", "RX-001")));
        // 검색어는 앞부분 일치다 (인덱스를 타도록). 처방전번호는 대소문자를 구분하지 않고, 중간 글자로는 찾지 않는다.
        given().queryParam("keyword", "rx-00")
        .when().get("/api/prescriptions")
        .then().body("content.prescriptionNo", equalTo(List.of("RX-003", "RX-002", "RX-001")));
        given().queryParam("keyword", "P002")
        .when().get("/api/prescriptions")
        .then().body("content.prescriptionNo", equalTo(List.of("RX-002")));
        given().queryParam("keyword", "길동")
        .when().get("/api/prescriptions")
        .then().body("totalElements", equalTo(0));
        given().queryParam("keyword", "001")
        .when().get("/api/prescriptions")
        .then().body("totalElements", equalTo(0));

        given().queryParam("status", "ORDERED").queryParam("from", "2026-11-02").queryParam("to", "2026-11-02")
        .when().get("/api/prescriptions")
        .then()
                .statusCode(200)
                .body("content.prescriptionNo", equalTo(List.of("RX-002")))
                .body("totalElements", equalTo(1));
    }

    @Test
    @DisplayName("조제 전 처방은 취소할 수 있고, 취소된 처방은 다시 취소하거나 조제할 수 없다")
    void cancelPrescription() {
        JsonPath rx = createPrescription("RX-001");
        long id = rx.getLong("id");

        cancel(id)
        .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"))
                .body("cancelReason", equalTo("처방 변경"));

        cancel(id).then().statusCode(409).body("code", equalTo("PRESCRIPTION_NOT_CANCELLABLE"));
        dispense(rx).then().statusCode(409).body("code", equalTo("PRESCRIPTION_CANCELLED"));
    }

    // ---- 조제 ----

    @Test
    @DisplayName("처방을 조제하면 조제자가 로그인 사용자로 채워지고, 처방은 DISPENSED가 되어 취소할 수 없다")
    void dispense() {
        JsonPath rx = createPrescription("RX-001");

        dispense(rx)
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/dispensings/\\d+"))
                .body("prescriptionNo", equalTo("RX-001"))
                .body("status", equalTo("DISPENSED"))
                .body("dispensedBy", equalTo(ADMIN_USERNAME))
                .body("items", hasSize(2))
                .body("items[0].drugName", equalTo("아목시실린캡슐 500mg"))
                .body("items[0].lotNo", equalTo("LOT-1"))
                .body("items[0].expiryDate", equalTo("2027-12-31"));

        given().when().get("/api/prescriptions/{id}", rx.getLong("id"))
        .then().body("status", equalTo("DISPENSED"));
        cancel(rx.getLong("id")).then().statusCode(409).body("code", equalTo("PRESCRIPTION_NOT_CANCELLABLE"));
    }

    @Test
    @DisplayName("조제 품목이 처방 품목과 하나씩 맞지 않으면 400, 이미 조제된 처방을 또 조제하면 409를 반환한다")
    void dispenseInvalid() {
        JsonPath rx = createPrescription("RX-001");
        long prescriptionId = rx.getLong("id");
        long first = rx.getLong("items[0].id");

        // 품목 누락
        given().contentType(ContentType.JSON).body(dispensingJson(prescriptionId, List.of(first)))
        .when().post("/api/dispensings")
        .then().statusCode(400).body("code", equalTo("INVALID_DISPENSING_ITEMS"));
        // 같은 품목 두 번
        given().contentType(ContentType.JSON).body(dispensingJson(prescriptionId, List.of(first, first)))
        .when().post("/api/dispensings")
        .then().statusCode(400).body("code", equalTo("INVALID_DISPENSING_ITEMS"));

        dispense(rx).then().statusCode(201);
        dispense(rx).then().statusCode(409).body("code", equalTo("DISPENSING_ALREADY_EXISTS"));
    }

    // ---- 검수 ----

    @Test
    @DisplayName("전 품목 합격이면 검수 결과 PASS, 조제는 INSPECTED가 되고 다시 검수할 수 없다")
    void inspectPass() {
        JsonPath dispensing = dispensed("RX-001");

        inspect(dispensing, "PASS", "PASS")
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/inspections/\\d+"))
                .body("result", equalTo("PASS"))
                .body("inspectedBy", equalTo(ADMIN_USERNAME))
                .body("items[0].result", equalTo("PASS"))
                .body("items[0].defectReason", nullValue());

        given().when().get("/api/dispensings/{id}", dispensing.getLong("id"))
        .then().body("status", equalTo("INSPECTED"));
        inspect(dispensing, "PASS", "PASS")
        .then().statusCode(409).body("code", equalTo("DISPENSING_NOT_INSPECTABLE"));
    }

    @Test
    @DisplayName("불합격 품목이 있으면 검수 결과 FAIL, 조제는 REJECTED가 되어 불출할 수 없고 같은 처방을 다시 조제할 수 있다")
    void inspectFailAndRedispense() {
        JsonPath rx = createPrescription("RX-001");
        JsonPath dispensing = dispense(rx).then().statusCode(201).extract().jsonPath();

        inspect(dispensing, "PASS", "FAIL")
        .then()
                .statusCode(201)
                .body("result", equalTo("FAIL"))
                .body("items[1].defectReason", equalTo("수량 부족"));

        given().when().get("/api/dispensings/{id}", dispensing.getLong("id"))
        .then().body("status", equalTo("REJECTED"));
        issue(dispensing.getLong("id"))
        .then().statusCode(409).body("code", equalTo("DISPENSING_NOT_ISSUABLE"));
        dispense(rx).then().statusCode(201).body("status", equalTo("DISPENSED"));
    }

    @Test
    @DisplayName("불합격 품목에 불량 사유가 없거나 검수 품목이 조제 품목과 맞지 않으면 400을 반환한다")
    void inspectInvalid() {
        JsonPath dispensing = dispensed("RX-001");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"dispensingId": %d, "items": [
                          {"dispensingItemId": %d, "result": "PASS"},
                          {"dispensingItemId": %d, "result": "FAIL", "defectReason": " "}]}
                        """.formatted(dispensing.getLong("id"), dispensing.getLong("items[0].id"),
                        dispensing.getLong("items[1].id")))
        .when().post("/api/inspections")
        .then().statusCode(400).body("code", equalTo("DEFECT_REASON_REQUIRED"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"dispensingId": %d, "items": [{"dispensingItemId": %d, "result": "PASS"}]}
                        """.formatted(dispensing.getLong("id"), dispensing.getLong("items[0].id")))
        .when().post("/api/inspections")
        .then().statusCode(400).body("code", equalTo("INVALID_INSPECTION_ITEMS"));
    }

    // ---- 불출 ----

    @Test
    @DisplayName("검수 합격한 조제를 불출하면 조제 품목이 수량 그대로 옮겨지고, 조제는 ISSUED가 되어 다시 불출할 수 없다")
    void issueDispensing() {
        JsonPath dispensing = inspectedDispensing("RX-001");

        issue(dispensing.getLong("id"))
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/issues/\\d+"))
                .body("destinationCode", equalTo("W71"))
                .body("issuedBy", equalTo(ADMIN_USERNAME))
                .body("items", hasSize(2))
                .body("items[0].quantity", equalTo(21.0f))
                .body("items[0].returnedQuantity", equalTo(0.0f))
                .body("items[0].lotNo", equalTo("LOT-1"));

        given().when().get("/api/dispensings/{id}", dispensing.getLong("id"))
        .then().body("status", equalTo("ISSUED"));
        issue(dispensing.getLong("id"))
        .then().statusCode(409).body("code", equalTo("DISPENSING_NOT_ISSUABLE"));
    }

    // ---- 반납 ----

    @Test
    @DisplayName("불출 품목 일부를 여러 번 반납할 수 있고, 반납 합계는 불출 상세의 반납 수량에 반영된다")
    void returnIssuedDrugs() {
        JsonPath issued = issued("RX-001");
        long issueId = issued.getLong("id");
        long firstItem = issued.getLong("items[0].id");

        returnDrugs(issueId, firstItem, "5", "RESTOCK")
        .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/returns/\\d+"))
                .body("returnedBy", equalTo(ADMIN_USERNAME))
                .body("reason", equalTo("퇴원"))
                .body("items", hasSize(1))
                .body("items[0].issuedQuantity", equalTo(21.0f))
                .body("items[0].quantity", equalTo(5.0f))
                .body("items[0].disposition", equalTo("RESTOCK"));
        returnDrugs(issueId, firstItem, "16", "DISPOSE").then().statusCode(201);

        given().when().get("/api/issues/{id}", issueId)
        .then()
                .statusCode(200)
                .body("items[0].returnedQuantity", equalTo(21.0f))
                .body("items[1].returnedQuantity", equalTo(0.0f));
    }

    @Test
    @DisplayName("반납 합계가 불출 수량을 넘으면 409, 다른 불출의 품목이거나 중복이면 400을 반환한다")
    void returnInvalid() {
        JsonPath issued = issued("RX-001");
        long issueId = issued.getLong("id");
        long firstItem = issued.getLong("items[0].id");

        returnDrugs(issueId, firstItem, "20", "RESTOCK").then().statusCode(201);
        returnDrugs(issueId, firstItem, "1.5", "RESTOCK")
        .then().statusCode(409).body("code", equalTo("RETURN_QUANTITY_EXCEEDED"));

        returnDrugs(issueId, 9999L, "1", "RESTOCK")
        .then().statusCode(400).body("code", equalTo("INVALID_RETURN_ITEMS"));
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"issueId": %d, "reason": "퇴원", "items": [
                          {"issueItemId": %d, "quantity": 0.5, "disposition": "RESTOCK"},
                          {"issueItemId": %d, "quantity": 0.5, "disposition": "DISPOSE"}]}
                        """.formatted(issueId, firstItem, firstItem))
        .when().post("/api/returns")
        .then().statusCode(400).body("code", equalTo("INVALID_RETURN_ITEMS"));
    }

    @Test
    @DisplayName("조제·검수·불출·반납 목록은 처방전번호·환자로 검색되고 처방 정보를 함께 보여준다")
    void searchDownstream() {
        JsonPath issued = issued("RX-001");
        returnDrugs(issued.getLong("id"), issued.getLong("items[0].id"), "1", "RESTOCK").then().statusCode(201);
        dispense(createPrescription("RX-002", "P002", "김철수", "2026-11-02T10:00:00")).then().statusCode(201);

        given().queryParam("keyword", "rx-001")
        .when().get("/api/dispensings")
        .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].status", equalTo("ISSUED"))
                .body("content[0].patientName", equalTo("홍길동"));
        given().queryParam("status", "DISPENSED")
        .when().get("/api/dispensings")
        .then().body("content.prescriptionNo", equalTo(List.of("RX-002")));
        given().queryParam("result", "PASS")
        .when().get("/api/inspections")
        .then().body("content.prescriptionNo", equalTo(List.of("RX-001")));
        given().queryParam("destinationCode", "w71")
        .when().get("/api/issues")
        .then().body("content.prescriptionNo", equalTo(List.of("RX-001")));
        given().queryParam("keyword", "홍길동")
        .when().get("/api/returns")
        .then()
                .body("content", hasSize(1))
                .body("content[0].destinationName", equalTo("71병동"));
    }

    @Test
    @DisplayName("없는 처방·조제·검수·불출·반납을 조회하면 404를 반환한다")
    void notFound() {
        given().when().get("/api/prescriptions/{id}", 999).then().statusCode(404)
                .body("code", equalTo("PRESCRIPTION_NOT_FOUND"));
        given().when().get("/api/dispensings/{id}", 999).then().statusCode(404)
                .body("code", equalTo("DISPENSING_NOT_FOUND"));
        given().when().get("/api/inspections/{id}", 999).then().statusCode(404)
                .body("code", equalTo("INSPECTION_NOT_FOUND"));
        given().when().get("/api/issues/{id}", 999).then().statusCode(404)
                .body("code", equalTo("ISSUE_NOT_FOUND"));
        given().when().get("/api/returns/{id}", 999).then().statusCode(404)
                .body("code", equalTo("RETURN_NOT_FOUND"));
    }

    @Test
    @DisplayName("일반 사용자는 약제 업무를 조회만 할 수 있고, 등록·취소는 403을 반환한다")
    void userCanOnlyRead() {
        long id = createPrescription("RX-001").getLong("id");
        authenticate(userToken);

        given().when().get("/api/prescriptions").then().statusCode(200);
        given().when().get("/api/prescriptions/{id}", id).then().statusCode(200);
        given().when().get("/api/dispensings").then().statusCode(200);
        given().when().get("/api/inspections").then().statusCode(200);
        given().when().get("/api/issues").then().statusCode(200);
        given().when().get("/api/returns").then().statusCode(200);

        given().contentType(ContentType.JSON).body(prescriptionJson("RX-002", "P002", "김철수", DEFAULT_PRESCRIBED_AT))
        .when().post("/api/prescriptions")
        .then().statusCode(403).body("code", equalTo("FORBIDDEN"));
        cancel(id).then().statusCode(403);
        for (String path : List.of("/api/dispensings", "/api/inspections", "/api/issues", "/api/returns")) {
            given().contentType(ContentType.JSON).body("{}")
            .when().post(path)
            .then().statusCode(403);
        }
    }

    // ---- 도우미 ----

    /** 품목 2개(아목시실린 21캡슐, 타이레놀 3정)짜리 처방을 등록한다. */
    private JsonPath createPrescription(String prescriptionNo) {
        return createPrescription(prescriptionNo, "P001", "홍길동", DEFAULT_PRESCRIBED_AT);
    }

    private JsonPath createPrescription(String prescriptionNo, String patientNo, String patientName,
            String prescribedAt) {
        return given()
                .contentType(ContentType.JSON)
                .body(prescriptionJson(prescriptionNo, patientNo, patientName, prescribedAt))
        .when()
                .post("/api/prescriptions")
        .then()
                .statusCode(201)
                .extract().jsonPath();
    }

    private static String prescriptionJson(String prescriptionNo, String patientNo, String patientName,
            String prescribedAt) {
        return """
                {"prescriptionNo": "%s", "patientNo": "%s", "patientName": "%s",
                 "departmentCode": "im", "prescriberName": "이의사", "prescriptionType": "OUTPATIENT",
                 "prescribedAt": "%s", "remark": null,
                 "items": [
                   {"drugCode": "amxcln500", "drugName": "아목시실린캡슐 500mg", "dose": 500, "doseUnit": "mg",
                    "route": "po", "frequency": "tid", "days": 7, "totalQuantity": 21, "usageNote": "식후 30분"},
                   {"drugCode": "TYL500", "drugName": "타이레놀정 500mg", "dose": 1, "doseUnit": "TAB",
                    "route": "PO", "frequency": "PRN", "days": 3, "totalQuantity": 3, "usageNote": null}]}
                """.formatted(prescriptionNo, patientNo, patientName, prescribedAt);
    }

    private Response cancel(long prescriptionId) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"reason": "처방 변경"}
                        """)
                .post("/api/prescriptions/{id}/cancel", prescriptionId);
    }

    /** 처방의 모든 품목을 조제한다 (첫 품목 21, 나머지 3, 로트는 LOT-순번). */
    private Response dispense(JsonPath prescription) {
        return given()
                .contentType(ContentType.JSON)
                .body(dispensingJson(prescription.getLong("id"), prescription.getList("items.id", Long.class)))
                .post("/api/dispensings");
    }

    private static String dispensingJson(long prescriptionId, List<Long> prescriptionItemIds) {
        String items = IntStream.range(0, prescriptionItemIds.size())
                .mapToObj(i -> """
                        {"prescriptionItemId": %d, "quantity": %s, "lotNo": "LOT-%d", "expiryDate": "2027-12-31"}
                        """.formatted(prescriptionItemIds.get(i), i == 0 ? "21" : "3", i + 1))
                .collect(Collectors.joining(","));
        return """
                {"prescriptionId": %d, "remark": null, "items": [%s]}
                """.formatted(prescriptionId, items);
    }

    /** 조제 품목 2개에 결과를 하나씩 매겨 검수한다. 불합격 품목의 사유는 "수량 부족". */
    private Response inspect(JsonPath dispensing, String first, String second) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"dispensingId": %d, "items": [
                          {"dispensingItemId": %d, "result": "%s", "defectReason": %s},
                          {"dispensingItemId": %d, "result": "%s", "defectReason": %s}]}
                        """.formatted(dispensing.getLong("id"),
                        dispensing.getLong("items[0].id"), first, defectReason(first),
                        dispensing.getLong("items[1].id"), second, defectReason(second)))
                .post("/api/inspections");
    }

    private static String defectReason(String result) {
        return "FAIL".equals(result) ? "\"수량 부족\"" : "null";
    }

    /** 처방 → 조제까지 마친 조제. */
    private JsonPath dispensed(String prescriptionNo) {
        return dispense(createPrescription(prescriptionNo)).then().statusCode(201).extract().jsonPath();
    }

    /** 처방 → 조제 → 검수(전 품목 합격)까지 마친 조제. */
    private JsonPath inspectedDispensing(String prescriptionNo) {
        JsonPath dispensing = dispensed(prescriptionNo);
        inspect(dispensing, "PASS", "PASS").then().statusCode(201);
        return dispensing;
    }

    /** 처방 → 조제 → 검수 → 불출까지 마친 불출. */
    private JsonPath issued(String prescriptionNo) {
        return issue(inspectedDispensing(prescriptionNo).getLong("id")).then().statusCode(201).extract().jsonPath();
    }

    private Response issue(long dispensingId) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"dispensingId": %d, "destinationCode": "w71", "destinationName": "71병동",
                         "receiverName": "박간호", "remark": null}
                        """.formatted(dispensingId))
                .post("/api/issues");
    }

    private Response returnDrugs(long issueId, long issueItemId, String quantity, String disposition) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"issueId": %d, "reason": "퇴원", "items": [
                          {"issueItemId": %d, "quantity": %s, "disposition": "%s"}]}
                        """.formatted(issueId, issueItemId, quantity, disposition))
                .post("/api/returns");
    }
}
