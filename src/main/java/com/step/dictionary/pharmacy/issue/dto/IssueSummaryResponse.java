package com.step.dictionary.pharmacy.issue.dto;

import com.step.dictionary.pharmacy.dispensing.Dispensing;
import com.step.dictionary.pharmacy.issue.Issue;
import com.step.dictionary.pharmacy.prescription.Prescription;
import java.time.LocalDateTime;

/** 불출 목록 응답 (품목 제외). */
public record IssueSummaryResponse(
        Long id,
        Long dispensingId,
        Long prescriptionId,
        String prescriptionNo,
        String patientNo,
        String patientName,
        String destinationCode,
        String destinationName,
        String receiverName,
        String issuedBy,
        LocalDateTime issuedAt
) {

    /** 조제·처방은 목록 조회 쿼리에서 조인해 채워 둔다. */
    public static IssueSummaryResponse from(Issue issue) {
        Dispensing dispensing = issue.getDispensing();
        Prescription prescription = dispensing.getPrescription();
        return new IssueSummaryResponse(
                issue.getId(),
                dispensing.getId(),
                prescription.getId(),
                prescription.getPrescriptionNo(),
                prescription.getPatientNo(),
                prescription.getPatientName(),
                issue.getDestinationCode(),
                issue.getDestinationName(),
                issue.getReceiverName(),
                issue.getIssuedBy(),
                issue.getIssuedAt());
    }
}
