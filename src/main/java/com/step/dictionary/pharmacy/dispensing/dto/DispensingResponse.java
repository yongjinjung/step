package com.step.dictionary.pharmacy.dispensing.dto;

import com.step.dictionary.pharmacy.dispensing.Dispensing;
import com.step.dictionary.pharmacy.dispensing.DispensingStatus;
import com.step.dictionary.pharmacy.prescription.Prescription;
import java.time.LocalDateTime;
import java.util.List;

/** 조제 단건 응답 (품목 포함). */
public record DispensingResponse(
        Long id,
        Long prescriptionId,
        String prescriptionNo,
        String patientNo,
        String patientName,
        DispensingStatus status,
        String dispensedBy,
        LocalDateTime dispensedAt,
        String remark,
        List<DispensingItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 처방과 품목을 읽으므로 트랜잭션 안에서 호출한다. */
    public static DispensingResponse from(Dispensing dispensing) {
        Prescription prescription = dispensing.getPrescription();
        return new DispensingResponse(
                dispensing.getId(),
                prescription.getId(),
                prescription.getPrescriptionNo(),
                prescription.getPatientNo(),
                prescription.getPatientName(),
                dispensing.getStatus(),
                dispensing.getDispensedBy(),
                dispensing.getDispensedAt(),
                dispensing.getRemark(),
                dispensing.getItems().stream().map(DispensingItemResponse::from).toList(),
                dispensing.getCreatedAt(),
                dispensing.getUpdatedAt());
    }
}
