package com.step.dictionary.meta.column;

import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.meta.column.dto.MetaColumnRequest;
import com.step.dictionary.meta.column.dto.MetaColumnResponse;
import com.step.dictionary.meta.column.dto.NonStandardColumnCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 컬럼 정의 REST API.
 *
 * <ul>
 *   <li>{@code /api/tables/{tableId}/columns}: 테이블에 속한 컬럼 CRUD (하위 리소스)</li>
 *   <li>{@code /api/columns/non-standard}: 여러 테이블에 걸친 비표준 컬럼 검색</li>
 * </ul>
 * 경로가 두 갈래라 클래스 수준 {@code @RequestMapping}을 두지 않고 메서드마다 전체 경로를 적는다.
 */
@Tag(name = "컬럼 정의", description = "테이블 컬럼 정의와 비표준 컬럼 검출")
@RestController
public class MetaColumnController {

    private final MetaColumnService columnService;

    public MetaColumnController(MetaColumnService columnService) {
        this.columnService = columnService;
    }

    /** 컬럼 등록. 201 Created + Location 헤더({@code /api/tables/{tableId}/columns/{id}}). */
    @Operation(summary = "컬럼 등록", description = "물리명은 대문자로 저장한다. 응답에 표준 검출 결과(standard, issues)가 포함된다.")
    @PostMapping("/api/tables/{tableId}/columns")
    public ResponseEntity<MetaColumnResponse> create(
            @PathVariable Long tableId, @Valid @RequestBody MetaColumnRequest request) {
        MetaColumnResponse response = columnService.create(tableId, request);
        return ResponseEntity.created(URI.create("/api/tables/" + tableId + "/columns/" + response.id()))
                .body(response);
    }

    /** 테이블의 전체 컬럼 목록 (페이징 없음). 테이블이 없으면 404. */
    @Operation(summary = "테이블 컬럼 목록", description = "순서(seq) 오름차순. 컬럼마다 표준 검출 결과를 포함한다.")
    @GetMapping("/api/tables/{tableId}/columns")
    public List<MetaColumnResponse> findAll(@PathVariable Long tableId) {
        return columnService.findAll(tableId);
    }

    /** 컬럼 단건 조회. 다른 테이블의 컬럼이면 404. */
    @Operation(summary = "컬럼 단건 조회")
    @GetMapping("/api/tables/{tableId}/columns/{columnId}")
    public MetaColumnResponse findById(@PathVariable Long tableId, @PathVariable Long columnId) {
        return columnService.findById(tableId, columnId);
    }

    /** 컬럼 수정 (전체 교체). 소속 테이블은 바꿀 수 없다. */
    @Operation(summary = "컬럼 수정")
    @PutMapping("/api/tables/{tableId}/columns/{columnId}")
    public MetaColumnResponse update(
            @PathVariable Long tableId, @PathVariable Long columnId, @Valid @RequestBody MetaColumnRequest request) {
        return columnService.update(tableId, columnId, request);
    }

    /** 컬럼 삭제. 성공 시 204. */
    @Operation(summary = "컬럼 삭제")
    @DeleteMapping("/api/tables/{tableId}/columns/{columnId}")
    public ResponseEntity<Void> delete(@PathVariable Long tableId, @PathVariable Long columnId) {
        columnService.delete(tableId, columnId);
        return ResponseEntity.noContent().build();
    }

    /** 비표준 컬럼 검색 ({@code ?tableId=&issue=&page=&size=}). 기본 20건. */
    @Operation(summary = "비표준 컬럼 검색",
            description = "용어 매핑 없음(NO_TERM), 물리명 불일치(NAME_MISMATCH), 타입 불일치(TYPE_MISMATCH), "
                    + "길이/소수점 불일치(LENGTH_MISMATCH) 중 하나라도 해당하는 컬럼. 테이블 물리명, 컬럼 순서로 정렬.")
    @GetMapping("/api/columns/non-standard")
    public PageResponse<MetaColumnResponse> searchNonStandard(
            @ParameterObject NonStandardColumnCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return columnService.searchNonStandard(condition, pageable);
    }
}
