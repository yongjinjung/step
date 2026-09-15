package com.step.dictionary.support;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/** H2 테스트 DB의 모든 테이블을 비운다 (Flyway 이력 테이블 제외). */
public final class DatabaseCleaner {

    private static final String FLYWAY_HISTORY_TABLE = "flyway_schema_history";

    private DatabaseCleaner() {
    }

    public static void clean(JdbcTemplate jdbcTemplate) {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = CURRENT_SCHEMA AND table_type = 'BASE TABLE'",
                String.class);

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        tables.stream()
                .filter(table -> !table.equalsIgnoreCase(FLYWAY_HISTORY_TABLE))
                .forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY"));
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
