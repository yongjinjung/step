package com.step.dictionary.pharmacy.prescription.dto;

import com.step.dictionary.pharmacy.prescription.PrescriptionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 처방 등록 요청. 처방은 수정하지 않으므로 등록에만 쓴다.
 *
 * @param prescriptionNo   처방전번호. 필수, 최대 30자, 유일 (저장 시 대문자)
 * @param patientNo        환자번호. 필수, 최대 20자
 * @param patientName      환자명. 필수, 최대 100자
 * @param departmentCode   진료과 코드. 필수, 최대 20자 (저장 시 대문자)
 * @param prescriberName   처방의 이름. 필수, 최대 100자
 * @param prescriptionType 처방 구분
 * @param prescribedAt     처방 일시
 * @param remark           비고. 선택, 최대 1000자
 * @param items            처방 품목. 1~100개, 순서대로 순번이 매겨진다
 */
public record PrescriptionRequest(
        @NotBlank @Size(max = 30) String prescriptionNo,
        @NotBlank @Size(max = 20) String patientNo,
        @NotBlank @Size(max = 100) String patientName,
        @NotBlank @Size(max = 20) String departmentCode,
        @NotBlank @Size(max = 100) String prescriberName,
        @NotNull PrescriptionType prescriptionType,
        @NotNull LocalDateTime prescribedAt,
        @Size(max = 1000) String remark,
        @NotEmpty @Size(max = 100) List<@Valid @NotNull PrescriptionItemRequest> items
) {
}
