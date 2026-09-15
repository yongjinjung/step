package com.step.dictionary.history;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ChangedFieldsTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("수정 이력은 값이 달라진 필드만 나온다 (id 제외)")
    void update() {
        JsonNode before = json("{\"id\": 1, \"name\": \"고객\", \"englishName\": \"Customer\", \"description\": null}");
        JsonNode after = json("{\"id\": 1, \"name\": \"고객\", \"englishName\": \"Client\", \"description\": \"설명\"}");

        assertThat(ChangedFields.between(before, after)).containsExactly("englishName", "description");
    }

    @Test
    @DisplayName("등록 이력은 값이 있는 필드가, 삭제 이력은 값이 있던 필드가 나온다")
    void createAndDelete() {
        JsonNode snapshot = json("{\"id\": 1, \"name\": \"고객\", \"description\": null, \"classifier\": false}");

        assertThat(ChangedFields.between(null, snapshot)).containsExactly("name", "classifier");
        assertThat(ChangedFields.between(snapshot, null)).containsExactly("name", "classifier");
    }

    @Test
    @DisplayName("배열 값은 순서까지 비교한다")
    void arrayOrder() {
        JsonNode before = json("{\"wordIds\": [1, 2]}");
        JsonNode after = json("{\"wordIds\": [2, 1]}");

        assertThat(ChangedFields.between(before, after)).containsExactly("wordIds");
        assertThat(ChangedFields.between(before, json("{\"wordIds\": [1, 2]}"))).isEmpty();
    }

    private JsonNode json(String value) {
        return jsonMapper.readTree(value);
    }
}
