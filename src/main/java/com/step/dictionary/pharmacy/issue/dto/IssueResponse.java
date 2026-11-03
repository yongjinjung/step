package com.step.dictionary.pharmacy.issue.dto;

import com.step.dictionary.pharmacy.Quantities;
import com.step.dictionary.pharmacy.dispensing.Dispensing;
import com.step.dictionary.pharmacy.issue.Issue;
import com.step.dictionary.pharmacy.prescription.Prescription;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 불출 단건 응답 (품목과 품목별 반납 수량 포함). */
public record IssueResponse(
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
        LocalDateTime issuedAt,
        String remark,
        List<IssueItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 엔티티를 응답으로 변환한다. 조제·처방·품목을 읽으므로 트랜잭션 안에서 호출한다.
     *
     * @param returnedByItemId 불출 품목 ID → 반납 수량 합계. 반납이 없는 품목은 0으로 본다
     */
    public static IssueResponse from(Issue issue, Map<Long, BigDecimal> returnedByItemId) {
        Dispensing dispensing = issue.getDispensing();
        Prescription prescription = dispensing.getPrescription();
        return new IssueResponse(
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
                issue.getIssuedAt(),
                issue.getRemark(),
                issue.getItems().stream()
                        .map(item -> IssueItemResponse.from(item,
                                returnedByItemId.getOrDefault(item.getId(), Quantities.ZERO)))
                        .toList(),
                issue.getCreatedAt(),
                issue.getUpdatedAt());
    }
}
