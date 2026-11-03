package com.step.dictionary.pharmacy.prescription;

import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.pharmacy.prescription.dto.PrescriptionCancelRequest;
import com.step.dictionary.pharmacy.prescription.dto.PrescriptionRequest;
import com.step.dictionary.pharmacy.prescription.dto.PrescriptionResponse;
import com.step.dictionary.pharmacy.prescription.dto.PrescriptionSearchCondition;
import com.step.dictionary.pharmacy.prescription.dto.PrescriptionSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 처방 REST API ({@code /api/prescriptions}).
 * 권한: 처방 메뉴({@code /prescriptions})의 권한. 취소는 수정 권한이다 ({@code ApiPermissionRules}).
 */
@Tag(name = "처방", description = "처방 마스터·품목 등록, 조회, 취소")
@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    /** 처방 등록 (품목 포함). 201 Created + Location 헤더. */
    @Operation(summary = "처방 등록", description = "품목 순번은 요청 순서대로 매긴다. 처방전번호는 대문자로 저장한다.")
    @PostMapping
    public ResponseEntity<PrescriptionResponse> create(@Valid @RequestBody PrescriptionRequest request) {
        PrescriptionResponse response = prescriptionService.create(request);
        return ResponseEntity.created(URI.create("/api/prescriptions/" + response.id())).body(response);
    }

    /** 처방 단건 조회 (품목 포함). */
    @Operation(summary = "처방 단건 조회")
    @GetMapping("/{id}")
    public PrescriptionResponse findById(@PathVariable Long id) {
        return prescriptionService.findById(id);
    }

    /** 처방 검색. 기본 20건, 처방 일시 최신순. */
    @Operation(summary = "처방 검색", description = "keyword는 처방전번호·환자번호·환자명 부분 일치. from/to는 처방일(yyyy-MM-dd).")
    @GetMapping
    public PageResponse<PrescriptionSummaryResponse> search(
            @ParameterObject PrescriptionSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return prescriptionService.search(condition, pageable);
    }

    /** 처방 취소. 조제 전에만 할 수 있다. */
    @Operation(summary = "처방 취소", description = "조제 전(ORDERED) 처방만 취소할 수 있다.")
    @PostMapping("/{id}/cancel")
    public PrescriptionResponse cancel(@PathVariable Long id, @Valid @RequestBody PrescriptionCancelRequest request) {
        return prescriptionService.cancel(id, request);
    }
}
