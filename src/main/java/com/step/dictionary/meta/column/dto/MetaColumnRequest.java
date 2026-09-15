package com.step.dictionary.meta.column.dto;

import com.step.dictionary.meta.PhysicalNames;
import com.step.dictionary.standard.domain.DataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 컬럼 정의 등록/수정 요청 본문.
 *
 * <p>여기서는 필드 단위 형식만 검증한다(400 INVALID_INPUT). 타입/길이 조합과 기본키 NULL 규칙은 서비스에서 검증한다.
 * boolean 값을 기본형이 아닌 {@code Boolean + @NotNull}로 받는 이유는 누락을 false로 오인하지 않기 위해서다.
 *
 * @param physicalName 물리명. 필수, 최대 64자, 영문자로 시작하는 영문/숫자/'_'. 저장 시 대문자로 바뀐다
 * @param logicalName  논리명(한글명). 필수, 최대 200자
 * @param termId       매핑할 표준 용어 ID. 없으면 비표준(NO_TERM)으로 검출된다
 * @param dataType     데이터 타입. 필수
 * @param dataLength   길이를 쓰는 타입(CHAR, VARCHAR, DECIMAL)만 입력한다. 1 이상
 * @param dataScale    DECIMAL만 입력한다. 0 이상, 생략하면 0
 * @param nullable     NULL 허용 여부. 필수
 * @param primaryKey   기본키 여부. 필수. true면 nullable은 false여야 한다
 * @param seq          테이블 내 컬럼 순서. 필수, 1 이상
 */
public record MetaColumnRequest(
        @NotBlank @Size(max = PhysicalNames.MAX_LENGTH)
        @Pattern(regexp = PhysicalNames.PATTERN, message = PhysicalNames.PATTERN_MESSAGE)
        String physicalName,

        @NotBlank @Size(max = 200)
        String logicalName,

        Long termId,

        @NotNull
        DataType dataType,

        @Positive
        Integer dataLength,

        @PositiveOrZero
        Integer dataScale,

        @NotNull
        Boolean nullable,

        @NotNull
        Boolean primaryKey,

        @NotNull @Positive
        Integer seq
) {
}
