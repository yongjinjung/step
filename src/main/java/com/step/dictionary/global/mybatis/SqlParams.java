package com.step.dictionary.global.mybatis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * 검색 조건 값을 SQL 파라미터로 다듬는 도우미. 매퍼 XML에서 {@code <bind>}의 OGNL 정적 호출로 쓴다.
 *
 * <pre>{@code
 * <bind name="requester" value="@com.step.dictionary.global.mybatis.SqlParams@lower(c.requester)"/>
 * <if test="requester != null">AND u.username = #{requester}</if>
 * }</pre>
 *
 * 모든 메서드는 값이 비어 있으면 null을 돌려준다. 매퍼에서는 null이면 조건을 붙이지 않는다.
 */
public final class SqlParams {

    private SqlParams() {
    }

    /** 앞뒤 공백을 빼고 소문자로 (로그인 아이디처럼 소문자로 저장하는 값). */
    public static String lower(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    /** 앞뒤 공백을 빼고 대문자로 (진료과·불출처 코드처럼 대문자로 저장하는 값). */
    public static String upper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    /** 기간 시작일의 0시. {@code 일시 >= 시작}으로 비교한다. */
    public static LocalDateTime startOfDay(LocalDate from) {
        return from == null ? null : from.atStartOfDay();
    }

    /** 기간 종료일 다음 날 0시. {@code 일시 < 다음 날}로 비교해 종료일 하루 전체를 포함한다. */
    public static LocalDateTime startOfNextDay(LocalDate to) {
        return to == null ? null : to.plusDays(1).atStartOfDay();
    }
}
