package com.step.dictionary.global.mybatis;

import org.springframework.util.StringUtils;

/**
 * LIKE 부분 일치 패턴. 매퍼 XML에서 OGNL 정적 호출로 쓴다.
 *
 * <pre>{@code
 * <bind name="keyword" value="@com.step.dictionary.global.mybatis.Likes@contains(c.keyword)"/>
 * <if test="keyword != null">AND name LIKE #{keyword} ESCAPE '!'</if>
 * }</pre>
 *
 * 사용자가 입력한 {@code %}, {@code _}는 와일드카드가 아니라 글자로 찾도록 {@code !}로 이스케이프한다.
 * 그래서 SQL에 반드시 {@code ESCAPE '!'}를 붙인다. 대소문자 무시 비교는 {@code LOWER(컬럼) LIKE LOWER(#{keyword})}로 쓴다.
 */
public final class Likes {

    private Likes() {
    }

    /** 앞뒤 공백을 뺀 키워드를 포함하는 패턴 ({@code %키워드%}). 비어 있으면 null(조건 없음). */
    public static String contains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String escaped = keyword.trim()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
