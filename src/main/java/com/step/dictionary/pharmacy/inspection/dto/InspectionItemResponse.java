package com.step.dictionary.pharmacy.inspection.dto;

import com.step.dictionary.pharmacy.dispensing.DispensingItem;
import com.step.dictionary.pharmacy.inspection.InspectionItem;
import com.step.dictionary.pharmacy.inspection.InspectionResult;
import com.step.dictionary.pharmacy.prescription.PrescriptionItem;
import java.math.BigDecimal;

/**
 * 검수 품목 응답. 어떤 약인지 바로 보이도록 처방 순번·약품과 조제 수량·로트를 함께 담는다.
 *
 * @param quantity 조제 수량
 */
public record InspectionItemResponse(
        Long id,
        Long dispensingItemId,
        int lineNo,
        String drugCode,
        String drugName,
        BigDecimal quantity,
        String lotNo,
        InspectionResult result,
        String defectReason
) {

    public static InspectionItemResponse from(InspectionItem item) {
        DispensingItem dispensingItem = item.getDispensingItem();
        PrescriptionItem prescriptionItem = dispensingItem.getPrescriptionItem();
        return new InspectionItemResponse(
                item.getId(),
                dispensingItem.getId(),
                prescriptionItem.getLineNo(),
                prescriptionItem.getDrugCode(),
                prescriptionItem.getDrugName(),
                dispensingItem.getQuantity(),
                dispensingItem.getLotNo(),
                item.getResult(),
                item.getDefectReason());
    }
}
