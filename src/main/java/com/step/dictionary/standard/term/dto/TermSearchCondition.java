package com.step.dictionary.standard.term.dto;

/**
 * 용어 검색 조건. 쿼리 파라미터가 record 생성자로 바인딩된다. 모든 조건은 선택이다.
 *
 * @param keyword  용어명 / 영문약어 부분 일치 (앞뒤 공백 제거, 영문약어는 대소문자 구분 없음)
 * @param domainId 도메인 ID (null이면 전체)
 * @param wordId   이 단어를 구성 단어로 포함하는 용어만 (null이면 전체)
 */
public record TermSearchCondition(String keyword, Long domainId, Long wordId) {
}
