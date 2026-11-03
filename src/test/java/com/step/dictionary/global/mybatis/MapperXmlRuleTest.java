package com.step.dictionary.global.mybatis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** 매퍼 XML 작성 규칙 검사. */
class MapperXmlRuleTest {

    @Test
    @DisplayName("매퍼 XML은 문자열 치환(${})을 쓰지 않고 #{} 바인딩만 쓴다 (SQL 인젝션 방지 보안 규칙)")
    void noStringSubstitution() throws IOException {
        Resource[] mappers = new PathMatchingResourcePatternResolver().getResources("classpath:mapper/**/*.xml");
        assertThat(mappers).isNotEmpty();

        List<String> violations = new ArrayList<>();
        for (Resource mapper : mappers) {
            String[] lines = mapper.getContentAsString(StandardCharsets.UTF_8).split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains("${")) {
                    violations.add(mapper.getFilename() + ":" + (i + 1) + " " + lines[i].trim());
                }
            }
        }
        assertThat(violations).isEmpty();
    }
}
