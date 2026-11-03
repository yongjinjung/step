package com.step.dictionary.menu;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.menu.dto.UserMenuPermissionRequest;
import com.step.dictionary.menu.dto.UserMenuPermissionResponse;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.AppUserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자별 메뉴 권한 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>권한 행이 없는 사용자는 역할이 주는 메뉴를 그대로 본다.</li>
 *   <li>권한을 지정하면 그중에서도 지정한 메뉴만 본다. 역할에 없는 메뉴를 지정해도 보이지 않으므로,
 *       이 설정은 보이는 메뉴를 넓히는 것이 아니라 좁히는 쪽으로만 동작한다.</li>
 *   <li>권한은 통째로 교체한다 (PUT 의미). {@code useDefault}면 행을 모두 지워 기본 규칙으로 되돌린다.</li>
 *   <li>메뉴가 보이는지의 문제일 뿐, 기능 권한은 각 API가 그대로 검사한다.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class MenuPermissionService {

    private final UserMenuRepository userMenuRepository;
    /** 대상 사용자 확인용. */
    private final AppUserRepository userRepository;
    /** 지정한 메뉴가 실제로 있는지 확인용. */
    private final MenuRepository menuRepository;

    public MenuPermissionService(UserMenuRepository userMenuRepository, AppUserRepository userRepository,
            MenuRepository menuRepository) {
        this.userMenuRepository = userMenuRepository;
        this.userRepository = userRepository;
        this.menuRepository = menuRepository;
    }

    /**
     * 사용자의 메뉴 권한을 조회한다.
     *
     * @throws BusinessException USER_NOT_FOUND (404)
     */
    public UserMenuPermissionResponse findByUser(Long userId) {
        AppUser user = getUser(userId);
        return response(user, userMenuRepository.findMenuIdsByUserId(userId));
    }

    /**
     * 사용자의 메뉴 권한을 통째로 바꾼다.
     *
     * @throws BusinessException USER_NOT_FOUND, MENU_NOT_FOUND (404)
     */
    @Transactional
    public UserMenuPermissionResponse replace(Long userId, UserMenuPermissionRequest request) {
        AppUser user = getUser(userId);
        List<Long> menuIds = request.useDefault() || request.menuIds() == null
                ? List.of()
                : request.menuIds().stream().distinct().toList();

        // 지정한 메뉴가 모두 있는지 먼저 확인한다 (하나라도 없으면 아무것도 바꾸지 않는다).
        List<Menu> menus = menuRepository.findAllById(menuIds);
        if (menus.size() != menuIds.size()) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }

        // 통째로 교체한다. 삭제가 먼저 실행되므로 같은 (사용자, 메뉴) 조합을 다시 넣어도 unique 제약에 걸리지 않는다.
        userMenuRepository.deleteAllByUserId(userId);
        userMenuRepository.saveAll(menus.stream().map(menu -> new UserMenu(user, menu)).toList());

        return response(user, menus.stream().map(Menu::getId).toList());
    }

    private static UserMenuPermissionResponse response(AppUser user, List<Long> menuIds) {
        return new UserMenuPermissionResponse(user.getId(), user.getUsername(), user.getName(),
                menuIds.isEmpty(), menuIds);
    }

    /** ID로 사용자를 찾고, 없으면 USER_NOT_FOUND(404)를 던진다. */
    private AppUser getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
