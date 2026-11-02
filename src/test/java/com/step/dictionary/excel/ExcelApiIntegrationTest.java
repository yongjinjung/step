package com.step.dictionary.excel;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;

import com.step.dictionary.excel.support.ExcelWriter;
import com.step.dictionary.support.IntegrationTest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExcelApiIntegrationTest extends IntegrationTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Test
    @DisplayName("관리자가 단어를 업로드하면 바로 등록되고 변경 이력도 남는다")
    void adminUploadsWords() {
        upload("words", ExcelWriter.write(ExcelSheet.WORD, List.of(
                Arrays.asList("고객", "cust", "Customer", "N", null),
                Arrays.asList("번호", "NO", "Number", "Y", "분류어"))))
                .statusCode(200)
                .body("mode", equalTo("APPLIED"))
                .body("rowCount", equalTo(2))
                .body("ids", hasSize(2));

        given().queryParam("classifier", true)
        .when().get("/api/words")
        .then().body("content.name", equalTo(List.of("번호")));
        given().queryParam("keyword", "cust")
        .when().get("/api/words")
        .then().body("content[0].abbreviation", equalTo("CUST"));
        given().queryParam("targetType", "WORD").queryParam("changeType", "CREATE")
        .when().get("/api/histories")
        .then().body("totalElements", equalTo(2));
    }

    @Test
    @DisplayName("오류 행이 하나라도 있으면 아무것도 반영하지 않고 모든 오류를 행 번호와 함께 돌려준다")
    void allOrNothing() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"name": "고객", "abbreviation": "CUST", "englishName": "Customer", "description": null, "classifier": false}
                        """)
        .when().post("/api/words")
        .then().statusCode(201);

        upload("words", ExcelWriter.write(ExcelSheet.WORD, List.of(
                Arrays.asList("번호", "NO", "Number", "Y", null),
                Arrays.asList("손님", "cust", "Guest", "N", null),
                Arrays.asList("일자", "1DT", "Date", "N", null),
                Arrays.asList("금액", "AMT", "Amount", "X", null))))
                .statusCode(400)
                .body("code", equalTo("EXCEL_INVALID_ROWS"))
                .body("fieldErrors.field", equalTo(List.of("3행", "4행 영문약어", "5행 분류어 여부(Y/N)")))
                .body("fieldErrors[0].reason", equalTo("이미 등록된 영문약어입니다."));

        given().when().get("/api/words").then().body("totalElements", equalTo(1));
    }

    @Test
    @DisplayName("일반 사용자가 단어를 업로드하면 행마다 등록 신청이 만들어지고 단어는 아직 생기지 않는다")
    void userUploadCreatesRequests() {
        authenticate(userToken);

        upload("words", ExcelWriter.write(ExcelSheet.WORD, List.of(
                Arrays.asList("고객", "CUST", "Customer", "N", null),
                Arrays.asList("번호", "NO", "Number", "Y", null))))
                .statusCode(200)
                .body("mode", equalTo("REQUESTED"))
                .body("ids", hasSize(2));

        given().when().get("/api/words").then().body("totalElements", equalTo(0));
        given().when().get("/api/requests")
        .then()
                .body("totalElements", equalTo(2))
                .body("content.status", everyItem(equalTo("REQUESTED")))
                .body("content.reason", everyItem(containsString("엑셀 업로드 upload.xlsx")));
    }

    @Test
    @DisplayName("도메인과 용어는 참조 대상을 이름으로 지정해 업로드하고, 없는 이름은 그 열의 오류가 된다")
    void domainsAndTerms() {
        createWord("고객", "CUST", false);
        createWord("번호", "NO", true);

        upload("domains", ExcelWriter.write(ExcelSheet.DOMAIN, List.of(
                Arrays.asList("번호V20", "번호", "varchar", 20, null, null))))
                .statusCode(200);

        upload("terms", ExcelWriter.write(ExcelSheet.TERM, List.of(
                Arrays.asList("고객, 없는단어", "번호V20", null, null, null))))
                .statusCode(400)
                .body("fieldErrors.field", equalTo(List.of("2행 구성 단어명(쉼표 구분)")))
                .body("fieldErrors[0].reason", containsString("없는단어"));

        upload("terms", ExcelWriter.write(ExcelSheet.TERM, List.of(
                Arrays.asList("고객, 번호", "번호V20", "고객 식별 번호", "무시되는 값", null))))
                .statusCode(200);

        given().when().get("/api/terms")
        .then()
                .body("content[0].name", equalTo("고객번호"))
                .body("content[0].abbreviation", equalTo("CUST_NO"))
                .body("content[0].dataLength", equalTo(20));
    }

    @Test
    @DisplayName("일반 사용자도 테이블 정의를 업로드해 바로 등록하며, 같은 물리명 행은 한 테이블로 묶인다")
    void userUploadsTables() {
        authenticate(userToken);

        List<Integer> tableIds = upload("tables", ExcelWriter.write(ExcelSheet.TABLE, List.of(
                Arrays.asList("TB_CUSTOMER", "고객", null, 1, "cust_no", "고객번호", null, "VARCHAR", 20, null, "N", "Y", null),
                Arrays.asList("tb_customer", "무시", null, null, "CUST_NM", "고객명", null, "VARCHAR", 100, null, "Y", "N", null),
                Arrays.asList("TB_ORDER", "주문", "주문 테이블", null, null, null, null, null, null, null, null, null, null))))
                .statusCode(200)
                .body("mode", equalTo("APPLIED"))
                .body("rowCount", equalTo(3))
                .extract().jsonPath().getList("ids", Integer.class);

        assertThat(tableIds).hasSize(2);
        given().when().get("/api/tables")
        .then().body("content.physicalName", equalTo(List.of("TB_CUSTOMER", "TB_ORDER")));
        given().when().get("/api/tables/{id}/columns", tableIds.getFirst())
        .then()
                .body("physicalName", equalTo(List.of("CUST_NO", "CUST_NM")))
                .body("seq", equalTo(List.of(1, 2)))
                .body("[0].issues", equalTo(List.of("NO_TERM")));
    }

    @Test
    @DisplayName("테이블 정의 업로드의 검증·업무 규칙 오류도 전체를 반영하지 않는다")
    void tableUploadErrors() {
        upload("tables", ExcelWriter.write(ExcelSheet.TABLE, List.of(
                Arrays.asList("TB_A", "A", null, 1, "ID", "아이디", null, "BIGINT", null, null, "Y", "Y", null),
                Arrays.asList("TB_B", null, null, null, null, null, null, null, null, null, null, null, null))))
                .statusCode(400)
                .body("fieldErrors.field", equalTo(List.of("2행", "3행 테이블 논리명")))
                .body("fieldErrors[0].reason", equalTo("기본키 컬럼은 NULL을 허용할 수 없습니다."));

        given().when().get("/api/tables").then().body("totalElements", equalTo(0));
    }

    @Test
    @DisplayName("단어를 검색 조건으로 다운로드하면 한글 파일 이름의 xlsx를 내려준다")
    void downloadWords() {
        createWord("고객", "CUST", false);
        createWord("번호", "NO", true);

        ExtractableResponse<Response> response = given().queryParam("classifier", true)
        .when().get("/api/excel/words")
        .then()
                .statusCode(200)
                .contentType(XLSX)
                .header("Content-Disposition", containsString("filename*=UTF-8''%EB%8B%A8%EC%96%B4_"))
                .extract();

        List<List<String>> rows = readSheet(response.asByteArray());
        assertThat(rows).containsExactly(
                ExcelSheet.WORD.getHeaders(),
                List.of("번호", "NO", "NO", "Y", ""));
    }

    @Test
    @DisplayName("테이블 정의를 다운로드하면 컬럼마다 한 행이고 비표준 사유가 참고 열에 들어간다")
    void downloadTables() {
        long tableId = given().contentType(ContentType.JSON)
                .body("""
                        {"physicalName": "TB_CUSTOMER", "logicalName": "고객", "description": null}
                        """)
        .when().post("/api/tables")
        .then().statusCode(201)
        .extract().jsonPath().getLong("id");
        given().contentType(ContentType.JSON)
                .body("""
                        {"physicalName": "CUST_NO", "logicalName": "고객번호", "termId": null, "dataType": "VARCHAR",
                         "dataLength": 20, "dataScale": null, "nullable": false, "primaryKey": true, "seq": 1}
                        """)
        .when().post("/api/tables/{id}/columns", tableId)
        .then().statusCode(201);

        byte[] bytes = given().when().get("/api/excel/tables").then().statusCode(200).extract().asByteArray();

        assertThat(readSheet(bytes).get(1)).containsExactly(
                "TB_CUSTOMER", "고객", "", "1", "CUST_NO", "고객번호", "", "VARCHAR", "20", "", "N", "Y", "NO_TERM");
    }

    @Test
    @DisplayName("업로드 양식은 헤더만 있고, 내려받은 양식을 채워 그대로 올릴 수 있다")
    void template() {
        byte[] bytes = given().when().get("/api/excel/{target}/template", "terms")
                .then().statusCode(200).contentType(XLSX)
                .extract().asByteArray();
        assertThat(readSheet(bytes)).containsExactly(ExcelSheet.TERM.getHeaders());

        // 권한은 요청 경로를 메뉴로 옮겨 검사하므로(ApiPermissionRules), 규칙에 없는 대상은 컨트롤러에 닿기 전에 막힌다.
        given().when().get("/api/excel/{target}/template", "unknown").then().statusCode(403);
    }

    @Test
    @DisplayName("파일 파트가 없거나 xlsx가 아니거나 헤더가 다르면 400을 반환한다")
    void invalidUploads() {
        given().multiPart("other", "a.txt", "x".getBytes(), "text/plain")
        .when().post("/api/excel/words")
        .then().statusCode(400).body("code", equalTo("INVALID_INPUT"));

        upload("words", "단어명,영문약어".getBytes())
                .statusCode(400).body("code", equalTo("EXCEL_INVALID_FILE"));

        upload("words", ExcelWriter.write(ExcelSheet.DOMAIN, List.of(Arrays.asList("번호V20", "번호", "VARCHAR", 20, null, null))))
                .statusCode(400)
                .body("code", equalTo("EXCEL_INVALID_HEADER"))
                .body("fieldErrors[0].field", equalTo("헤더"));
    }

    private static ValidatableResponse upload(String target, byte[] content) {
        return given().multiPart("file", "upload.xlsx", content, XLSX)
                .when().post("/api/excel/" + target)
                .then();
    }

    private static void createWord(String name, String abbreviation, boolean classifier) {
        given().contentType(ContentType.JSON)
                .body("""
                        {"name": "%s", "abbreviation": "%s", "englishName": "%s", "description": null, "classifier": %s}
                        """.formatted(name, abbreviation, abbreviation, classifier))
                .when().post("/api/words")
                .then().statusCode(201);
    }

    /** 첫 시트를 화면에 보이는 문자열로 읽는다. 각 행은 헤더 수만큼 채우고 빈 셀은 빈 문자열이다. */
    private static List<List<String>> readSheet(byte[] bytes) {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int columnCount = sheet.getRow(0).getLastCellNum();
            List<List<String>> rows = new ArrayList<>();
            for (int index = 0; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                List<String> values = new ArrayList<>();
                for (int column = 0; column < columnCount; column++) {
                    values.add(row == null || row.getCell(column) == null ? "" : formatter.formatCellValue(row.getCell(column)));
                }
                rows.add(values);
            }
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
