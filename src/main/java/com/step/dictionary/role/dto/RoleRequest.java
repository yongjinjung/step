package com.step.dictionary.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 역할 등록/수정 요청 본문.
 *
 * @param code        역할 코드. 필수, 최대 50자, 영문자로 시작하는 영문/숫자/'_'. 저장 시 대문자로 바뀐다.
 *                    기본 제공 역할은 코드를 바꿀 수 없다
 * @param name        역할명. 필수, 최대 100자
 * @param description 설명. 선택, 최대 1000자
 * @param superAdmin  전권 여부. 필수. true면 메뉴 권한과 무관하게 모두 허용된다(기본 제공 역할은 바꿀 수 없다)
 * @param used        사용 여부. 필수. 미사용 역할의 권한은 무시된다
 */
public record RoleRequest(
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "영문자로 시작하는 영문/숫자/_만 사용할 수 있습니다.")
        String code,

        @NotBlank @Size(max = 100)
        String name,

        @Size(max = 1000)
        String description,

        @NotNull
        Boolean superAdmin,

        @NotNull
        Boolean used
) {
}
