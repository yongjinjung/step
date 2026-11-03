package com.step.dictionary.pharmacy.inspection;

import com.step.dictionary.global.entity.BaseTimeEntity;
import com.step.dictionary.pharmacy.dispensing.Dispensing;
import com.step.dictionary.pharmacy.dispensing.DispensingItem;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 검수 마스터. 조제 하나를 검수한 기록이며 검수 품목({@link InspectionItem})을 가진다.
 *
 * <p>테이블: {@code phm_inspection} (V12). 조제 하나에 검수 하나다(유일 제약).
 * 전체 결과는 품목 결과로 계산한다: 모두 합격이면 PASS, 하나라도 불합격이면 FAIL.
 */
public class Inspection extends BaseTimeEntity {

    private Long id;

    private Dispensing dispensing;

    private InspectionResult result;

    /** 검수자 아이디 (로그인 사용자). */
    private String inspectedBy;

    private LocalDateTime inspectedAt;

    private String remark;

    private List<InspectionItem> items = new ArrayList<>();

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected Inspection() {
    }

    /** 새 검수를 만든다. 품목을 붙이기 전까지 결과는 PASS다. */
    public Inspection(Dispensing dispensing, String inspectedBy, LocalDateTime inspectedAt, String remark) {
        this.dispensing = dispensing;
        this.inspectedBy = inspectedBy;
        this.inspectedAt = inspectedAt;
        this.remark = remark;
        this.result = InspectionResult.PASS;
    }

    /** 품목을 붙인다. 불합격 품목이 하나라도 있으면 전체 결과가 FAIL이 된다. */
    public InspectionItem addItem(DispensingItem dispensingItem, InspectionResult itemResult, String defectReason) {
        InspectionItem item = new InspectionItem(this, dispensingItem, itemResult, defectReason);
        items.add(item);
        if (itemResult == InspectionResult.FAIL) {
            this.result = InspectionResult.FAIL;
        }
        return item;
    }

    public boolean isPassed() {
        return result == InspectionResult.PASS;
    }

    /** 조회한 품목을 채운다. {@link InspectionRepository}만 호출한다. */
    void loadItems(List<InspectionItem> loaded) {
        loaded.forEach(item -> item.attachTo(this));
        this.items = new ArrayList<>(loaded);
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public Dispensing getDispensing() {
        return dispensing;
    }

    public InspectionResult getResult() {
        return result;
    }

    public String getInspectedBy() {
        return inspectedBy;
    }

    public LocalDateTime getInspectedAt() {
        return inspectedAt;
    }

    public String getRemark() {
        return remark;
    }

    /** 품목 목록 (읽기 전용). */
    public List<InspectionItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
