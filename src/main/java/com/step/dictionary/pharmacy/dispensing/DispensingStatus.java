package com.step.dictionary.pharmacy.dispensing;

/**
 * 조제 상태.
 *
 * <pre>
 * DISPENSED ──검수(전 품목 합격)──▶ INSPECTED ──불출──▶ ISSUED
 *     └──검수(불합격 품목 있음)──▶ REJECTED  (같은 처방으로 다시 조제할 수 있다)
 * </pre>
 */
public enum DispensingStatus {

    /** 조제됨. 검수를 기다린다. */
    DISPENSED,
    /** 검수 합격. 불출할 수 있다. */
    INSPECTED,
    /** 검수 불합격. 이 조제는 더 진행하지 않는다. */
    REJECTED,
    /** 불출됨. */
    ISSUED
}
