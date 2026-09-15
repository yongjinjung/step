package com.step.dictionary.standard.domain.dto;

import com.step.dictionary.standard.domain.DataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 도메인 등록/수정 요청 본문.
 *
 * <p>여기서는 필드 단위 형식만 검증한다(400 INVALID_INPUT, fieldErrors 포함).
 * 타입과 길이/소수점의 조합 규칙은 서비스에서 {@link DataType#isValidSpec}으로 검증한다(400 INVALID_DOMAIN_SPEC).
 *
 * @param name             도메인명. 필수, 최대 100자
 * @param classifierWordId 분류어 단어 ID. 필수. 분류어가 아닌 단어면 400
 * @param dataType         데이터 타입. 필수. 없는 enum 값이면 JSON 파싱 오류로 400
 * @param dataLength       길이를 쓰는 타입(CHAR, VARCHAR, DECIMAL)만 입력한다. 1 이상
 * @param dataScale        DECIMAL만 입력한다. 0 이상, 생략하면 0
 * @param description      설명. 선택, 최대 1000자
 */
public record DomainRequest(
        @NotBlank @Size(max = 100)
        String name,

        @NotNull
        Long classifierWordId,

        @NotNull
        DataType dataType,

        @Positive
        Integer dataLength,

        @PositiveOrZero
        Integer dataScale,

        @Size(max = 1000)
        String description
) {
}
