package com.step.dictionary.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 메뉴 등록/수정 요청 본문. 길이 제한은 {@code app_menu} 컬럼 길이와 맞춘다.
 *
 * <p>여기서는 필드 형식만 검증한다(400 INVALID_INPUT). 2단 구조와 경로 규칙은 서비스에서 검증한다.
 *
 * @param parentId  상위 메뉴 ID. 없으면 상위(TOP) 메뉴로 등록한다
 * @param name      메뉴명. 필수, 최대 100자
 * @param path      화면 경로. 하위 메뉴는 필수, 상위 메뉴는 비운다. '/'로 시작하는 영문/숫자/'_'/'-'/'/'
 *                  (앞의 '/'는 없어도 저장할 때 붙는다)
 * @param icon      아이콘 이름 (선택). 화면이 아는 이름이어야 아이콘이 보인다
 * @param sortOrder 같은 단계 안에서의 정렬 순서. 필수, 0 이상
 * @param used      사용 여부. 필수 (누락을 false로 오인하지 않도록 Boolean + @NotNull)
 * @param adminOnly 관리자 전용 여부. 필수
 */
public record MenuRequest(
        Long parentId,

        @NotBlank @Size(max = 100)
        String name,

        @Size(max = 200)
        @Pattern(regexp = "^/?[A-Za-z0-9/_-]*$", message = "경로는 영문/숫자/_/-와 /로만 쓸 수 있습니다.")
        String path,

        @Size(max = 50)
        String icon,

        @NotNull @PositiveOrZero
        Integer sortOrder,

        @NotNull
        Boolean used,

        @NotNull
        Boolean adminOnly
) {
}
