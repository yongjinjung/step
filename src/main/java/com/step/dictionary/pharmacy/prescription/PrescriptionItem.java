package com.step.dictionary.pharmacy.prescription;

import com.step.dictionary.pharmacy.Quantities;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * 처방 품목 (처방의 하위). 한 행이 처방 약품 하나다.
 *
 * <p>테이블: {@code phm_prescription_item} (V12). {@link Prescription#addItem}으로만 만든다.
 * 약품 마스터가 아직 없어 약품코드·약품명을 그대로 저장한다.
 */
public class PrescriptionItem {

    private Long id;

    private Prescription prescription;

    /** 처방 안에서의 순번 (1부터). */
    private int lineNo;

    /** 약품코드 (대문자). */
    private String drugCode;

    private String drugName;

    /** 1회 투여량. */
    private BigDecimal dose;

    /** 투여 단위 (대문자). 예: MG */
    private String doseUnit;

    /** 투여 경로 (대문자). 예: PO */
    private String route;

    /** 투여 빈도 (대문자). 예: TID */
    private String frequency;

    /** 투여 일수. */
    private int days;

    /** 총 처방량. 조제 수량의 기준이다. */
    private BigDecimal totalQuantity;

    private String usageNote;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected PrescriptionItem() {
    }

    PrescriptionItem(Prescription prescription, int lineNo, String drugCode, String drugName, BigDecimal dose,
            String doseUnit, String route, String frequency, int days, BigDecimal totalQuantity, String usageNote) {
        this.prescription = prescription;
        this.lineNo = lineNo;
        // 코드성 값은 대문자로 맞춘다 (mg와 MG를 같게 본다).
        this.drugCode = upper(drugCode);
        this.drugName = drugName;
        this.dose = Quantities.normalize(dose);
        this.doseUnit = upper(doseUnit);
        this.route = upper(route);
        this.frequency = upper(frequency);
        this.days = days;
        this.totalQuantity = Quantities.normalize(totalQuantity);
        this.usageNote = usageNote;
    }

    private static String upper(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /** 조회 후 소속 Prescription를 연결한다. */
    void attachTo(Prescription parent) {
        this.prescription = parent;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public int getLineNo() {
        return lineNo;
    }

    public String getDrugCode() {
        return drugCode;
    }

    public String getDrugName() {
        return drugName;
    }

    public BigDecimal getDose() {
        return dose;
    }

    public String getDoseUnit() {
        return doseUnit;
    }

    public String getRoute() {
        return route;
    }

    public String getFrequency() {
        return frequency;
    }

    public int getDays() {
        return days;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public String getUsageNote() {
        return usageNote;
    }
}
