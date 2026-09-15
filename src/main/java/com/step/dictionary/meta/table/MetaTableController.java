package com.step.dictionary.meta.table;

import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.meta.table.dto.MetaTableRequest;
import com.step.dictionary.meta.table.dto.MetaTableResponse;
import com.step.dictionary.meta.table.dto.MetaTableSearchCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테이블 정의 REST API ({@code /api/tables}).
 * 테이블에 속한 컬럼 API는 {@code MetaColumnController}({@code /api/tables/{tableId}/columns})에 있다.
 */
@Tag(name = "테이블 정의", description = "테이블 정의 관리")
@RestController
@RequestMapping("/api/tables")
public class MetaTableController {

    private final MetaTableService tableService;

    public MetaTableController(MetaTableService tableService) {
        this.tableService = tableService;
    }

    /** 테이블 등록. 201 Created + Location 헤더. */
    @Operation(summary = "테이블 등록", description = "물리명은 대문자로 저장한다.")
    @PostMapping
    public ResponseEntity<MetaTableResponse> create(@Valid @RequestBody MetaTableRequest request) {
        MetaTableResponse response = tableService.create(request);
        return ResponseEntity.created(URI.create("/api/tables/" + response.id())).body(response);
    }

    /** 테이블 단건 조회. 없으면 404. */
    @Operation(summary = "테이블 단건 조회")
    @GetMapping("/{id}")
    public MetaTableResponse findById(@PathVariable Long id) {
        return tableService.findById(id);
    }

    /** 테이블 검색 ({@code ?keyword=&page=&size=}). 기본 20건. */
    @Operation(summary = "테이블 검색", description = "keyword는 물리명/논리명에 부분 일치. 물리명 오름차순 정렬.")
    @GetMapping
    public PageResponse<MetaTableResponse> search(
            @ParameterObject MetaTableSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return tableService.search(condition, pageable);
    }

    /** 테이블 수정 (전체 교체). */
    @Operation(summary = "테이블 수정")
    @PutMapping("/{id}")
    public MetaTableResponse update(@PathVariable Long id, @Valid @RequestBody MetaTableRequest request) {
        return tableService.update(id, request);
    }

    /** 테이블 삭제. 컬럼 정의도 함께 삭제하며 성공 시 204. */
    @Operation(summary = "테이블 삭제", description = "테이블의 컬럼 정의도 함께 삭제한다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
