package com.step.dictionary.meta.table.dto;

import com.step.dictionary.meta.table.MetaTable;
import java.time.LocalDateTime;

/**
 * 테이블 정의 응답. 컬럼 목록은 포함하지 않으며 {@code GET /api/tables/{tableId}/columns}로 따로 조회한다.
 *
 * @param id           테이블 정의 ID
 * @param physicalName 물리명 (대문자)
 * @param logicalName  논리명
 * @param description  설명 (null 가능)
 * @param createdAt    생성 일시
 * @param updatedAt    최종 수정 일시
 */
public record MetaTableResponse(
        Long id,
        String physicalName,
        String logicalName,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 엔티티를 응답으로 변환한다. */
    public static MetaTableResponse from(MetaTable table) {
        return new MetaTableResponse(
                table.getId(),
                table.getPhysicalName(),
                table.getLogicalName(),
                table.getDescription(),
                table.getCreatedAt(),
                table.getUpdatedAt());
    }
}
