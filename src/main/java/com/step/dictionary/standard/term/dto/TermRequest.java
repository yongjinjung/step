package com.step.dictionary.standard.term.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 용어 등록/수정 요청 본문. 용어명과 영문약어는 입력받지 않고 구성 단어에서 생성한다.
 *
 * <pre>
 * {"wordIds": [1, 2], "domainId": 3, "description": "고객을 식별하는 번호"}
 * </pre>
 *
 * @param wordIds     구성 단어 ID (조합 순서대로). 필수, 1~10개, 요소 null 불가, 같은 ID 반복 가능.
 *                    마지막 단어는 분류어여야 한다
 * @param domainId    마지막 단어를 분류어로 갖는 도메인 ID. 필수
 * @param description 설명. 선택, 최대 1000자
 */
public record TermRequest(
        @NotEmpty @Size(max = 10)
        List<@NotNull Long> wordIds,

        @NotNull
        Long domainId,

        @Size(max = 1000)
        String description
) {
}
