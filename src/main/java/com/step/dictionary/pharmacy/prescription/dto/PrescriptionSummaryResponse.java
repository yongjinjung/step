package com.step.dictionary.pharmacy.prescription.dto;

import com.step.dictionary.pharmacy.prescription.Prescription;
import com.step.dictionary.pharmacy.prescription.PrescriptionStatus;
import com.step.dictionary.pharmacy.prescription.PrescriptionType;
import java.time.LocalDateTime;

/** 처방 목록 응답 (품목 제외). 품목은 단건 조회({@link PrescriptionResponse})에서 본다. */
public record PrescriptionSummaryResponse(
        Long id,
        String prescriptionNo,
        String patientNo,
        String patientName,
        String departmentCode,
        String prescriberName,
        PrescriptionType prescriptionType,
        PrescriptionStatus status,
        LocalDateTime prescribedAt,
        LocalDateTime createdAt
) {

    public static PrescriptionSummaryResponse from(Prescription prescription) {
        return new PrescriptionSummaryResponse(
                prescription.getId(),
                prescription.getPrescriptionNo(),
                prescription.getPatientNo(),
                prescription.getPatientName(),
                prescription.getDepartmentCode(),
                prescription.getPrescriberName(),
                prescription.getPrescriptionType(),
                prescription.getStatus(),
                prescription.getPrescribedAt(),
                prescription.getCreatedAt());
    }
}
