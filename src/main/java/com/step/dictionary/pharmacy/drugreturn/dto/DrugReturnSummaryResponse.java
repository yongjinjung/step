package com.step.dictionary.pharmacy.drugreturn.dto;

import com.step.dictionary.pharmacy.drugreturn.DrugReturn;
import com.step.dictionary.pharmacy.issue.Issue;
import com.step.dictionary.pharmacy.prescription.Prescription;
import java.time.LocalDateTime;

/** 반납 목록 응답 (품목 제외). */
public record DrugReturnSummaryResponse(
        Long id,
        Long issueId,
        Long prescriptionId,
        String prescriptionNo,
        String patientNo,
        String patientName,
        String destinationName,
        String reason,
        String returnedBy,
        LocalDateTime returnedAt
) {

    /** 불출·조제·처방은 목록 조회 쿼리에서 조인해 채워 둔다. */
    public static DrugReturnSummaryResponse from(DrugReturn drugReturn) {
        Issue issue = drugReturn.getIssue();
        Prescription prescription = issue.getDispensing().getPrescription();
        return new DrugReturnSummaryResponse(
                drugReturn.getId(),
                issue.getId(),
                prescription.getId(),
                prescription.getPrescriptionNo(),
                prescription.getPatientNo(),
                prescription.getPatientName(),
                issue.getDestinationName(),
                drugReturn.getReason(),
                drugReturn.getReturnedBy(),
                drugReturn.getReturnedAt());
    }
}
