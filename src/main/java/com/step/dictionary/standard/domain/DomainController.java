package com.step.dictionary.standard.domain;

import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.standard.domain.dto.DomainRequest;
import com.step.dictionary.standard.domain.dto.DomainResponse;
import com.step.dictionary.standard.domain.dto.DomainSearchCondition;
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
 * 표준 도메인 REST API ({@code /api/domains}).
 * 업무 규칙은 {@link DomainService}에 있고, 이 클래스는 요청 검증과 HTTP 응답 형태만 담당한다.
 */
@Tag(name = "표준 도메인", description = "표준 도메인 관리")
@RestController
@RequestMapping("/api/domains")
public class DomainController {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    /** 도메인 등록. 201 Created + Location 헤더. */
    @Operation(summary = "도메인 등록", description = "분류어 단어에만 연결할 수 있다. 길이/소수점은 데이터 타입 규칙을 따른다.")
    @PostMapping
    public ResponseEntity<DomainResponse> create(@Valid @RequestBody DomainRequest request) {
        DomainResponse response = domainService.create(request);
        return ResponseEntity.created(URI.create("/api/domains/" + response.id())).body(response);
    }

    /** 도메인 단건 조회. 분류어 단어 ID/이름을 포함한다. */
    @Operation(summary = "도메인 단건 조회")
    @GetMapping("/{id}")
    public DomainResponse findById(@PathVariable Long id) {
        return domainService.findById(id);
    }

    /** 도메인 검색 ({@code ?keyword=&classifierWordId=&dataType=&page=&size=}). 기본 20건. */
    @Operation(summary = "도메인 검색", description = "keyword는 도메인명에 부분 일치. 도메인명 오름차순 정렬.")
    @GetMapping
    public PageResponse<DomainResponse> search(
            @ParameterObject DomainSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return domainService.search(condition, pageable);
    }

    /** 도메인 수정 (전체 교체). */
    @Operation(summary = "도메인 수정")
    @PutMapping("/{id}")
    public DomainResponse update(@PathVariable Long id, @Valid @RequestBody DomainRequest request) {
        return domainService.update(id, request);
    }

    /** 도메인 삭제. 성공 시 204, 용어에서 사용 중이면 409. */
    @Operation(summary = "도메인 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        domainService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
