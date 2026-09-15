package com.step.dictionary.meta.table.dto;

/**
 * 테이블 정의 검색 조건. 쿼리 파라미터가 record 생성자로 바인딩된다.
 *
 * @param keyword 물리명(대소문자 구분 없음) / 논리명 부분 일치. 앞뒤 공백을 제거하며, 비어 있으면 전체
 */
public record MetaTableSearchCondition(String keyword) {
}
