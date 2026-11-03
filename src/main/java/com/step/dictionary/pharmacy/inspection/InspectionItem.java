package com.step.dictionary.pharmacy.inspection;

import com.step.dictionary.pharmacy.dispensing.DispensingItem;

/**
 * 검수 품목 (검수의 하위). 조제 품목 하나의 검수 결과다.
 *
 * <p>테이블: {@code phm_inspection_item} (V12). {@link Inspection#addItem}으로만 만든다.
 */
public class InspectionItem {

    private Long id;

    private Inspection inspection;

    private DispensingItem dispensingItem;

    private InspectionResult result;

    /** 불량 사유. 불합격이면 필수(서비스에서 검사), 합격이면 저장하지 않는다. */
    private String defectReason;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected InspectionItem() {
    }

    InspectionItem(Inspection inspection, DispensingItem dispensingItem, InspectionResult result,
            String defectReason) {
        this.inspection = inspection;
        this.dispensingItem = dispensingItem;
        this.result = result;
        this.defectReason = result == InspectionResult.FAIL ? defectReason : null;
    }

    /** 조회 후 소속 Inspection를 연결한다. */
    void attachTo(Inspection parent) {
        this.inspection = parent;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public Inspection getInspection() {
        return inspection;
    }

    public DispensingItem getDispensingItem() {
        return dispensingItem;
    }

    public InspectionResult getResult() {
        return result;
    }

    public String getDefectReason() {
        return defectReason;
    }
}
