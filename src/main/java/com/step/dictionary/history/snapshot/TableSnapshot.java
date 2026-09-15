package com.step.dictionary.history.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.step.dictionary.history.HistoryTargetType;
import com.step.dictionary.meta.table.MetaTable;

/**
 * 테이블 정의 스냅샷.
 *
 * @param id           테이블 정의 ID
 * @param physicalName 물리명
 * @param logicalName  논리명
 * @param description  설명
 */
public record TableSnapshot(Long id, String physicalName, String logicalName, String description)
        implements HistorySnapshot {

    /** 엔티티의 현재 값으로 스냅샷을 뜬다. */
    public static TableSnapshot from(MetaTable table) {
        return new TableSnapshot(table.getId(), table.getPhysicalName(), table.getLogicalName(), table.getDescription());
    }

    @Override
    @JsonIgnore
    public HistoryTargetType targetType() {
        return HistoryTargetType.TABLE;
    }
}
