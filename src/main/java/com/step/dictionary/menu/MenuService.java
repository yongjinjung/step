package com.step.dictionary.menu;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.menu.dto.MenuRequest;
import com.step.dictionary.menu.dto.MenuResponse;
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
 *   <li>하위 메뉴가 있는 메뉴는 삭제할 수 없다 ({@code MENU_HAS_CHILDREN}).</li>
 *   <li>화면 표시용 조회는 사용 중인 메뉴만, 관리자 전용은 관리자에게만 돌려주고, 보여줄 하위가 없는 상위 메뉴는 뺀다.</li>
 * </ul>
 *
 * <p>메뉴는 화면 구성이라 변경 이력({@code change_history})을 남기지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    /** 사용자별 메뉴 권한 조회와, 메뉴 삭제 시 권한 정리용. */
    private final UserMenuRepository userMenuRepository;

    public MenuService(MenuRepository menuRepository, UserMenuRepository userMenuRepository) {
        this.menuRepository = menuRepository;
        this.userMenuRepository = userMenuRepository;
    }

    /**
     * 화면에 그릴 메뉴 트리 (위쪽 상위 메뉴 + 각 상위의 하위 메뉴).
     *
     * <p>사용자별 메뉴 권한이 지정돼 있으면 그 메뉴만 남긴다. 지정이 없으면 기본 규칙(사용 중 + 역할)대로 본다.
     *
     * @param username 로그인한 사용자 아이디 (메뉴 권한 조회용)
     * @param admin    관리자 여부. 관리자 전용 메뉴를 포함할지 정한다
     */
    public List<MenuResponse> findNavigation(String username, boolean admin) {
        List<Long> allowed = userMenuRepository.findMenuIdsByUsername(username);
        return tree(false, admin, allowed.isEmpty() ? null : Set.copyOf(allowed));
    }

    /** 관리 화면용 전체 메뉴 트리. 미사용·관리자 전용 메뉴도 모두 담는다. */
    public List<MenuResponse> findAll() {
        return tree(true, true, null);
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
                request.sortOrder(), request.used(), request.adminOnly()));
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
        // 엔티티를 바꾸기 전에 검사한다. 변경 후 exists 쿼리를 날리면 auto flush로 unique 제약 위반이 먼저 터진다.
        validate(request, parent, id);

        menu.update(parent, request.name(), request.path(), request.icon(), request.sortOrder(), request.used(),
                request.adminOnly());
        // 즉시 update를 실행해 갱신된 updatedAt으로 응답한다.
        menuRepository.flush();
        return MenuResponse.from(menu);
    }

    /**
     * 메뉴를 삭제한다. 하위 메뉴는 함께 지우지 않고 막는다 (실수로 묶음을 통째로 지우지 않도록).
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
        // 이 메뉴를 가리키는 사용자 권한을 먼저 지운다 (FK 위반 방지).
        userMenuRepository.deleteAllByMenuId(id);
        userMenuRepository.flush();
        menuRepository.delete(menu);
    }

    /**
     * 전체 메뉴를 읽어 상위-하위 트리로 묶는다.
     *
     * @param includeHidden true면 미사용·관리자 전용도 모두 담는다 (관리 화면)
     * @param admin         관리자 여부 (화면 표시용 조회에서 관리자 전용 메뉴를 담을지)
     * @param allowedIds    사용자에게 지정된 메뉴 ID. null이면 지정이 없어 기본 규칙대로 본다.
     *                      지정은 하위 메뉴에만 적용하고, 상위 메뉴는 보여줄 하위가 있으면 남는다
     */
    private List<MenuResponse> tree(boolean includeHidden, boolean admin, Set<Long> allowedIds) {
        List<Menu> all = menuRepository.findAllByOrderBySortOrderAscIdAsc();
        // 하위 메뉴를 상위 메뉴 ID로 묶는다. 상위 메뉴는 이미 영속성 컨텍스트에 있어 프록시라도 조회가 더 나가지 않는다.
        Map<Long, List<Menu>> childrenByParent = all.stream()
                .filter(menu -> !menu.isTop())
                .collect(Collectors.groupingBy(menu -> menu.getParent().getId(), LinkedHashMap::new,
                        Collectors.toList()));

        return all.stream()
                .filter(Menu::isTop)
                .filter(top -> visible(top, includeHidden, admin))
                .map(top -> MenuResponse.of(top, childrenByParent.getOrDefault(top.getId(), List.of()).stream()
                        .filter(child -> visible(child, includeHidden, admin))
                        .filter(child -> allowedIds == null || allowedIds.contains(child.getId()))
                        .map(MenuResponse::from)
                        .toList()))
                // 화면에 그릴 때 하위가 하나도 없는 상위 메뉴는 누를 것이 없어 뺀다.
                .filter(top -> includeHidden || !top.children().isEmpty())
                .toList();
    }

    /** 화면에 보일 메뉴인지. 관리 화면(includeHidden)에서는 모두 보인다. */
    private static boolean visible(Menu menu, boolean includeHidden, boolean admin) {
        return includeHidden || (menu.isUsed() && (!menu.isAdminOnly() || admin));
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
