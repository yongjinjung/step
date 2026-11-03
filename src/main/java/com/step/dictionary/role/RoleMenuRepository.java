package com.step.dictionary.role;

import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 역할별 메뉴 권한 리포지토리 ({@link RoleMenuMapper} 사용).
 * 권한은 수정하지 않고 역할 단위로 지운 뒤 다시 넣는다({@code RoleService.replacePermissions}).
 */
@Repository
public class RoleMenuRepository {

    private final RoleMenuMapper mapper;

    public RoleMenuRepository(RoleMenuMapper mapper) {
        this.mapper = mapper;
    }

    /** 역할의 메뉴 권한 (메뉴 ID 순, 메뉴 포함). */
    public List<RoleMenu> findAllByRoleId(Long roleId) {
        return mapper.findAllByRoleId(roleId);
    }

    /** 사용자가 가진 사용 중인 역할들의 메뉴 권한 (메뉴 포함). 권한 계산({@code PermissionService})에 쓴다. */
    public List<RoleMenu> findAllByUsername(String username) {
        return mapper.findAllByUsername(username);
    }

    public long count() {
        return mapper.count();
    }

    /** 한 건씩 insert한다. 같은 (역할, 메뉴)가 이미 있으면 unique 제약 위반이므로 먼저 지워야 한다. */
    public List<RoleMenu> saveAll(Collection<RoleMenu> roleMenus) {
        roleMenus.forEach(mapper::insert);
        return List.copyOf(roleMenus);
    }

    public void deleteAllByRoleId(Long roleId) {
        mapper.deleteByRoleId(roleId);
    }

    public void deleteAllByMenuId(Long menuId) {
        mapper.deleteByMenuId(menuId);
    }
}
