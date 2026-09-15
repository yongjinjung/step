package com.step.dictionary.standard.word.dto;

/**
 * 단어 검색 조건. 쿼리 파라미터({@code ?keyword=&classifier=})가 record 생성자로 바인딩된다.
 * 모든 조건은 선택이며, 값이 없으면 해당 조건을 적용하지 않는다.
 *
 * @param keyword    단어명 / 영문약어 / 영문명 부분 일치 (앞뒤 공백 제거, 영문은 대소문자 구분 없음)
 * @param classifier 분류어 여부 (null이면 전체)
 */
public record WordSearchCondition(String keyword, Boolean classifier) {
}
