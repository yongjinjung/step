package com.step.dictionary.global.mybatis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * {@code IN (...)} 조회를 나눠 실행한다.
 *
 * <p>Oracle은 IN 목록에 값을 1000개까지만 받는다(ORA-01795). 목록이 길면 1000개씩 나눠 매퍼를 여러 번 부르고 결과를 이어 붙인다.
 * 결과 순서는 나눈 묶음 안에서만 SQL의 ORDER BY를 따른다. 묶음을 넘는 순서가 필요하면 호출한 쪽에서 다시 정렬하거나 묶는다.
 *
 * <pre>{@code
 * List<Word> words = InClause.query(ids, mapper::findAllById);
 * }</pre>
 */
public final class InClause {

    /** Oracle IN 목록 최대 개수. */
    public static final int MAX_SIZE = 1000;

    private InClause() {
    }

    public static <T, R> List<R> query(Collection<T> values, Function<List<T>, List<R>> query) {
        List<T> all = List.copyOf(values);
        if (all.isEmpty()) {
            return List.of();
        }
        if (all.size() <= MAX_SIZE) {
            return query.apply(all);
        }
        List<R> result = new ArrayList<>();
        for (int from = 0; from < all.size(); from += MAX_SIZE) {
            result.addAll(query.apply(all.subList(from, Math.min(from + MAX_SIZE, all.size()))));
        }
        return result;
    }
}
