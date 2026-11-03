package com.step.dictionary.global.logging;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.springframework.util.StringUtils;

/**
 * P6Spy SQL 로그 형식. 실제 실행된 SQL(파라미터 값이 채워진 것)을 보기 좋게 줄바꿈·들여쓰기해 남긴다.
 *
 * <pre>
 * [SQL] 2 ms | connection 7
 *     SELECT
 *       w.id,
 *       ...
 *     WHERE
 *       w.name LIKE '%고객%' ESCAPE '!'
 * </pre>
 *
 * 커밋·롤백처럼 SQL이 없는 이벤트는 한 줄로 남긴다. {@code src/main/resources/spy.properties}의
 * {@code logMessageFormat}으로 등록되며, SQL 로그는 {@code decorator.datasource.enabled=true}인 프로파일(local)에서만 나온다.
 */
public class P6SpySqlFormatter implements MessageFormattingStrategy {

    private static final String INDENT = "    ";

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared,
            String sql, String url) {
        if (!StringUtils.hasText(sql)) {
            return "[SQL] " + category + " | connection " + connectionId;
        }
        return "[SQL] " + elapsed + " ms | connection " + connectionId + System.lineSeparator() + format(category, sql);
    }

    /** 일반 SQL은 MySQL 문법으로 정렬하고, 각 줄을 들여써 로그의 다른 줄과 구분한다. */
    static String format(String category, String sql) {
        String formatted = isStatement(category) ? SqlFormatter.of(Dialect.MySql).format(sql.trim()) : sql.trim();
        return INDENT + formatted.replace("\n", System.lineSeparator() + INDENT);
    }

    private static boolean isStatement(String category) {
        return Category.STATEMENT.getName().equals(category) || Category.BATCH.getName().equals(category);
    }
}
