package com.step.dictionary.menu;

import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 사용자별 메뉴 지정 리포지토리 ({@link UserMenuMapper} 사용).
 * 지정은 수정하지 않고 사용자 단위로 지운 뒤 다시 넣는다({@code MenuPermissionService.replace}).
 */
@Repository
public class UserMenuRepository {

    private final UserMenuMapper mapper;

    public UserMenuRepository(UserMenuMapper mapper) {
        this.mapper = mapper;
    }

    /** 사용자에게 지정된 메뉴 ID (오름차순). 비어 있으면 기본 규칙대로 보여준다. */
    public List<Long> findMenuIdsByUserId(Long userId) {
        return mapper.findMenuIdsByUserId(userId);
    }

    /** 로그인 아이디로 지정된 메뉴 ID. 화면 메뉴 조회({@code MenuService.findNavigation})에 쓴다. */
    public List<Long> findMenuIdsByUsername(String username) {
        return mapper.findMenuIdsByUsername(username);
    }

    /** 한 건씩 insert한다. 같은 (사용자, 메뉴)가 이미 있으면 unique 제약 위반이므로 먼저 지워야 한다. */
    public List<UserMenu> saveAll(Collection<UserMenu> userMenus) {
        userMenus.forEach(mapper::insert);
        return List.copyOf(userMenus);
    }

    public void deleteAllByUserId(Long userId) {
        mapper.deleteByUserId(userId);
    }

    public void deleteAllByMenuId(Long menuId) {
        mapper.deleteByMenuId(menuId);
    }
}
