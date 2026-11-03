package com.step.dictionary.pharmacy.prescription.dto;

import com.step.dictionary.pharmacy.prescription.Prescription;
import com.step.dictionary.pharmacy.prescription.PrescriptionStatus;
import com.step.dictionary.pharmacy.prescription.PrescriptionType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 처방 단건 응답 (품목 포함).
 *
 * @param cancelledAt  취소 일시 (취소한 처방만)
 * @param cancelReason 취소 사유 (취소한 처방만)
 * @param items        처방 품목 (순번 순)
 */
public record PrescriptionResponse(
        Long id,
        String prescriptionNo,
        String patientNo,
        String patientName,
        String departmentCode,
        String prescriberName,
        PrescriptionType prescriptionType,
        PrescriptionStatus status,
        LocalDateTime prescribedAt,
        String remark,
        LocalDateTime cancelledAt,
        String cancelReason,
        List<PrescriptionItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 엔티티를 응답으로 변환한다. 품목을 읽으므로 트랜잭션 안에서 호출한다. */
    public static PrescriptionResponse from(Prescription prescription) {
        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getPrescriptionNo(),
                prescription.getPatientNo(),
                prescription.getPatientName(),
                prescription.getDepartmentCode(),
                prescription.getPrescriberName(),
                prescription.getPrescriptionType(),
                prescription.getStatus(),
                prescription.getPrescribedAt(),
                prescription.getRemark(),
                prescription.getCancelledAt(),
                prescription.getCancelReason(),
                prescription.getItems().stream().map(PrescriptionItemResponse::from).toList(),
                prescription.getCreatedAt(),
                prescription.getUpdatedAt());
    }
}
