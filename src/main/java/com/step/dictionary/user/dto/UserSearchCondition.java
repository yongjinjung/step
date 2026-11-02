package com.step.dictionary.user.dto;

/**
 * 사용자 검색 조건. 쿼리 파라미터가 record 생성자로 바인딩된다. 모든 조건은 선택이다.
 *
 * @param keyword 아이디(대소문자 구분 없음) / 이름 부분 일치. 앞뒤 공백 제거
 * @param roleId  이 역할을 가진 사용자만 (null이면 전체)
 * @param enabled 활성 여부 (null이면 전체)
 */
public record UserSearchCondition(String keyword, Long roleId, Boolean enabled) {
}
