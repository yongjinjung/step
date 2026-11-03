package com.step.dictionary.pharmacy.prescription.dto;

import com.step.dictionary.pharmacy.prescription.PrescriptionItem;
import java.math.BigDecimal;

/** 처방 품목 응답. 필드 설명은 {@link PrescriptionItemRequest}와 같다. */
public record PrescriptionItemResponse(
        Long id,
        int lineNo,
        String drugCode,
        String drugName,
        BigDecimal dose,
        String doseUnit,
        String route,
        String frequency,
        int days,
        BigDecimal totalQuantity,
        String usageNote
) {

    public static PrescriptionItemResponse from(PrescriptionItem item) {
        return new PrescriptionItemResponse(
                item.getId(),
                item.getLineNo(),
                item.getDrugCode(),
                item.getDrugName(),
                item.getDose(),
                item.getDoseUnit(),
                item.getRoute(),
                item.getFrequency(),
                item.getDays(),
                item.getTotalQuantity(),
                item.getUsageNote());
    }
}
