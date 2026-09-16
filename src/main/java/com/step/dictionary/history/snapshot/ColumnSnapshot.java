package com.step.dictionary.history.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.step.dictionary.code.group.CodeGroup;
import com.step.dictionary.history.HistoryTargetType;
import com.step.dictionary.meta.column.MetaColumn;
import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.term.Term;

/**
 * 컬럼 정의 스냅샷. 비표준 검출 결과는 조회 시점에 계산되는 값이라 넣지 않는다.
 *
 * <p>코드 그룹 필드(codeGroupId, codeId)는 9단계에서 추가했다. 그 전에 남은 이력 JSON에는 없지만,
 * {@code changedFields}는 필드 없음과 null을 같게 보므로 이전 이력과 비교해도 변경으로 보이지 않는다.
 *
 * @param id                컬럼 정의 ID
 * @param tableId           소속 테이블 ID
 * @param tablePhysicalName 소속 테이블 물리명 (읽기용)
 * @param physicalName      물리명
 * @param logicalName       논리명
 * @param termId            매핑 용어 ID (없으면 null)
 * @param termName          매핑 용어명 (읽기용)
 * @param codeGroupId       매핑 공통 코드 그룹 ID (없으면 null)
 * @param codeId            매핑 코드 그룹의 코드ID (읽기용)
 * @param dataType          데이터 타입
 * @param dataLength        길이
 * @param dataScale         소수점 자리수
 * @param nullable          NULL 허용 여부
 * @param primaryKey        기본키 여부
 * @param seq               컬럼 순서
 */
public record ColumnSnapshot(Long id, Long tableId, String tablePhysicalName, String physicalName,
        String logicalName, Long termId, String termName, Long codeGroupId, String codeId, DataType dataType,
        Integer dataLength, Integer dataScale, boolean nullable, boolean primaryKey, int seq)
        implements HistorySnapshot {

    /** 엔티티의 현재 값으로 스냅샷을 뜬다. 테이블, 용어, 코드 그룹을 읽으므로 트랜잭션 안에서 호출한다. */
    public static ColumnSnapshot from(MetaColumn column) {
        Term term = column.getTerm();
        CodeGroup codeGroup = column.getCodeGroup();
        return new ColumnSnapshot(column.getId(), column.getTable().getId(), column.getTable().getPhysicalName(),
                column.getPhysicalName(), column.getLogicalName(),
                term == null ? null : term.getId(), term == null ? null : term.getName(),
                codeGroup == null ? null : codeGroup.getId(), codeGroup == null ? null : codeGroup.getCodeId(),
                column.getDataType(), column.getDataLength(), column.getDataScale(),
                column.isNullable(), column.isPrimaryKey(), column.getSeq());
    }

    @Override
    @JsonIgnore
    public HistoryTargetType targetType() {
        return HistoryTargetType.COLUMN;
    }
}
