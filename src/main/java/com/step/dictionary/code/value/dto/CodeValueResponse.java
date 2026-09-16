package com.step.dictionary.code.value.dto;

import com.step.dictionary.code.group.CodeGroup;
import com.step.dictionary.code.value.CodeValue;
import java.time.LocalDateTime;

/**
 * 코드값 응답.
 *
 * @param id          코드값 ID
 * @param groupId     소속 코드 그룹 ID
 * @param codeId      소속 그룹의 코드ID (어느 그룹의 값인지 바로 보이도록)
 * @param codeValue   코드값 (대문자)
 * @param valueName   코드값명
 * @param description 설명 (null 가능)
 * @param sortOrder   정렬 순서
 * @param used        사용 여부
 * @param createdAt   생성 일시
 * @param updatedAt   최종 수정 일시
 */
public record CodeValueResponse(
        Long id,
        Long groupId,
        String codeId,
        String codeValue,
        String valueName,
        String description,
        int sortOrder,
        boolean used,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 엔티티를 응답으로 변환한다. 소속 그룹을 읽으므로 트랜잭션 안에서 호출한다. */
    public static CodeValueResponse from(CodeValue value) {
        CodeGroup group = value.getGroup();
        return new CodeValueResponse(
                value.getId(),
                group.getId(),
                group.getCodeId(),
                value.getCodeValue(),
                value.getValueName(),
                value.getDescription(),
                value.getSortOrder(),
                value.isUsed(),
                value.getCreatedAt(),
                value.getUpdatedAt());
    }
}
