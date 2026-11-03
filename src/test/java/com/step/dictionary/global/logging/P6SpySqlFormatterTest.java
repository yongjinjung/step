package com.step.dictionary.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class P6SpySqlFormatterTest {

    private final P6SpySqlFormatter formatter = new P6SpySqlFormatter();

    @Test
    @DisplayName("값이 채워진 SQL을 줄바꿈·들여쓰기하고 실행 시간과 연결 번호를 앞에 붙인다")
    void formatStatement() {
        String message = formatter.formatMessage(7, "", 3, "statement",
                "SELECT w.id FROM std_word w WHERE w.name = ?",
                "SELECT w.id, w.name FROM std_word w WHERE w.name = '고객' ORDER BY w.name", "jdbc:h2:mem");

        String[] lines = message.split("\\R");
        assertThat(lines[0]).isEqualTo("[SQL] 3 ms | connection 7");
        assertThat(lines).hasSizeGreaterThan(3);
        assertThat(message).contains("'고객'").doesNotContain("?");
        assertThat(lines[1]).startsWith("    SELECT");
    }

    @Test
    @DisplayName("SQL이 없는 이벤트(커밋 등)는 한 줄로 남긴다")
    void formatCommit() {
        assertThat(formatter.formatMessage(7, "", 0, "commit", "", "", "jdbc:h2:mem"))
                .isEqualTo("[SQL] commit | connection 7");
    }
}
