package com.step.dictionary.menu.dto;

import com.step.dictionary.user.Role;
import java.util.List;

/**
 * 사용자 메뉴 권한 응답.
 *
 * @param userId     사용자 ID
 * @param username   아이디
 * @param name       표시 이름
 * @param role       역할 (관리자 전용 메뉴가 보이는지와 관련 있어 함께 내려준다)
 * @param useDefault 지정된 메뉴가 없어 기본 규칙(사용 중 + 역할)대로 보는 상태인지
 * @param menuIds    지정된 메뉴 ID 목록 (useDefault면 빈 목록)
 */
public record UserMenuPermissionResponse(
        Long userId,
        String username,
        String name,
        Role role,
        boolean useDefault,
        List<Long> menuIds
) {
}
