package com.step.dictionary.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 모든 테이블·컬럼에 설명(Description)이 달렸는지 검사한다.
 *
 * <p>설명은 마이그레이션의 {@code COMMENT ON TABLE/COLUMN}으로 단다(V14, Oracle과 H2 문법이 같다). 테이블이나 컬럼을
 * 추가하면서 설명을 빠뜨리면 이 테스트가 실패한다.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SchemaCommentTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("모든 테이블과 컬럼에 설명이 있다 (Flyway 이력 테이블 제외)")
    void everyTableAndColumnHasComment() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = CURRENT_SCHEMA AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                  AND (remarks IS NULL OR remarks = '')
                """, String.class);
        List<String> columns = jdbcTemplate.queryForList("""
                SELECT c.table_name || '.' || c.column_name
                FROM information_schema.columns c
                JOIN information_schema.tables t ON t.table_schema = c.table_schema AND t.table_name = c.table_name
                WHERE c.table_schema = CURRENT_SCHEMA AND t.table_type = 'BASE TABLE'
                  AND c.table_name <> 'flyway_schema_history'
                  AND (c.remarks IS NULL OR c.remarks = '')
                """, String.class);

        assertThat(tables).as("설명 없는 테이블").isEmpty();
        assertThat(columns).as("설명 없는 컬럼").isEmpty();
    }
}
