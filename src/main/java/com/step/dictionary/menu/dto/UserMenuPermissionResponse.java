package com.step.dictionary.menu.dto;

import java.util.List;

/**
 * 사용자 메뉴 권한 응답 (역할과 별개로 사용자에게 직접 지정하는 좁히기 설정).
 *
 * @param userId     사용자 ID
 * @param username   아이디
 * @param name       표시 이름
 * @param useDefault 지정된 메뉴가 없어 역할 권한 그대로 보는 상태인지
 * @param menuIds    지정된 메뉴 ID 목록 (useDefault면 빈 목록)
 */
public record UserMenuPermissionResponse(
        Long userId,
        String username,
        String name,
        boolean useDefault,
        List<Long> menuIds
) {
}
