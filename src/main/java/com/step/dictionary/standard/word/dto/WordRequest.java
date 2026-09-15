package com.step.dictionary.standard.word.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 단어 등록/수정 요청 본문. 컨트롤러의 {@code @Valid}로 검증하며 실패하면 400(INVALID_INPUT)이다.
 * 길이 제한은 {@code std_word} 컬럼 길이와 맞춘다.
 *
 * @param name         단어명(한글). 필수, 최대 100자
 * @param abbreviation 영문약어. 필수, 최대 50자, 영문자로 시작하는 영문/숫자.
 *                     용어 영문약어를 '_'로 이어 만들기 때문에 '_'는 허용하지 않는다. 저장 시 대문자로 바뀐다
 * @param englishName  영문명. 필수, 최대 200자
 * @param description  설명. 선택, 최대 1000자
 * @param classifier   분류어 여부. 필수 (기본형 boolean으로 받으면 누락 시 false로 들어가므로 Boolean + @NotNull)
 */
public record WordRequest(
        @NotBlank @Size(max = 100)
        String name,

        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9]*$", message = "영문자로 시작하는 영문/숫자만 사용할 수 있습니다.")
        String abbreviation,

        @NotBlank @Size(max = 200)
        String englishName,

        @Size(max = 1000)
        String description,

        @NotNull
        Boolean classifier
) {
}
