package com.step.dictionary.code.group.dto;

import com.step.dictionary.code.group.CodeGroup;
import java.time.LocalDateTime;

/**
 * 코드 그룹 응답.
 *
 * @param id          코드 그룹 ID
 * @param codeId      코드ID (대문자)
 * @param codeName    코드명
 * @param description 설명 (null 가능)
 * @param used        사용 여부
 * @param createdAt   생성 일시
 * @param updatedAt   최종 수정 일시
 */
public record CodeGroupResponse(
        Long id,
        String codeId,
        String codeName,
        String description,
        boolean used,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 엔티티를 응답으로 변환한다. */
    public static CodeGroupResponse from(CodeGroup group) {
        return new CodeGroupResponse(
                group.getId(),
                group.getCodeId(),
                group.getCodeName(),
                group.getDescription(),
                group.isUsed(),
                group.getCreatedAt(),
                group.getUpdatedAt());
    }
}
