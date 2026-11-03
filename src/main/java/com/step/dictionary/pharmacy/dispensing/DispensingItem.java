package com.step.dictionary.pharmacy.dispensing;

import com.step.dictionary.pharmacy.Quantities;
import com.step.dictionary.pharmacy.prescription.PrescriptionItem;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 조제 품목 (조제의 하위). 처방 품목 하나를 얼마나, 어느 로트로 조제했는지 남긴다.
 *
 * <p>테이블: {@code phm_dispensing_item} (V12). {@link Dispensing#addItem}으로만 만든다.
 */
public class DispensingItem {

    private Long id;

    private Dispensing dispensing;

    private PrescriptionItem prescriptionItem;

    /** 조제 수량. */
    private BigDecimal quantity;

    /** 제조번호(로트). 선택. */
    private String lotNo;

    /** 유효기간 만료일. 선택. */
    private LocalDate expiryDate;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected DispensingItem() {
    }

    DispensingItem(Dispensing dispensing, PrescriptionItem prescriptionItem, BigDecimal quantity, String lotNo,
            LocalDate expiryDate) {
        this.dispensing = dispensing;
        this.prescriptionItem = prescriptionItem;
        this.quantity = Quantities.normalize(quantity);
        this.lotNo = lotNo;
        this.expiryDate = expiryDate;
    }

    /** 조회 후 소속 Dispensing를 연결한다. */
    void attachTo(Dispensing parent) {
        this.dispensing = parent;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public Dispensing getDispensing() {
        return dispensing;
    }

    public PrescriptionItem getPrescriptionItem() {
        return prescriptionItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getLotNo() {
        return lotNo;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }
}
