package com.step.dictionary.pharmacy.prescription;

import com.step.dictionary.global.entity.BaseTimeEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 처방 마스터. 처방 품목({@link PrescriptionItem}) 여러 개를 가진다.
 *
 * <p>테이블: {@code phm_prescription} (V12). 처방전번호는 대문자로 저장하며 전체에서 유일하다.
 * 품목은 처방과 함께 저장되는 하위 데이터라 {@code PrescriptionRepository.save()}가 처방과 함께 넣는다.
 * 처방은 수정하지 않는다. 내용이 바뀌면 취소하고 새로 처방한다(조제 전까지만 취소 가능).
 */
public class Prescription extends BaseTimeEntity {

    private Long id;

    /** 처방전번호 (대문자, 유일). */
    private String prescriptionNo;

    private String patientNo;

    private String patientName;

    /** 처방 진료과 코드 (대문자). */
    private String departmentCode;

    private String prescriberName;
    private PrescriptionType prescriptionType;

    private PrescriptionStatus status;

    private LocalDateTime prescribedAt;

    private String remark;

    /** 취소 일시. 취소한 처방만 값이 있다. */
    private LocalDateTime cancelledAt;

    /** 취소 사유. 취소한 처방만 값이 있다. */
    private String cancelReason;

    /**
     * 처방 품목 (순번 순). 목록 조회에서는 읽지 않고 단건 조회에서만 읽는다.
     * 리포지토리가 처방을 읽은 뒤 품목을 따로 읽어 {@code loadItems()}로 채운다.
     */
    private List<PrescriptionItem> items = new ArrayList<>();

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected Prescription() {
    }

    /**
     * 새 처방을 만든다. 상태는 ORDERED다. 품목은 {@link #addItem}으로 붙인다.
     *
     * @param prescriptionNo 처방전번호 (저장 시 앞뒤 공백 제거 + 대문자)
     * @param departmentCode 진료과 코드 (저장 시 대문자)
     */
    public Prescription(String prescriptionNo, String patientNo, String patientName, String departmentCode,
            String prescriberName, PrescriptionType prescriptionType, LocalDateTime prescribedAt, String remark) {
        this.prescriptionNo = normalizeNo(prescriptionNo);
        this.patientNo = patientNo;
        this.patientName = patientName;
        this.departmentCode = departmentCode.trim().toUpperCase(Locale.ROOT);
        this.prescriberName = prescriberName;
        this.prescriptionType = prescriptionType;
        this.prescribedAt = prescribedAt;
        this.remark = remark;
        this.status = PrescriptionStatus.ORDERED;
    }

    /** 처방전번호는 대문자로 저장한다. 중복 검사 전에도 같은 규칙을 적용해야 한다. */
    public static String normalizeNo(String prescriptionNo) {
        // Locale.ROOT: 터키어 로케일 등에서 'i'가 'İ'로 바뀌는 문제를 막는다.
        return prescriptionNo.trim().toUpperCase(Locale.ROOT);
    }

    /** 품목을 붙인다. 순번은 붙인 순서대로 1부터 매긴다. */
    public PrescriptionItem addItem(String drugCode, String drugName, BigDecimal dose, String doseUnit, String route,
            String frequency, int days, BigDecimal totalQuantity, String usageNote) {
        PrescriptionItem item = new PrescriptionItem(this, items.size() + 1, drugCode, drugName, dose, doseUnit,
                route, frequency, days, totalQuantity, usageNote);
        items.add(item);
        return item;
    }

    /** 취소된 처방인가. 취소된 처방은 조제할 수 없다. */
    public boolean isCancelled() {
        return status == PrescriptionStatus.CANCELLED;
    }

    /** 취소할 수 있는가. 조제 전(ORDERED)에만 취소할 수 있다. */
    public boolean isCancellable() {
        return status == PrescriptionStatus.ORDERED;
    }

    /** 조제가 등록되면 호출한다. 다시 조제할 때(이전 조제가 불합격)는 이미 DISPENSED다. */
    public void markDispensed() {
        this.status = PrescriptionStatus.DISPENSED;
    }

    /** 처방을 취소한다. 취소 가능 여부는 서비스가 먼저 확인한다. */
    public void cancel(String reason, LocalDateTime at) {
        this.status = PrescriptionStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = at;
    }

    /** 조회한 품목을 채운다. {@link PrescriptionRepository}만 호출한다. */
    void loadItems(List<PrescriptionItem> loaded) {
        loaded.forEach(item -> item.attachTo(this));
        this.items = new ArrayList<>(loaded);
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public String getPrescriptionNo() {
        return prescriptionNo;
    }

    public String getPatientNo() {
        return patientNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public String getPrescriberName() {
        return prescriberName;
    }

    public PrescriptionType getPrescriptionType() {
        return prescriptionType;
    }

    public PrescriptionStatus getStatus() {
        return status;
    }

    public LocalDateTime getPrescribedAt() {
        return prescribedAt;
    }

    public String getRemark() {
        return remark;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    /** 품목 목록 (읽기 전용). 품목은 {@link #addItem}으로만 추가한다. */
    public List<PrescriptionItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
