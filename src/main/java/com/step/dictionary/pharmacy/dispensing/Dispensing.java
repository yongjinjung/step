package com.step.dictionary.pharmacy.dispensing;

import com.step.dictionary.global.entity.BaseTimeEntity;
import com.step.dictionary.pharmacy.prescription.Prescription;
import com.step.dictionary.pharmacy.prescription.PrescriptionItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 조제 마스터. 처방 하나를 조제한 기록이며 조제 품목({@link DispensingItem})을 가진다.
 *
 * <p>테이블: {@code phm_dispensing} (V12). 처방 하나에 조제가 여러 번 있을 수 있지만
 * 검수 불합격(REJECTED)이 아닌 조제는 하나뿐이다({@code DispensingService}에서 검사).
 */
public class Dispensing extends BaseTimeEntity {

    private Long id;

    private Prescription prescription;

    private DispensingStatus status;

    /** 조제자 아이디 (로그인 사용자). */
    private String dispensedBy;

    private LocalDateTime dispensedAt;

    private String remark;

    /** 조제 품목 (ID 순 = 처방 품목 순으로 저장된다). */
    private List<DispensingItem> items = new ArrayList<>();

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected Dispensing() {
    }

    /** 새 조제를 만든다. 상태는 DISPENSED다. */
    public Dispensing(Prescription prescription, String dispensedBy, LocalDateTime dispensedAt, String remark) {
        this.prescription = prescription;
        this.dispensedBy = dispensedBy;
        this.dispensedAt = dispensedAt;
        this.remark = remark;
        this.status = DispensingStatus.DISPENSED;
    }

    /** 품목을 붙인다. 처방 품목이 이 처방의 것인지는 서비스가 먼저 확인한다. */
    public DispensingItem addItem(PrescriptionItem prescriptionItem, BigDecimal quantity, String lotNo,
            LocalDate expiryDate) {
        DispensingItem item = new DispensingItem(this, prescriptionItem, quantity, lotNo, expiryDate);
        items.add(item);
        return item;
    }

    /** 검수를 등록할 수 있는가 (조제 상태에서만). */
    public boolean isInspectable() {
        return status == DispensingStatus.DISPENSED;
    }

    /** 검수 결과를 반영한다. 합격이면 INSPECTED, 불합격이면 REJECTED. */
    public void applyInspection(boolean passed) {
        this.status = passed ? DispensingStatus.INSPECTED : DispensingStatus.REJECTED;
    }

    /** 불출할 수 있는가 (검수 합격 상태에서만). */
    public boolean isIssuable() {
        return status == DispensingStatus.INSPECTED;
    }

    /** 불출되면 호출한다. */
    public void markIssued() {
        this.status = DispensingStatus.ISSUED;
    }

    /** 조회한 품목을 채운다. {@link DispensingRepository}만 호출한다. */
    void loadItems(List<DispensingItem> loaded) {
        loaded.forEach(item -> item.attachTo(this));
        this.items = new ArrayList<>(loaded);
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public DispensingStatus getStatus() {
        return status;
    }

    public String getDispensedBy() {
        return dispensedBy;
    }

    public LocalDateTime getDispensedAt() {
        return dispensedAt;
    }

    public String getRemark() {
        return remark;
    }

    /** 품목 목록 (읽기 전용). */
    public List<DispensingItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
