package com.step.dictionary.pharmacy.dispensing.dto;

import com.step.dictionary.pharmacy.dispensing.DispensingItem;
import com.step.dictionary.pharmacy.prescription.PrescriptionItem;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 조제 품목 응답. 어떤 약인지 바로 보이도록 처방 품목의 순번·약품을 함께 담는다.
 *
 * @param prescribedQuantity 처방 품목의 총 처방량 (조제 수량과 비교용)
 */
public record DispensingItemResponse(
        Long id,
        Long prescriptionItemId,
        int lineNo,
        String drugCode,
        String drugName,
        BigDecimal prescribedQuantity,
        BigDecimal quantity,
        String lotNo,
        LocalDate expiryDate
) {

    public static DispensingItemResponse from(DispensingItem item) {
        PrescriptionItem prescriptionItem = item.getPrescriptionItem();
        return new DispensingItemResponse(
                item.getId(),
                prescriptionItem.getId(),
                prescriptionItem.getLineNo(),
                prescriptionItem.getDrugCode(),
                prescriptionItem.getDrugName(),
                prescriptionItem.getTotalQuantity(),
                item.getQuantity(),
                item.getLotNo(),
                item.getExpiryDate());
    }
}
