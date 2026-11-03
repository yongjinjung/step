package com.step.dictionary.role;

import com.step.dictionary.global.entity.CreatedAtAware;
import com.step.dictionary.menu.Menu;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * 역할이 메뉴 하나에 대해 가지는 권한 (조회/등록/수정/삭제).
 *
 * <p>테이블: {@code app_role_menu} (V11 마이그레이션). 역할-메뉴 조합은 유일하다.
 * 권한은 역할 단위로 통째로 교체하므로 수정 일시는 두지 않는다.
 *
 * <p>조회 권한이 없으면 그 메뉴는 화면에 보이지 않는다. 등록·수정·삭제 권한은 화면의 버튼과
 * 서버의 요청 검사({@code MenuAuthorizationManager})에 함께 쓰인다.
 */
public class RoleMenu implements CreatedAtAware {

    /** 역할 메뉴 권한 ID (AUTO_INCREMENT). */
    private Long id;

    /** 권한을 가진 역할. */
    private Role role;

    /** 권한 대상 메뉴. */
    private Menu menu;

    /** 조회 권한. */
    private boolean canRead;

    /** 등록 권한. */
    private boolean canCreate;

    /** 수정 권한. */
    private boolean canUpdate;

    /** 삭제 권한. */
    private boolean canDelete;

    /** 부여 일시. {@code JpaConfig}의 감사 설정이 채운다. */
    private LocalDateTime createdAt;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected RoleMenu() {
    }

    public RoleMenu(Role role, Menu menu, boolean canRead, boolean canCreate, boolean canUpdate, boolean canDelete) {
        this.role = role;
        this.menu = menu;
        this.canRead = canRead;
        this.canCreate = canCreate;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
    }

    /** 권한을 동작 집합으로 돌려준다. 여러 역할의 권한을 합칠 때 쓴다. */
    public Set<PermissionAction> actions() {
        Set<PermissionAction> actions = EnumSet.noneOf(PermissionAction.class);
        if (canRead) {
            actions.add(PermissionAction.READ);
        }
        if (canCreate) {
            actions.add(PermissionAction.CREATE);
        }
        if (canUpdate) {
            actions.add(PermissionAction.UPDATE);
        }
        if (canDelete) {
            actions.add(PermissionAction.DELETE);
        }
        return actions;
    }

    /** 아무 권한도 없는 행인지. 저장할 때 걸러낸다. */
    public boolean isEmpty() {
        return !canRead && !canCreate && !canUpdate && !canDelete;
    }

    // ---- 조회용 getter ----

    @Override
    public void markCreated(LocalDateTime now) {
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public Menu getMenu() {
        return menu;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public boolean isCanCreate() {
        return canCreate;
    }

    public boolean isCanUpdate() {
        return canUpdate;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
