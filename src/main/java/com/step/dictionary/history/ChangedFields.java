package com.step.dictionary.history;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * 변경 전후 스냅샷에서 달라진 필드 이름을 구한다. 이력 응답의 {@code changedFields}를 만든다.
 *
 * <p>규칙:
 * <ul>
 *   <li>JSON null과 필드 없음은 같은 것으로 본다.</li>
 *   <li>등록(변경 전 없음)은 값이 있는 필드, 삭제(변경 후 없음)는 값이 있던 필드가 나온다.</li>
 *   <li>{@code id}는 대상을 가리키는 값이라 비교에서 뺀다.</li>
 *   <li>순서는 스냅샷 필드 순서(변경 전 → 변경 후)를 따른다.</li>
 * </ul>
 * 저장하지 않고 조회할 때 계산하므로 규칙을 바꿔도 과거 이력에 바로 적용된다.
 */
public final class ChangedFields {

    private static final String ID_FIELD = "id";

    /** 인스턴스 생성 방지. */
    private ChangedFields() {
    }

    /**
     * @param before 변경 전 스냅샷 (null 가능)
     * @param after  변경 후 스냅샷 (null 가능)
     * @return 달라진 필드 이름 목록
     */
    public static List<String> between(JsonNode before, JsonNode after) {
        Set<String> names = new LinkedHashSet<>();
        if (before != null) {
            names.addAll(before.propertyNames());
        }
        if (after != null) {
            names.addAll(after.propertyNames());
        }
        names.remove(ID_FIELD);
        return names.stream()
                .filter(name -> !Objects.equals(valueOf(before, name), valueOf(after, name)))
                .toList();
    }

    /** 필드 값. 스냅샷이 없거나, 필드가 없거나, JSON null이면 모두 null로 맞춘다. */
    private static JsonNode valueOf(JsonNode node, String name) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(name);
        return value == null || value.isNull() ? null : value;
    }
}
