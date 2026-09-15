package com.step.dictionary.standard.domain.dto;

import com.step.dictionary.standard.domain.DataType;

/**
 * 도메인 검색 조건. 쿼리 파라미터가 record 생성자로 바인딩된다. 모든 조건은 선택이다.
 *
 * @param keyword          도메인명 부분 일치 (앞뒤 공백 제거, 대소문자 구분 없음)
 * @param classifierWordId 분류어 단어 ID (null이면 전체)
 * @param dataType         데이터 타입 (null이면 전체). 없는 값이면 400
 */
public record DomainSearchCondition(String keyword, Long classifierWordId, DataType dataType) {
}
