package com.step.dictionary.pharmacy.inspection.dto;

import com.step.dictionary.pharmacy.dispensing.Dispensing;
import com.step.dictionary.pharmacy.inspection.Inspection;
import com.step.dictionary.pharmacy.inspection.InspectionResult;
import com.step.dictionary.pharmacy.prescription.Prescription;
import java.time.LocalDateTime;
import java.util.List;

/** 검수 단건 응답 (품목 포함). */
public record InspectionResponse(
        Long id,
        Long dispensingId,
        Long prescriptionId,
        String prescriptionNo,
        String patientNo,
        String patientName,
        InspectionResult result,
        String inspectedBy,
        LocalDateTime inspectedAt,
        String remark,
        List<InspectionItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 조제·처방·품목을 읽으므로 트랜잭션 안에서 호출한다. */
    public static InspectionResponse from(Inspection inspection) {
        Dispensing dispensing = inspection.getDispensing();
        Prescription prescription = dispensing.getPrescription();
        return new InspectionResponse(
                inspection.getId(),
                dispensing.getId(),
                prescription.getId(),
                prescription.getPrescriptionNo(),
                prescription.getPatientNo(),
                prescription.getPatientName(),
                inspection.getResult(),
                inspection.getInspectedBy(),
                inspection.getInspectedAt(),
                inspection.getRemark(),
                inspection.getItems().stream().map(InspectionItemResponse::from).toList(),
                inspection.getCreatedAt(),
                inspection.getUpdatedAt());
    }
}
