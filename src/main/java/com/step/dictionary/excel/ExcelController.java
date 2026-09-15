package com.step.dictionary.excel;

import com.step.dictionary.auth.CurrentUser;
import com.step.dictionary.excel.dto.ExcelUploadResponse;
import com.step.dictionary.excel.support.ExcelWriter;
import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.meta.table.dto.MetaTableSearchCondition;
import com.step.dictionary.standard.domain.dto.DomainSearchCondition;
import com.step.dictionary.standard.term.dto.TermSearchCondition;
import com.step.dictionary.standard.word.dto.WordSearchCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 엑셀 다운로드/업로드 REST API ({@code /api/excel}). 로그인한 사용자 누구나 쓸 수 있다.
 *
 * <ul>
 *   <li>{@code GET /{words|domains|terms|tables}}: 목록 API와 같은 검색 조건으로 조회한 결과를 .xlsx로 내려준다 (최대 10,000건).</li>
 *   <li>{@code GET /{words|domains|terms|tables}/template}: 헤더만 있는 업로드 양식.</li>
 *   <li>{@code POST /{words|domains|terms|tables}}: multipart {@code file} 파트로 .xlsx를 올린다 (최대 1,000행, 10MB).
 *       표준 데이터는 ADMIN이면 반영, USER면 등록 신청을 만든다. 테이블 정의는 모두 반영한다.</li>
 * </ul>
 * 표준 데이터 쓰기 권한 규칙({@code SecurityConfig})은 {@code /api/words/**} 같은 경로에 걸려 있으므로,
 * 여기서는 서비스가 역할에 따라 반영/신청을 나눈다.
 */
@Tag(name = "엑셀", description = "표준 데이터와 테이블 정의의 엑셀 다운로드/업로드")
@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    /** .xlsx MIME 타입. */
    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final StandardExcelService standardExcelService;
    private final MetaExcelService metaExcelService;
    /** 다운로드 파일 이름의 날짜 계산용. */
    private final Clock clock;

    public ExcelController(StandardExcelService standardExcelService, MetaExcelService metaExcelService, Clock clock) {
        this.standardExcelService = standardExcelService;
        this.metaExcelService = metaExcelService;
        this.clock = clock;
    }

    // ---- 다운로드 ----

    @Operation(summary = "단어 다운로드", description = "단어 검색과 같은 조건. 파일 이름은 단어_yyyyMMdd.xlsx")
    @GetMapping("/words")
    public ResponseEntity<byte[]> downloadWords(@ParameterObject WordSearchCondition condition) {
        return download(ExcelSheet.WORD, standardExcelService.downloadWords(condition));
    }

    @Operation(summary = "도메인 다운로드", description = "도메인 검색과 같은 조건.")
    @GetMapping("/domains")
    public ResponseEntity<byte[]> downloadDomains(@ParameterObject DomainSearchCondition condition) {
        return download(ExcelSheet.DOMAIN, standardExcelService.downloadDomains(condition));
    }

    @Operation(summary = "용어 다운로드", description = "용어 검색과 같은 조건.")
    @GetMapping("/terms")
    public ResponseEntity<byte[]> downloadTerms(@ParameterObject TermSearchCondition condition) {
        return download(ExcelSheet.TERM, standardExcelService.downloadTerms(condition));
    }

    @Operation(summary = "테이블 정의 다운로드", description = "테이블 검색과 같은 조건. 한 행이 컬럼 하나이며 비표준 사유가 참고 열에 들어간다.")
    @GetMapping("/tables")
    public ResponseEntity<byte[]> downloadTables(@ParameterObject MetaTableSearchCondition condition) {
        return download(ExcelSheet.TABLE, metaExcelService.downloadTables(condition));
    }

    /** 업로드 양식. 경로 이름이 양식에 없으면 400. */
    @Operation(summary = "업로드 양식 다운로드", description = "target은 words, domains, terms, tables 중 하나.")
    @GetMapping("/{target}/template")
    public ResponseEntity<byte[]> template(@PathVariable String target) {
        ExcelSheet sheet = ExcelSheet.fromPath(target)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
        return file(sheet.getSheetName() + "_업로드양식.xlsx", ExcelWriter.write(sheet, List.of()));
    }

    // ---- 업로드 ----

    @Operation(summary = "단어 업로드", description = "ADMIN은 바로 등록, USER는 행마다 등록 신청. 오류 행이 있으면 아무것도 반영하지 않는다.")
    @PostMapping(value = "/words", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExcelUploadResponse uploadWords(@AuthenticationPrincipal Jwt jwt, @RequestPart("file") MultipartFile file) {
        return standardExcelService.uploadWords(file, CurrentUser.from(jwt));
    }

    @Operation(summary = "도메인 업로드", description = "분류어는 단어명으로 지정한다. ADMIN은 등록, USER는 신청.")
    @PostMapping(value = "/domains", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExcelUploadResponse uploadDomains(@AuthenticationPrincipal Jwt jwt, @RequestPart("file") MultipartFile file) {
        return standardExcelService.uploadDomains(file, CurrentUser.from(jwt));
    }

    @Operation(summary = "용어 업로드", description = "구성 단어명(쉼표 구분)과 도메인명으로 지정한다. ADMIN은 등록, USER는 신청.")
    @PostMapping(value = "/terms", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExcelUploadResponse uploadTerms(@AuthenticationPrincipal Jwt jwt, @RequestPart("file") MultipartFile file) {
        return standardExcelService.uploadTerms(file, CurrentUser.from(jwt));
    }

    @Operation(summary = "테이블 정의 업로드", description = "같은 테이블 물리명의 행을 묶어 테이블과 컬럼을 등록한다. 역할과 관계없이 바로 반영한다.")
    @PostMapping(value = "/tables", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExcelUploadResponse uploadTables(@RequestPart("file") MultipartFile file) {
        return metaExcelService.uploadTables(file);
    }

    /** 다운로드 응답. 파일 이름은 "{시트 이름}_{yyyyMMdd}.xlsx". */
    private ResponseEntity<byte[]> download(ExcelSheet sheet, byte[] content) {
        String date = LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE);
        return file(sheet.getSheetName() + "_" + date + ".xlsx", content);
    }

    /**
     * 파일 응답. 한글 파일 이름이 깨지지 않도록 RFC 5987 형식({@code filename*=UTF-8''...})으로 인코딩한다.
     */
    private static ResponseEntity<byte[]> file(String fileName, byte[] content) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(XLSX)
                .body(content);
    }
}
