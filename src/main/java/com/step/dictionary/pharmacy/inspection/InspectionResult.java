package com.step.dictionary.pharmacy.inspection;

/** 검수 결과. 품목별 결과이자, 검수 전체 결과(품목이 모두 합격이면 PASS)다. */
public enum InspectionResult {

    /** 합격. */
    PASS,
    /** 불합격. 품목이면 불량 사유가 필요하다. */
    FAIL
}
