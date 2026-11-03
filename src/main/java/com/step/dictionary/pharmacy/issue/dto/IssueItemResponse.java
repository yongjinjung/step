package com.step.dictionary.pharmacy.issue.dto;

import com.step.dictionary.pharmacy.dispensing.DispensingItem;
import com.step.dictionary.pharmacy.issue.IssueItem;
import com.step.dictionary.pharmacy.prescription.PrescriptionItem;
import java.math.BigDecimal;

/**
 * 불출 품목 응답.
 *
 * @param quantity         불출 수량
 * @param returnedQuantity 지금까지 반납한 수량 합계 (반납 가능 수량 = quantity - returnedQuantity)
 */
public record IssueItemResponse(
        Long id,
        Long dispensingItemId,
        int lineNo,
        String drugCode,
        String drugName,
        String lotNo,
        BigDecimal quantity,
        BigDecimal returnedQuantity
) {

    public static IssueItemResponse from(IssueItem item, BigDecimal returnedQuantity) {
        DispensingItem dispensingItem = item.getDispensingItem();
        PrescriptionItem prescriptionItem = dispensingItem.getPrescriptionItem();
        return new IssueItemResponse(
                item.getId(),
                dispensingItem.getId(),
                prescriptionItem.getLineNo(),
                prescriptionItem.getDrugCode(),
                prescriptionItem.getDrugName(),
                dispensingItem.getLotNo(),
                item.getQuantity(),
                returnedQuantity);
    }
}
