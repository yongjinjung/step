package com.step.dictionary.history.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.step.dictionary.code.group.CodeGroup;
import com.step.dictionary.history.HistoryTargetType;

/**
 * 공통 코드 그룹 스냅샷.
 *
 * @param id          코드 그룹 ID
 * @param codeId      코드ID
 * @param codeName    코드명
 * @param description 설명
 * @param used        사용 여부
 */
public record CodeGroupSnapshot(Long id, String codeId, String codeName, String description, boolean used)
        implements HistorySnapshot {

    /** 엔티티의 현재 값으로 스냅샷을 뜬다. */
    public static CodeGroupSnapshot from(CodeGroup group) {
        return new CodeGroupSnapshot(
                group.getId(), group.getCodeId(), group.getCodeName(), group.getDescription(), group.isUsed());
    }

    @Override
    @JsonIgnore
    public HistoryTargetType targetType() {
        return HistoryTargetType.CODE_GROUP;
    }
}
