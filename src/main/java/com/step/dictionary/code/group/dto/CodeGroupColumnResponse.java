package com.step.dictionary.code.group.dto;

import com.step.dictionary.meta.column.MetaColumn;
import com.step.dictionary.meta.table.MetaTable;

/**
 * 코드 그룹에 매핑된 컬럼 정의 요약. 코드 그룹 화면에서 "어느 테이블의 어느 컬럼이 이 코드를 쓰는지" 보여준다.
 * 비표준 검출 결과는 필요 없어 {@code MetaColumnResponse} 대신 필요한 값만 담는다 (용어·도메인을 읽지 않는다).
 *
 * @param columnId          컬럼 정의 ID
 * @param tableId           소속 테이블 ID
 * @param tablePhysicalName 테이블 물리명
 * @param tableLogicalName  테이블 논리명
 * @param physicalName      컬럼 물리명
 * @param logicalName       컬럼 논리명
 */
public record CodeGroupColumnResponse(
        Long columnId,
        Long tableId,
        String tablePhysicalName,
        String tableLogicalName,
        String physicalName,
        String logicalName
) {

    /** 컬럼 정의를 응답으로 변환한다. 소속 테이블을 읽으므로 테이블을 함께 조회해 두거나 트랜잭션 안에서 호출한다. */
    public static CodeGroupColumnResponse from(MetaColumn column) {
        MetaTable table = column.getTable();
        return new CodeGroupColumnResponse(
                column.getId(),
                table.getId(),
                table.getPhysicalName(),
                table.getLogicalName(),
                column.getPhysicalName(),
                column.getLogicalName());
    }
}
