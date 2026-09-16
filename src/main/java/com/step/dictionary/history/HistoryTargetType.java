package com.step.dictionary.history;

/**
 * 변경 이력 대상 종류.
 * 신청 대상({@code request.TargetType})보다 넓다. 테이블/컬럼 정의는 신청 없이 바뀌지만 이력은 남긴다.
 */
public enum HistoryTargetType {

    /** 표준 단어. */
    WORD,
    /** 표준 도메인. */
    DOMAIN,
    /** 표준 용어. */
    TERM,
    /** 테이블 정의. */
    TABLE,
    /** 컬럼 정의. */
    COLUMN,
    /** 공통 코드 그룹 (9단계). 신청 없이 ADMIN이 바로 바꾼다. */
    CODE_GROUP,
    /** 공통 코드값 (9단계). */
    CODE_VALUE
}
