package com.step.dictionary.meta.table.dto;

import com.step.dictionary.meta.PhysicalNames;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 테이블 정의 등록/수정 요청 본문. 검증 실패 시 400(INVALID_INPUT).
 *
 * @param physicalName 물리명. 필수, 최대 64자, 영문자로 시작하는 영문/숫자/'_'. 저장 시 대문자로 바뀐다
 * @param logicalName  논리명(한글명). 필수, 최대 200자
 * @param description  설명. 선택, 최대 1000자
 */
public record MetaTableRequest(
        @NotBlank @Size(max = PhysicalNames.MAX_LENGTH)
        @Pattern(regexp = PhysicalNames.PATTERN, message = PhysicalNames.PATTERN_MESSAGE)
        String physicalName,

        @NotBlank @Size(max = 200)
        String logicalName,

        @Size(max = 1000)
        String description
) {
}
