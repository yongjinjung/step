package com.step.dictionary.pharmacy.inspection;

import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.pharmacy.inspection.dto.InspectionRequest;
import com.step.dictionary.pharmacy.inspection.dto.InspectionResponse;
import com.step.dictionary.pharmacy.inspection.dto.InspectionSearchCondition;
import com.step.dictionary.pharmacy.inspection.dto.InspectionSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 검수 REST API ({@code /api/inspections}). 권한: 검수 메뉴({@code /inspections})의 권한.
 */
@Tag(name = "검수", description = "조제 검수 등록과 조회")
@RestController
@RequestMapping("/api/inspections")
public class InspectionController {

    private final InspectionService inspectionService;

    public InspectionController(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    /** 검수 등록 (품목 포함). 검수자는 로그인 사용자다. 201 Created + Location 헤더. */
    @Operation(summary = "검수 등록",
            description = "조제 품목마다 결과를 하나씩 보낸다. 모두 합격이면 조제가 불출 가능, 하나라도 불합격이면 재조제 대상이 된다.")
    @PostMapping
    public ResponseEntity<InspectionResponse> create(@Valid @RequestBody InspectionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        InspectionResponse response = inspectionService.create(request, jwt.getSubject());
        return ResponseEntity.created(URI.create("/api/inspections/" + response.id())).body(response);
    }

    /** 검수 단건 조회 (품목 포함). */
    @Operation(summary = "검수 단건 조회")
    @GetMapping("/{id}")
    public InspectionResponse findById(@PathVariable Long id) {
        return inspectionService.findById(id);
    }

    /** 검수 검색. 기본 20건, 검수 일시 최신순. */
    @Operation(summary = "검수 검색", description = "keyword는 처방전번호·환자번호·환자명 부분 일치. from/to는 검수일(yyyy-MM-dd).")
    @GetMapping
    public PageResponse<InspectionSummaryResponse> search(
            @ParameterObject InspectionSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return inspectionService.search(condition, pageable);
    }
}
