package com.step.dictionary.pharmacy.drugreturn.dto;

import com.step.dictionary.pharmacy.drugreturn.DrugReturnItem;
import com.step.dictionary.pharmacy.drugreturn.ReturnDisposition;
import com.step.dictionary.pharmacy.issue.IssueItem;
import com.step.dictionary.pharmacy.prescription.PrescriptionItem;
import java.math.BigDecimal;

/**
 * 반납 품목 응답.
 *
 * @param issuedQuantity 불출 수량 (반납 수량과 비교용)
 * @param quantity       이번 반납 수량
 */
public record DrugReturnItemResponse(
        Long id,
        Long issueItemId,
        int lineNo,
        String drugCode,
        String drugName,
        BigDecimal issuedQuantity,
        BigDecimal quantity,
        ReturnDisposition disposition
) {

    public static DrugReturnItemResponse from(DrugReturnItem item) {
        IssueItem issueItem = item.getIssueItem();
        PrescriptionItem prescriptionItem = issueItem.getDispensingItem().getPrescriptionItem();
        return new DrugReturnItemResponse(
                item.getId(),
                issueItem.getId(),
                prescriptionItem.getLineNo(),
                prescriptionItem.getDrugCode(),
                prescriptionItem.getDrugName(),
                issueItem.getQuantity(),
                item.getQuantity(),
                item.getDisposition());
    }
}
