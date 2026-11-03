package com.step.dictionary.pharmacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.pharmacy.dispensing.Dispensing;
import com.step.dictionary.pharmacy.dispensing.DispensingStatus;
import com.step.dictionary.pharmacy.inspection.Inspection;
import com.step.dictionary.pharmacy.inspection.InspectionItem;
import com.step.dictionary.pharmacy.inspection.InspectionResult;
import com.step.dictionary.pharmacy.prescription.Prescription;
import com.step.dictionary.pharmacy.prescription.PrescriptionItem;
import com.step.dictionary.pharmacy.prescription.PrescriptionStatus;
import com.step.dictionary.pharmacy.prescription.PrescriptionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 약제 엔티티의 상태 전이와 품목 검사 (스프링 없이). */
class PharmacyDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 9, 0);

    @Test
    @DisplayName("처방은 처방전번호·코드를 대문자로 저장하고, 품목 순번을 붙인 순서대로 매긴다")
    void prescriptionNormalizesAndNumbersItems() {
        Prescription prescription = prescription();
        PrescriptionItem first = addItem(prescription, "amx500");
        PrescriptionItem second = addItem(prescription, "tyl500");

        assertThat(prescription.getPrescriptionNo()).isEqualTo("RX-001");
        assertThat(prescription.getDepartmentCode()).isEqualTo("IM");
        assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.ORDERED);
        assertThat(first.getLineNo()).isEqualTo(1);
        assertThat(second.getLineNo()).isEqualTo(2);
        assertThat(first.getDrugCode()).isEqualTo("AMX500");
        assertThat(first.getDoseUnit()).isEqualTo("MG");
    }

    @Test
    @DisplayName("처방은 조제 전에만 취소할 수 있다")
    void prescriptionCancellableOnlyBeforeDispensing() {
        Prescription ordered = prescription();
        assertThat(ordered.isCancellable()).isTrue();
        ordered.cancel("처방 변경", NOW);
        assertThat(ordered.isCancelled()).isTrue();
        assertThat(ordered.isCancellable()).isFalse();

        Prescription dispensed = prescription();
        dispensed.markDispensed();
        assertThat(dispensed.isCancellable()).isFalse();
    }

    @Test
    @DisplayName("검수는 품목이 모두 합격이면 PASS, 하나라도 불합격이면 FAIL이고 합격 품목의 불량 사유는 버린다")
    void inspectionResultFromItems() {
        Dispensing dispensing = new Dispensing(prescription(), "admin", NOW, null);

        Inspection passed = new Inspection(dispensing, "admin", NOW, null);
        InspectionItem item = passed.addItem(null, InspectionResult.PASS, "사유");
        assertThat(passed.isPassed()).isTrue();
        assertThat(item.getDefectReason()).isNull();

        Inspection failed = new Inspection(dispensing, "admin", NOW, null);
        failed.addItem(null, InspectionResult.PASS, null);
        failed.addItem(null, InspectionResult.FAIL, "수량 부족");
        failed.addItem(null, InspectionResult.PASS, null);
        assertThat(failed.getResult()).isEqualTo(InspectionResult.FAIL);
    }

    @Test
    @DisplayName("조제는 DISPENSED에서만 검수하고, 합격(INSPECTED)일 때만 불출할 수 있다")
    void dispensingStatusFlow() {
        Dispensing passed = new Dispensing(prescription(), "admin", NOW, null);
        assertThat(passed.isInspectable()).isTrue();
        assertThat(passed.isIssuable()).isFalse();
        passed.applyInspection(true);
        assertThat(passed.getStatus()).isEqualTo(DispensingStatus.INSPECTED);
        assertThat(passed.isInspectable()).isFalse();
        assertThat(passed.isIssuable()).isTrue();
        passed.markIssued();
        assertThat(passed.getStatus()).isEqualTo(DispensingStatus.ISSUED);

        Dispensing rejected = new Dispensing(prescription(), "admin", NOW, null);
        rejected.applyInspection(false);
        assertThat(rejected.getStatus()).isEqualTo(DispensingStatus.REJECTED);
        assertThat(rejected.isIssuable()).isFalse();
    }

    @Test
    @DisplayName("requireExactly는 순서와 상관없이 같은 집합만 통과시키고, 누락·중복·다른 품목은 막는다")
    void requireExactly() {
        ErrorCode code = ErrorCode.INVALID_DISPENSING_ITEMS;

        assertThatCode(() -> PharmacyItems.requireExactly(List.of(2L, 1L), List.of(1L, 2L), code))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> PharmacyItems.requireExactly(List.of(1L), List.of(1L, 2L), code))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> PharmacyItems.requireExactly(List.of(1L, 1L, 2L), List.of(1L, 2L), code))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> PharmacyItems.requireExactly(List.of(1L, 3L), List.of(1L, 2L), code))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("requireSubset은 일부만 다뤄도 통과시키고, 중복·다른 품목은 막는다")
    void requireSubset() {
        ErrorCode code = ErrorCode.INVALID_RETURN_ITEMS;

        assertThatCode(() -> PharmacyItems.requireSubset(List.of(2L), List.of(1L, 2L), code))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> PharmacyItems.requireSubset(List.of(2L, 2L), List.of(1L, 2L), code))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(code);
        assertThatThrownBy(() -> PharmacyItems.requireSubset(List.of(3L), List.of(1L, 2L), code))
                .isInstanceOf(BusinessException.class);
    }

    private static Prescription prescription() {
        return new Prescription(" rx-001 ", "P001", "홍길동", "im", "이의사", PrescriptionType.OUTPATIENT, NOW, null);
    }

    private static PrescriptionItem addItem(Prescription prescription, String drugCode) {
        return prescription.addItem(drugCode, "약", BigDecimal.ONE, "mg", "po", "tid", 3, BigDecimal.TEN, null);
    }
}
