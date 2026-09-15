package com.step.dictionary.user.dto;

import com.step.dictionary.user.Role;

/**
 * 사용자 검색 조건. 쿼리 파라미터가 record 생성자로 바인딩된다. 모든 조건은 선택이다.
 *
 * @param keyword 아이디(대소문자 구분 없음) / 이름 부분 일치. 앞뒤 공백 제거
 * @param role    역할 (null이면 전체)
 * @param enabled 활성 여부 (null이면 전체)
 */
public record UserSearchCondition(String keyword, Role role, Boolean enabled) {
}
