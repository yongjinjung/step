package com.step.dictionary.pharmacy.issue;

import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.pharmacy.issue.dto.IssueRequest;
import com.step.dictionary.pharmacy.issue.dto.IssueResponse;
import com.step.dictionary.pharmacy.issue.dto.IssueSearchCondition;
import com.step.dictionary.pharmacy.issue.dto.IssueSummaryResponse;
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
 * 불출 REST API ({@code /api/issues}). 권한: 불출 메뉴({@code /issues})의 권한.
 */
@Tag(name = "불출", description = "검수 합격한 조제의 불출 등록과 조회")
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    /** 불출 등록. 조제 품목을 그대로 불출한다. 불출자는 로그인 사용자다. 201 Created + Location 헤더. */
    @Operation(summary = "불출 등록", description = "검수 합격한 조제만 불출할 수 있다. 품목은 조제 품목을 수량 그대로 옮긴다.")
    @PostMapping
    public ResponseEntity<IssueResponse> create(@Valid @RequestBody IssueRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        IssueResponse response = issueService.create(request, jwt.getSubject());
        return ResponseEntity.created(URI.create("/api/issues/" + response.id())).body(response);
    }

    /** 불출 단건 조회 (품목과 품목별 반납 수량 포함). */
    @Operation(summary = "불출 단건 조회", description = "품목마다 지금까지 반납한 수량(returnedQuantity)을 함께 준다.")
    @GetMapping("/{id}")
    public IssueResponse findById(@PathVariable Long id) {
        return issueService.findById(id);
    }

    /** 불출 검색. 기본 20건, 불출 일시 최신순. */
    @Operation(summary = "불출 검색", description = "keyword는 처방전번호·환자번호·환자명 부분 일치. from/to는 불출일(yyyy-MM-dd).")
    @GetMapping
    public PageResponse<IssueSummaryResponse> search(
            @ParameterObject IssueSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return issueService.search(condition, pageable);
    }
}
