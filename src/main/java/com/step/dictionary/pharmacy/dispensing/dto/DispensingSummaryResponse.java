package com.step.dictionary.pharmacy.dispensing.dto;

import com.step.dictionary.pharmacy.dispensing.Dispensing;
import com.step.dictionary.pharmacy.dispensing.DispensingStatus;
import com.step.dictionary.pharmacy.prescription.Prescription;
import java.time.LocalDateTime;

/** 조제 목록 응답 (품목 제외). 처방전번호·환자를 함께 담아 목록에서 바로 알아볼 수 있게 한다. */
public record DispensingSummaryResponse(
        Long id,
        Long prescriptionId,
        String prescriptionNo,
        String patientNo,
        String patientName,
        DispensingStatus status,
        String dispensedBy,
        LocalDateTime dispensedAt
) {

    /** 처방은 목록 조회 쿼리에서 조인해 채워 둔다. */
    public static DispensingSummaryResponse from(Dispensing dispensing) {
        Prescription prescription = dispensing.getPrescription();
        return new DispensingSummaryResponse(
                dispensing.getId(),
                prescription.getId(),
                prescription.getPrescriptionNo(),
                prescription.getPatientNo(),
                prescription.getPatientName(),
                dispensing.getStatus(),
                dispensing.getDispensedBy(),
                dispensing.getDispensedAt());
    }
}
