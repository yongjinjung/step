package com.step.dictionary.pharmacy.drugreturn.dto;

import com.step.dictionary.pharmacy.drugreturn.DrugReturn;
import com.step.dictionary.pharmacy.issue.Issue;
import com.step.dictionary.pharmacy.prescription.Prescription;
import java.time.LocalDateTime;
import java.util.List;

/** 반납 단건 응답 (품목 포함). */
public record DrugReturnResponse(
        Long id,
        Long issueId,
        Long prescriptionId,
        String prescriptionNo,
        String patientNo,
        String patientName,
        String destinationName,
        String reason,
        String returnedBy,
        LocalDateTime returnedAt,
        String remark,
        List<DrugReturnItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 불출·조제·처방·품목을 읽으므로 트랜잭션 안에서 호출한다. */
    public static DrugReturnResponse from(DrugReturn drugReturn) {
        Issue issue = drugReturn.getIssue();
        Prescription prescription = issue.getDispensing().getPrescription();
        return new DrugReturnResponse(
                drugReturn.getId(),
                issue.getId(),
                prescription.getId(),
                prescription.getPrescriptionNo(),
                prescription.getPatientNo(),
                prescription.getPatientName(),
                issue.getDestinationName(),
                drugReturn.getReason(),
                drugReturn.getReturnedBy(),
                drugReturn.getReturnedAt(),
                drugReturn.getRemark(),
                drugReturn.getItems().stream().map(DrugReturnItemResponse::from).toList(),
                drugReturn.getCreatedAt(),
                drugReturn.getUpdatedAt());
    }
}
