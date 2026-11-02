package com.step.dictionary.role;

import java.util.Map;
import java.util.Set;

/**
 * 로그인한 사용자의 실제 권한. 가진 역할들의 권한을 합친 결과다.
 *
 * @param superAdmin  전권 역할을 가졌는지. true면 모든 메뉴·동작이 허용된다
 * @param byMenuPath  메뉴 경로별 허용 동작 (예: {@code /words -> [READ, CREATE]})
 * @param byMenuId    메뉴 ID별 허용 동작. 메뉴 트리를 거를 때 쓴다
 */
public record UserPermissions(
        boolean superAdmin,
        Map<String, Set<PermissionAction>> byMenuPath,
        Map<Long, Set<PermissionAction>> byMenuId
) {

    /** 권한이 하나도 없는 사용자 (역할이 없거나 모두 미사용). */
    public static UserPermissions none() {
        return new UserPermissions(false, Map.of(), Map.of());
    }

    /** 전권 사용자. 메뉴별 권한을 보지 않는다. (이름을 {@code superAdmin}으로 두면 record 접근자와 겹친다.) */
    public static UserPermissions all() {
        return new UserPermissions(true, Map.of(), Map.of());
    }

    /** 메뉴 경로에 대해 그 동작을 할 수 있는지. */
    public boolean can(String menuPath, PermissionAction action) {
        return superAdmin || byMenuPath.getOrDefault(menuPath, Set.of()).contains(action);
    }

    /** 메뉴(ID)를 볼 수 있는지. 전권이면 모든 메뉴를 본다. */
    public boolean canReadMenu(Long menuId) {
        return superAdmin || byMenuId.getOrDefault(menuId, Set.of()).contains(PermissionAction.READ);
    }

    /** 메뉴(ID)에 허용된 동작. 화면이 버튼을 보여줄지 정하는 데 쓴다. */
    public Set<PermissionAction> actionsOf(Long menuId) {
        return superAdmin ? Set.of(PermissionAction.values()) : byMenuId.getOrDefault(menuId, Set.of());
    }
}
