package com.step.dictionary.standard.term;

import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.standard.term.dto.TermRequest;
import com.step.dictionary.standard.term.dto.TermResponse;
import com.step.dictionary.standard.term.dto.TermSearchCondition;
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
 * 표준 용어 REST API ({@code /api/terms}).
 * 업무 규칙은 {@link TermService}에 있고, 이 클래스는 요청 검증과 HTTP 응답 형태만 담당한다.
 */
@Tag(name = "표준 용어", description = "표준 용어 관리")
@RestController
@RequestMapping("/api/terms")
public class TermController {

    private final TermService termService;

    public TermController(TermService termService) {
        this.termService = termService;
    }

    /** 용어 등록. 용어명/영문약어는 응답에서 확인한다. 201 Created + Location 헤더. */
    @Operation(summary = "용어 등록",
            description = "구성 단어 순서대로 용어명(단어명 연결)과 영문약어(`_` 연결)를 생성한다. "
                    + "마지막 단어는 분류어이고 도메인의 분류어와 같아야 한다.")
    @PostMapping
    public ResponseEntity<TermResponse> create(@Valid @RequestBody TermRequest request) {
        TermResponse response = termService.create(request);
        return ResponseEntity.created(URI.create("/api/terms/" + response.id())).body(response);
    }

    /** 용어 단건 조회. 구성 단어 목록과 도메인 타입/길이를 포함한다. */
    @Operation(summary = "용어 단건 조회")
    @GetMapping("/{id}")
    public TermResponse findById(@PathVariable Long id) {
        return termService.findById(id);
    }

    /** 용어 검색 ({@code ?keyword=&domainId=&wordId=&page=&size=}). 기본 20건. */
    @Operation(summary = "용어 검색", description = "keyword는 용어명/영문약어에 부분 일치. 용어명 오름차순 정렬.")
    @GetMapping
    public PageResponse<TermResponse> search(
            @ParameterObject TermSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return termService.search(condition, pageable);
    }

    /** 용어 수정 (전체 교체). */
    @Operation(summary = "용어 수정", description = "구성 단어를 바꾸면 용어명과 영문약어를 다시 생성한다.")
    @PutMapping("/{id}")
    public TermResponse update(@PathVariable Long id, @Valid @RequestBody TermRequest request) {
        return termService.update(id, request);
    }

    /** 용어 삭제. 성공 시 204, 컬럼에 매핑돼 있으면 409. */
    @Operation(summary = "용어 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        termService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
