package com.step.dictionary.code.value.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 코드값 등록/수정 요청 본문. 길이 제한은 {@code com_code_value} 컬럼 길이와 맞춘다.
 *
 * @param codeValue   코드값. 필수, 최대 50자, 영문/숫자/'_'/'-'. 저장 시 대문자로 바뀐다 (예: 01, Y, ACTIVE)
 * @param valueName   코드값명. 필수, 최대 200자
 * @param description 설명. 선택, 최대 1000자
 * @param sortOrder   그룹 내 정렬 순서. 필수, 1 이상
 * @param used        사용 여부. 필수
 */
public record CodeValueRequest(
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "영문/숫자/_/-만 사용할 수 있습니다.")
        String codeValue,

        @NotBlank @Size(max = 200)
        String valueName,

        @Size(max = 1000)
        String description,

        @NotNull @Positive
        Integer sortOrder,

        @NotNull
        Boolean used
) {
}
