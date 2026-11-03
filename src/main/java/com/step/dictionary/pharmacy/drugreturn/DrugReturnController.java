package com.step.dictionary.pharmacy.drugreturn;

import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.pharmacy.drugreturn.dto.DrugReturnRequest;
import com.step.dictionary.pharmacy.drugreturn.dto.DrugReturnResponse;
import com.step.dictionary.pharmacy.drugreturn.dto.DrugReturnSearchCondition;
import com.step.dictionary.pharmacy.drugreturn.dto.DrugReturnSummaryResponse;
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
 * 반납 REST API ({@code /api/returns}). 권한: 반납 메뉴({@code /returns})의 권한.
 */
@Tag(name = "반납", description = "불출된 약의 반납 등록과 조회")
@RestController
@RequestMapping("/api/returns")
public class DrugReturnController {

    private final DrugReturnService returnService;

    public DrugReturnController(DrugReturnService returnService) {
        this.returnService = returnService;
    }

    /** 반납 등록 (품목 포함). 반납자는 로그인 사용자다. 201 Created + Location 헤더. */
    @Operation(summary = "반납 등록", description = "불출 품목 일부만 반납할 수 있다. 품목별 반납 합계는 불출 수량을 넘을 수 없다.")
    @PostMapping
    public ResponseEntity<DrugReturnResponse> create(@Valid @RequestBody DrugReturnRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        DrugReturnResponse response = returnService.create(request, jwt.getSubject());
        return ResponseEntity.created(URI.create("/api/returns/" + response.id())).body(response);
    }

    /** 반납 단건 조회 (품목 포함). */
    @Operation(summary = "반납 단건 조회")
    @GetMapping("/{id}")
    public DrugReturnResponse findById(@PathVariable Long id) {
        return returnService.findById(id);
    }

    /** 반납 검색. 기본 20건, 반납 일시 최신순. */
    @Operation(summary = "반납 검색", description = "keyword는 처방전번호·환자번호·환자명 부분 일치. from/to는 반납일(yyyy-MM-dd).")
    @GetMapping
    public PageResponse<DrugReturnSummaryResponse> search(
            @ParameterObject DrugReturnSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return returnService.search(condition, pageable);
    }
}
