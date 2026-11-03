package com.step.dictionary.pharmacy.inspection.dto;

import com.step.dictionary.pharmacy.dispensing.Dispensing;
import com.step.dictionary.pharmacy.inspection.Inspection;
import com.step.dictionary.pharmacy.inspection.InspectionResult;
import com.step.dictionary.pharmacy.prescription.Prescription;
import java.time.LocalDateTime;

/** 검수 목록 응답 (품목 제외). */
public record InspectionSummaryResponse(
        Long id,
        Long dispensingId,
        Long prescriptionId,
        String prescriptionNo,
        String patientNo,
        String patientName,
        InspectionResult result,
        String inspectedBy,
        LocalDateTime inspectedAt
) {

    /** 조제·처방은 목록 조회 쿼리에서 조인해 채워 둔다. */
    public static InspectionSummaryResponse from(Inspection inspection) {
        Dispensing dispensing = inspection.getDispensing();
        Prescription prescription = dispensing.getPrescription();
        return new InspectionSummaryResponse(
                inspection.getId(),
                dispensing.getId(),
                prescription.getId(),
                prescription.getPrescriptionNo(),
                prescription.getPatientNo(),
                prescription.getPatientName(),
                inspection.getResult(),
                inspection.getInspectedBy(),
                inspection.getInspectedAt());
    }
}
