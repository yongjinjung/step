package com.step.dictionary.pharmacy.dispensing;

import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.pharmacy.dispensing.dto.DispensingRequest;
import com.step.dictionary.pharmacy.dispensing.dto.DispensingResponse;
import com.step.dictionary.pharmacy.dispensing.dto.DispensingSearchCondition;
import com.step.dictionary.pharmacy.dispensing.dto.DispensingSummaryResponse;
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
 * 조제 REST API ({@code /api/dispensings}). 권한: 조제 메뉴({@code /dispensings})의 권한.
 */
@Tag(name = "조제", description = "처방 조제 등록과 조회")
@RestController
@RequestMapping("/api/dispensings")
public class DispensingController {

    private final DispensingService dispensingService;

    public DispensingController(DispensingService dispensingService) {
        this.dispensingService = dispensingService;
    }

    /** 조제 등록 (품목 포함). 조제자는 로그인 사용자다. 201 Created + Location 헤더. */
    @Operation(summary = "조제 등록", description = "처방 품목마다 조제 품목을 하나씩 보낸다. 조제하면 처방은 취소할 수 없다.")
    @PostMapping
    public ResponseEntity<DispensingResponse> create(@Valid @RequestBody DispensingRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        DispensingResponse response = dispensingService.create(request, jwt.getSubject());
        return ResponseEntity.created(URI.create("/api/dispensings/" + response.id())).body(response);
    }

    /** 조제 단건 조회 (품목 포함). */
    @Operation(summary = "조제 단건 조회")
    @GetMapping("/{id}")
    public DispensingResponse findById(@PathVariable Long id) {
        return dispensingService.findById(id);
    }

    /** 조제 검색. 기본 20건, 조제 일시 최신순. */
    @Operation(summary = "조제 검색", description = "keyword는 처방전번호·환자번호·환자명 부분 일치. from/to는 조제일(yyyy-MM-dd).")
    @GetMapping
    public PageResponse<DispensingSummaryResponse> search(
            @ParameterObject DispensingSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return dispensingService.search(condition, pageable);
    }
}
