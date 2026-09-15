package com.step.dictionary.meta.column;

/**
 * 비표준 컬럼 검출 사유.
 *
 * <p>한 컬럼에 여러 사유가 동시에 붙을 수 있다 (NO_TERM은 예외로 단독).
 * 판정은 저장하지 않고 조회할 때마다 현재 용어/도메인으로 계산한다.
 * 규칙은 {@link MetaColumn#inspect()}(응답의 issues)와 {@link MetaColumnRepositoryCustomImpl}(검색 조건) 두 곳에 있으므로
 * 사유를 추가·변경하면 둘 다 고쳐야 한다.
 */
public enum ColumnIssue {

    /** 표준 용어가 매핑되지 않았다. 이 경우 나머지 항목은 검사하지 않는다. */
    NO_TERM,
    /** 컬럼 물리명이 용어 영문약어와 다르다. */
    NAME_MISMATCH,
    /** 데이터 타입이 용어 도메인과 다르다. */
    TYPE_MISMATCH,
    /** 길이 또는 소수점 자리수가 용어 도메인과 다르다. NULL끼리는 같다고 본다. */
    LENGTH_MISMATCH
}
