package com.step.dictionary.global.mybatis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InClauseTest {

    @Test
    @DisplayName("1000개를 넘으면 1000개씩 나눠 조회하고 결과를 이어 붙인다 (Oracle ORA-01795 방지)")
    void splitsByThousand() {
        List<Long> ids = LongStream.rangeClosed(1, 2500).boxed().toList();
        List<Integer> chunkSizes = new ArrayList<>();

        List<Long> result = InClause.query(ids, chunk -> {
            chunkSizes.add(chunk.size());
            return chunk;
        });

        assertThat(chunkSizes).containsExactly(1000, 1000, 500);
        assertThat(result).isEqualTo(ids);
    }

    @Test
    @DisplayName("빈 목록이면 조회하지 않는다 (IN () 문법 오류 방지)")
    void emptyDoesNotQuery() {
        assertThat(InClause.query(List.<Long>of(), chunk -> {
            throw new AssertionError("조회하면 안 된다");
        })).isEmpty();
    }
}
