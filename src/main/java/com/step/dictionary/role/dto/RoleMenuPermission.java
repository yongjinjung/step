package com.step.dictionary.role.dto;

import com.step.dictionary.role.RoleMenu;
import jakarta.validation.constraints.NotNull;

/**
 * 역할이 메뉴 하나에 가지는 권한. 조회 응답과 저장 요청이 같은 모양이라 함께 쓴다.
 *
 * @param menuId    메뉴 ID (필수)
 * @param menuName  메뉴명 (응답에만 채운다)
 * @param menuPath  메뉴 경로 (응답에만 채운다)
 * @param canRead   조회 권한. 없으면 메뉴가 보이지 않는다
 * @param canCreate 등록 권한
 * @param canUpdate 수정 권한
 * @param canDelete 삭제 권한
 */
public record RoleMenuPermission(
        @NotNull
        Long menuId,

        String menuName,

        String menuPath,

        @NotNull
        Boolean canRead,

        @NotNull
        Boolean canCreate,

        @NotNull
        Boolean canUpdate,

        @NotNull
        Boolean canDelete
) {

    /** 저장된 권한을 응답으로 변환한다. 메뉴를 읽으므로 트랜잭션 안에서 호출한다. */
    public static RoleMenuPermission from(RoleMenu roleMenu) {
        return new RoleMenuPermission(
                roleMenu.getMenu().getId(),
                roleMenu.getMenu().getName(),
                roleMenu.getMenu().getPath(),
                roleMenu.isCanRead(),
                roleMenu.isCanCreate(),
                roleMenu.isCanUpdate(),
                roleMenu.isCanDelete());
    }

    /** 권한이 하나도 없는 항목인지. 저장할 때 걸러낸다. */
    public boolean isEmpty() {
        return !canRead && !canCreate && !canUpdate && !canDelete;
    }
}
