package com.step.dictionary.global.mybatis;

import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * LIKE 검색 패턴. 매퍼 XML에서 OGNL 정적 호출로 쓴다.
 *
 * <pre>{@code
 * <bind name="keyword" value="@com.step.dictionary.global.mybatis.Likes@contains(c.keyword)"/>
 * <if test="keyword != null">AND name LIKE #{keyword} ESCAPE '!'</if>
 * }</pre>
 *
 * <ul>
 *   <li>사용자가 입력한 {@code %}, {@code _}는 와일드카드가 아니라 글자로 찾도록 {@code !}로 이스케이프한다.
 *       그래서 SQL에 반드시 {@code ESCAPE '!'}를 붙인다.</li>
 *   <li><b>컬럼에 함수({@code LOWER()} 등)를 씌우지 않는다.</b> 인덱스를 못 쓰게 된다. 대소문자는
 *       DB 정렬 규칙(MySQL {@code utf8mb4_0900_ai_ci}, 테스트 H2 {@code IGNORECASE=TRUE})이 구분하지 않는다.
 *       저장할 때 대문자·소문자로 정규화하는 컬럼은 {@code *Upper}/{@code *Lower}로 검색어도 같게 맞춘다.</li>
 *   <li>{@link #contains}({@code %값%})는 앞에 {@code %}가 붙어 인덱스를 못 탄다. 데이터가 적은 표준 사전·관리 화면에만 쓰고,
 *       많이 쌓이는 업무 데이터(약제)는 인덱스를 타는 {@link #startsWith}({@code 값%})를 쓴다.</li>
 * </ul>
 */
public final class Likes {

    private Likes() {
    }

    /** 키워드를 포함 ({@code %키워드%}). 비어 있으면 null(조건 없음). */
    public static String contains(String keyword) {
        String escaped = escape(keyword);
        return escaped == null ? null : "%" + escaped + "%";
    }

    /** 대문자로 바꿔 포함 (대문자로 저장하는 컬럼: 영문약어, 코드ID, 물리명). */
    public static String containsUpper(String keyword) {
        return contains(keyword == null ? null : keyword.toUpperCase(Locale.ROOT));
    }

    /** 소문자로 바꿔 포함 (소문자로 저장하는 컬럼: 로그인 아이디). */
    public static String containsLower(String keyword) {
        return contains(keyword == null ? null : keyword.toLowerCase(Locale.ROOT));
    }

    /** 키워드로 시작 ({@code 키워드%}). 인덱스 범위 검색이 된다. 비어 있으면 null. */
    public static String startsWith(String keyword) {
        String escaped = escape(keyword);
        return escaped == null ? null : escaped + "%";
    }

    /** 대문자로 바꿔 시작 (대문자로 저장하는 컬럼: 처방전번호). */
    public static String startsWithUpper(String keyword) {
        return startsWith(keyword == null ? null : keyword.toUpperCase(Locale.ROOT));
    }

    /** 앞뒤 공백을 빼고 {@code ! % _}를 이스케이프한다. 비어 있으면 null. */
    private static String escape(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
