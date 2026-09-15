package com.step.dictionary.user;

/**
 * 사용자 역할. DB에는 enum 이름 문자열로 저장하고, JWT의 {@code roles} 클레임에도 이 이름이 들어간다.
 * Spring Security에서는 {@code ROLE_} 접두어를 붙인 권한({@code ROLE_ADMIN})으로 바뀌어
 * {@code hasRole("ADMIN")} 규칙과 비교된다 ({@code SecurityConfig}).
 */
public enum Role {

    /** 관리자. 표준 단어/도메인/용어 쓰기, 사용자 관리를 포함한 모든 기능을 쓸 수 있다. */
    ADMIN,
    /** 일반 사용자. 조회와 테이블/컬럼 정의 쓰기를 할 수 있다. 표준 데이터 변경은 6단계에서 신청으로 한다. */
    USER
}
