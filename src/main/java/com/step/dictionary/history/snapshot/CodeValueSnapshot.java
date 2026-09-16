package com.step.dictionary.history.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.step.dictionary.code.group.CodeGroup;
import com.step.dictionary.code.value.CodeValue;
import com.step.dictionary.history.HistoryTargetType;

/**
 * 공통 코드값 스냅샷.
 *
 * @param id          코드값 ID
 * @param groupId     소속 코드 그룹 ID
 * @param codeId      소속 그룹의 코드ID (읽기용)
 * @param codeValue   코드값
 * @param valueName   코드값명
 * @param description 설명
 * @param sortOrder   정렬 순서
 * @param used        사용 여부
 */
public record CodeValueSnapshot(Long id, Long groupId, String codeId, String codeValue, String valueName,
        String description, int sortOrder, boolean used) implements HistorySnapshot {

    /** 엔티티의 현재 값으로 스냅샷을 뜬다. 소속 그룹의 코드ID를 읽으므로 트랜잭션 안에서 호출한다. */
    public static CodeValueSnapshot from(CodeValue value) {
        CodeGroup group = value.getGroup();
        return new CodeValueSnapshot(value.getId(), group.getId(), group.getCodeId(), value.getCodeValue(),
                value.getValueName(), value.getDescription(), value.getSortOrder(), value.isUsed());
    }

    @Override
    @JsonIgnore
    public HistoryTargetType targetType() {
        return HistoryTargetType.CODE_VALUE;
    }
}
