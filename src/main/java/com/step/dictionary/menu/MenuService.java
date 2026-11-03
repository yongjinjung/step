package com.step.dictionary.menu;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.menu.dto.MenuRequest;
import com.step.dictionary.menu.dto.MenuResponse;
import com.step.dictionary.role.PermissionService;
import com.step.dictionary.role.RoleMenuRepository;
import com.step.dictionary.role.UserPermissions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메뉴 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>메뉴는 상위(TOP)-하위(LEFT) 2단까지다. 하위 메뉴는 상위 메뉴에만 붙고, 하위를 가진 메뉴는 다른 메뉴 밑으로 옮길 수 없다
 *       ({@code INVALID_MENU_PARENT}).</li>
 *   <li>상위 메뉴는 묶음이라 경로가 없고, 하위 메뉴는 경로가 필요하다 ({@code INVALID_MENU_PATH}). 경로는 전체에서 유일하다
 *       ({@code DUPLICATE_MENU_PATH}).</li>
 *   <li>하위 메뉴가 있는 메뉴는 삭제할 수 없다 ({@code MENU_HAS_CHILDREN}). 삭제하면 그 메뉴에 걸린 역할 권한과
 *       사용자 메뉴 지정도 함께 지운다.</li>
 *   <li>화면 표시용 조회는 사용 중이면서 <b>조회 권한이 있는</b> 메뉴만 돌려주고, 보여줄 하위가 없는 상위 메뉴는 뺀다.
 *       사용자에게 메뉴 지정({@code app_user_menu})이 있으면 그중에서 더 좁힌다.</li>
 * </ul>
 *
 * <p>메뉴의 화면 경로는 권한의 기준이기도 하다 ({@code MenuPaths}). 경로를 바꾸면 그 메뉴에 걸린 권한이
 * API와 이어지지 않으므로, 이미 쓰고 있는 메뉴의 경로는 바꾸지 않는 것이 좋다.
 *
 * <p>메뉴는 화면 구성이라 변경 이력({@code change_history})을 남기지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    /** 사용자별 메뉴 지정(좁히기) 조회와, 메뉴 삭제 시 정리용. */
    private final UserMenuRepository userMenuRepository;
    /** 메뉴 삭제 시 역할 권한 정리용. */
    private final RoleMenuRepository roleMenuRepository;
    /** 로그인한 사용자의 실제 권한 계산용. */
    private final PermissionService permissionService;

    public MenuService(MenuRepository menuRepository, UserMenuRepository userMenuRepository,
            RoleMenuRepository roleMenuRepository, PermissionService permissionService) {
        this.menuRepository = menuRepository;
        this.userMenuRepository = userMenuRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.permissionService = permissionService;
    }

    /**
     * 화면에 그릴 메뉴 트리 (위쪽 상위 메뉴 + 각 상위의 하위 메뉴).
     * 메뉴마다 그 사용자가 할 수 있는 동작(조회/등록/수정/삭제)이 함께 담겨 화면이 버튼을 정할 수 있다.
     *
     * @param username 로그인한 사용자 아이디
     */
    public List<MenuResponse> findNavigation(String username) {
        UserPermissions permissions = permissionService.of(username);
        List<Long> narrowed = userMenuRepository.findMenuIdsByUsername(username);
        return tree(false, permissions, narrowed.isEmpty() ? null : Set.copyOf(narrowed));
    }

    /**
     * 관리 화면용 전체 메뉴 트리. 미사용 메뉴와 권한 없는 메뉴도 모두 담는다.
     *
     * @param username 로그인한 사용자 아이디 (메뉴별 권한 표시용)
     */
    public List<MenuResponse> findAll(String username) {
        return tree(true, permissionService.of(username), null);
    }

    /**
     * 메뉴를 단건 조회한다 (하위 메뉴는 담지 않는다).
     *
     * @throws BusinessException MENU_NOT_FOUND (404)
     */
    public MenuResponse findById(Long id) {
        return MenuResponse.from(getMenu(id));
    }

    /**
     * 메뉴를 등록한다.
     *
     * @throws BusinessException INVALID_MENU_PARENT, INVALID_MENU_PATH (400), MENU_PARENT_NOT_FOUND (404),
     *                           DUPLICATE_MENU_PATH (409)
     */
    @Transactional
    public MenuResponse create(MenuRequest request) {
        Menu parent = getParent(request.parentId());
        validate(request, parent, null);

        Menu menu = menuRepository.save(new Menu(parent, request.name(), request.path(), request.icon(),
                request.sortOrder(), request.used()));
        return MenuResponse.from(menu);
    }

    /**
     * 메뉴를 수정한다 (전체 교체). 상위 메뉴를 바꿔 다른 묶음으로 옮길 수 있다.
     *
     * @throws BusinessException INVALID_MENU_PARENT, INVALID_MENU_PATH (400), MENU_NOT_FOUND,
     *                           MENU_PARENT_NOT_FOUND (404), DUPLICATE_MENU_PATH (409)
     */
    @Transactional
    public MenuResponse update(Long id, MenuRequest request) {
        Menu menu = getMenu(id);
        Menu parent = getParent(request.parentId());
        // 엔티티를 바꾸기 전에 검사해 중복이면 저장하지 않고 409로 끝낸다.
        validate(request, parent, id);

        menu.update(parent, request.name(), request.path(), request.icon(), request.sortOrder(), request.used());
        // MyBatis는 변경 감지가 없으므로 직접 update한다. updatedAt도 함께 갱신되어 응답에 쓰인다.
        menuRepository.save(menu);
        return MenuResponse.from(menu);
    }

    /**
     * 메뉴를 삭제한다. 하위 메뉴는 함께 지우지 않고 막는다 (실수로 묶음을 통째로 지우지 않도록).
     * 메뉴에 걸린 역할 권한과 사용자 메뉴 지정은 함께 지운다.
     *
     * @throws BusinessException MENU_NOT_FOUND (404), MENU_HAS_CHILDREN (409)
     */
    @Transactional
    public void delete(Long id) {
        Menu menu = getMenu(id);
        // DB FK 위반(500)에 맡기지 않고 먼저 확인해 409로 응답한다.
        if (menuRepository.existsByParentId(id)) {
            throw new BusinessException(ErrorCode.MENU_HAS_CHILDREN);
        }
        // 이 메뉴를 가리키는 권한 행을 먼저 지운다 (FK 위반 방지).
        roleMenuRepository.deleteAllByMenuId(id);
        userMenuRepository.deleteAllByMenuId(id);
        menuRepository.delete(menu);
    }

    /**
     * 전체 메뉴를 읽어 상위-하위 트리로 묶는다.
     *
     * @param includeHidden true면 미사용·권한 없는 메뉴도 모두 담는다 (관리 화면)
     * @param permissions   요청한 사용자의 실제 권한
     * @param narrowedIds   사용자에게 지정된 메뉴 ID. null이면 지정이 없어 역할 권한 그대로 본다
     */
    private List<MenuResponse> tree(boolean includeHidden, UserPermissions permissions, Set<Long> narrowedIds) {
        List<Menu> all = menuRepository.findAllByOrderBySortOrderAscIdAsc();
        // 하위 메뉴를 상위 메뉴 ID로 묶는다. 상위 메뉴는 조회 쿼리에서 조인해 채워져 있다.
        Map<Long, List<Menu>> childrenByParent = all.stream()
                .filter(menu -> !menu.isTop())
                .collect(Collectors.groupingBy(menu -> menu.getParent().getId(), LinkedHashMap::new,
                        Collectors.toList()));

        return all.stream()
                .filter(Menu::isTop)
                .filter(top -> includeHidden || top.isUsed())
                .map(top -> MenuResponse.of(top, childrenByParent.getOrDefault(top.getId(), List.of()).stream()
                        .filter(child -> includeHidden || visible(child, permissions, narrowedIds))
                        .map(child -> MenuResponse.of(child, List.of(), permissions.actionsOf(child.getId())))
                        .toList(), permissions.actionsOf(top.getId())))
                // 화면에 그릴 때 하위가 하나도 없는 상위 메뉴는 누를 것이 없어 뺀다.
                .filter(top -> includeHidden || !top.children().isEmpty())
                .toList();
    }

    /** 화면에 보일 메뉴인지: 사용 중 + 조회 권한 + (사용자 지정이 있으면) 지정된 메뉴. */
    private static boolean visible(Menu menu, UserPermissions permissions, Set<Long> narrowedIds) {
        return menu.isUsed()
                && permissions.canReadMenu(menu.getId())
                && (narrowedIds == null || narrowedIds.contains(menu.getId()));
    }

    /**
     * 메뉴 값 규칙을 검사한다.
     *
     * @param parent 요청한 상위 메뉴 (null이면 상위 메뉴로 등록)
     * @param id     수정 중인 메뉴 ID (등록이면 null)
     */
    private void validate(MenuRequest request, Menu parent, Long id) {
        String path = Menu.normalizePath(request.path());
        if (parent == null) {
            // 상위 메뉴는 하위 메뉴를 묶는 이름일 뿐이라 화면 경로를 두지 않는다.
            if (path != null) {
                throw new BusinessException(ErrorCode.INVALID_MENU_PATH);
            }
            return;
        }

        // 2단 제한: 하위 메뉴 밑에 또 메뉴를 둘 수 없고, 하위를 가진 메뉴를 다른 메뉴 밑으로 옮길 수도 없다.
        if (!parent.isTop() || parent.getId().equals(id)) {
            throw new BusinessException(ErrorCode.INVALID_MENU_PARENT);
        }
        if (id != null && menuRepository.existsByParentId(id)) {
            throw new BusinessException(ErrorCode.INVALID_MENU_PARENT);
        }
        if (path == null) {
            throw new BusinessException(ErrorCode.INVALID_MENU_PATH);
        }
        boolean duplicated = id == null
                ? menuRepository.existsByPath(path)
                : menuRepository.existsByPathAndIdNot(path, id);
        if (duplicated) {
            throw new BusinessException(ErrorCode.DUPLICATE_MENU_PATH);
        }
    }

    /** ID로 메뉴를 찾고, 없으면 MENU_NOT_FOUND(404)를 던진다. */
    private Menu getMenu(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
    }

    /** 상위 메뉴 ID가 없으면 상위 메뉴로 등록(null), 있는데 찾을 수 없으면 MENU_PARENT_NOT_FOUND(404). */
    private Menu getParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return menuRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_PARENT_NOT_FOUND));
    }
}
