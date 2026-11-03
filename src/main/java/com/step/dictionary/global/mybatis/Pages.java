package com.step.dictionary.global.mybatis;

import java.util.List;
import java.util.function.LongSupplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

/**
 * MyBatis 목록 조회 결과를 Spring {@link Page}로 묶는다.
 *
 * <p>매퍼는 {@code search(조건, pageable)}(LIMIT/OFFSET)와 {@code count(조건)} 두 쿼리를 둔다.
 * 첫 페이지인데 내용이 페이지 크기보다 적거나 마지막 페이지라서 전체 건수를 알 수 있으면 count 쿼리를 생략한다.
 */
public final class Pages {

    private Pages() {
    }

    public static <T> Page<T> of(List<T> content, Pageable pageable, LongSupplier count) {
        return PageableExecutionUtils.getPage(content, pageable, count);
    }
}
