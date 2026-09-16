package com.step.dictionary.menu.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 사용자 메뉴 권한 저장 요청 본문. 지정 내용을 통째로 교체한다.
 *
 * @param useDefault true면 지정을 모두 지우고 기본 규칙(사용 중 + 역할)으로 되돌린다. 이때 {@code menuIds}는 무시한다
 * @param menuIds    보여줄 메뉴 ID 목록. 하위(LEFT) 메뉴를 지정하며, 상위 메뉴는 보여줄 하위가 있으면 자동으로 보인다.
 *                   빈 목록이면 메뉴가 하나도 보이지 않는다
 */
public record UserMenuPermissionRequest(
        @NotNull
        Boolean useDefault,

        List<Long> menuIds
) {
}
