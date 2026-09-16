package com.step.dictionary.code.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 코드 그룹 등록/수정 요청 본문. 컨트롤러의 {@code @Valid}로 검증하며 실패하면 400(INVALID_INPUT)이다.
 * 길이 제한은 {@code com_code_group} 컬럼 길이와 맞춘다.
 *
 * @param codeId      코드ID. 필수, 최대 50자, 영문자로 시작하는 영문/숫자/'_'. 저장 시 대문자로 바뀐다
 * @param codeName    코드명. 필수, 최대 200자
 * @param description 설명. 선택, 최대 1000자
 * @param used        사용 여부. 필수 (누락을 false로 오인하지 않도록 Boolean + @NotNull)
 */
public record CodeGroupRequest(
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "영문자로 시작하는 영문/숫자/_만 사용할 수 있습니다.")
        String codeId,

        @NotBlank @Size(max = 200)
        String codeName,

        @Size(max = 1000)
        String description,

        @NotNull
        Boolean used
) {
}
