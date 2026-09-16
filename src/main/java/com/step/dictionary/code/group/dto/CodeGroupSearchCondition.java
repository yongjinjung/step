package com.step.dictionary.code.group.dto;

/**
 * 코드 그룹 검색 조건. 쿼리 파라미터({@code ?keyword=&used=})가 record 생성자로 바인딩된다.
 * 모든 조건은 선택이며, 값이 없으면 해당 조건을 적용하지 않는다.
 *
 * @param keyword 코드ID / 코드명 부분 일치 (앞뒤 공백 제거, 코드ID는 대소문자 구분 없음)
 * @param used    사용 여부 (null이면 전체)
 */
public record CodeGroupSearchCondition(String keyword, Boolean used) {
}
