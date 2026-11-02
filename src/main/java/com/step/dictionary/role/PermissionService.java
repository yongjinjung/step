package com.step.dictionary.role;

import com.step.dictionary.user.AppUserRepository;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인한 사용자의 실제 권한을 계산한다.
 *
 * <p>권한 = 가진 역할(사용 중인 것)들의 메뉴별 권한 합집합. 전권 역할이 하나라도 있으면 모두 허용이다.
 *
 * <p>요청마다 두 번의 조회가 일어난다(전권 여부, 권한 목록). 메뉴·역할 수가 적어 그대로 두었고,
 * 느려지면 요청 범위 캐시나 짧은 TTL 캐시를 붙이면 된다. 권한 변경이 다음 요청에 바로 반영되는 장점이 있다.
 */
@Service
@Transactional(readOnly = true)
public class PermissionService {

    private final AppUserRepository userRepository;
    private final RoleMenuRepository roleMenuRepository;

    public PermissionService(AppUserRepository userRepository, RoleMenuRepository roleMenuRepository) {
        this.userRepository = userRepository;
        this.roleMenuRepository = roleMenuRepository;
    }

    /**
     * 아이디로 실제 권한을 계산한다.
     *
     * @param username 로그인 아이디 (JWT subject). 없는 사용자면 권한 없음
     */
    public UserPermissions of(String username) {
        if (userRepository.hasSuperAdminRole(username)) {
            return UserPermissions.all();
        }
        Map<String, Set<PermissionAction>> byPath = new HashMap<>();
        Map<Long, Set<PermissionAction>> byId = new HashMap<>();
        for (RoleMenu roleMenu : roleMenuRepository.findAllByUsername(username)) {
            Set<PermissionAction> actions = roleMenu.actions();
            if (actions.isEmpty()) {
                continue;
            }
            Long menuId = roleMenu.getMenu().getId();
            byId.computeIfAbsent(menuId, key -> EnumSet.noneOf(PermissionAction.class)).addAll(actions);
            String path = roleMenu.getMenu().getPath();
            if (path != null) {
                byPath.computeIfAbsent(path, key -> EnumSet.noneOf(PermissionAction.class)).addAll(actions);
            }
        }
        return byId.isEmpty() ? UserPermissions.none() : new UserPermissions(false, byPath, byId);
    }
}
