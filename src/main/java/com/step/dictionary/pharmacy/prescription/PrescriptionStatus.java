package com.step.dictionary.pharmacy.prescription;

/**
 * 처방 상태.
 *
 * <pre>
 * ORDERED ──조제 등록──▶ DISPENSED
 *    └──취소──▶ CANCELLED
 * </pre>
 * 조제가 검수 불합격(REJECTED)이 되어도 처방은 DISPENSED로 남고, 같은 처방으로 다시 조제할 수 있다.
 */
public enum PrescriptionStatus {

    /** 처방됨 (조제 전). 취소할 수 있다. */
    ORDERED,
    /** 조제됨. 취소할 수 없다. */
    DISPENSED,
    /** 취소됨. 조제할 수 없다. */
    CANCELLED
}
