package com.step.dictionary.pharmacy.prescription;

/** 처방 구분. DB에는 이름 그대로(VARCHAR) 저장한다. */
public enum PrescriptionType {

    /** 외래 처방. */
    OUTPATIENT,
    /** 입원 처방. */
    INPATIENT,
    /** 퇴원 처방. */
    DISCHARGE,
    /** 응급 처방. */
    EMERGENCY
}
