package com.step.dictionary.meta.column.dto;

import com.step.dictionary.meta.column.ColumnIssue;

/**
 * 비표준 컬럼 검색 조건. 쿼리 파라미터({@code ?tableId=&issue=})가 record 생성자로 바인딩된다.
 * 두 조건이 모두 없어도 비표준 컬럼만 조회된다 (표준 컬럼은 이 검색에 나오지 않는다).
 *
 * @param tableId 테이블 ID (null이면 전체)
 * @param issue   비표준 사유 (null이면 사유가 하나라도 있는 컬럼). 없는 값이면 400
 */
public record NonStandardColumnCondition(Long tableId, ColumnIssue issue) {
}
