package com.step.dictionary.role;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.menu.Menu;
import com.step.dictionary.menu.MenuRepository;
import com.step.dictionary.role.dto.RoleMenuPermission;
import com.step.dictionary.role.dto.RoleMenuPermissionRequest;
import com.step.dictionary.role.dto.RoleRequest;
import com.step.dictionary.role.dto.RoleResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 역할과 역할별 메뉴 권한 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>역할 코드는 대문자로 정규화한 뒤 유일해야 한다 ({@code DUPLICATE_ROLE_CODE}).</li>
 *   <li>기본 제공 역할(ADMIN, USER)은 삭제할 수 없고 코드와 전권 여부를 바꿀 수 없다 ({@code BUILT_IN_ROLE}).
 *       이름·설명·사용 여부·메뉴 권한은 바꿀 수 있다.</li>
 *   <li>사용자에게 부여된 역할은 삭제할 수 없다 ({@code ROLE_IN_USE}). 먼저 사용자에게서 빼야 한다.</li>
 *   <li>권한은 역할 단위로 통째로 교체한다. 네 권한이 모두 없는 항목은 저장하지 않는다.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    /** 권한을 줄 메뉴 확인용. */
    private final MenuRepository menuRepository;

    public RoleService(RoleRepository roleRepository, RoleMenuRepository roleMenuRepository,
            MenuRepository menuRepository) {
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.menuRepository = menuRepository;
    }

    /** 전체 역할 (코드 오름차순). */
    public List<RoleResponse> findAll() {
        return roleRepository.findAllByOrderByCodeAsc().stream().map(RoleResponse::from).toList();
    }

    /**
     * 역할을 단건 조회한다.
     *
     * @throws BusinessException ROLE_NOT_FOUND (404)
     */
    public RoleResponse findById(Long id) {
        return RoleResponse.from(getRole(id));
    }

    /**
     * 역할을 등록한다.
     *
     * @throws BusinessException DUPLICATE_ROLE_CODE (409)
     */
    @Transactional
    public RoleResponse create(RoleRequest request) {
        String code = Role.normalizeCode(request.code());
        if (roleRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ROLE_CODE);
        }

        // 화면에서 만든 역할은 기본 제공 역할이 아니다 (삭제·코드 변경이 가능하다).
        Role role = roleRepository.save(new Role(
                code, request.name(), request.description(), request.superAdmin(), request.used(), false));
        return RoleResponse.from(role);
    }

    /**
     * 역할을 수정한다.
     *
     * @throws BusinessException BUILT_IN_ROLE (400), ROLE_NOT_FOUND (404), DUPLICATE_ROLE_CODE (409)
     */
    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = getRole(id);
        String code = Role.normalizeCode(request.code());
        // 기본 제공 역할은 코드와 전권 여부가 시스템 동작의 기준이라 바꿀 수 없다.
        if (role.isBuiltIn() && (!role.getCode().equals(code) || role.isSuperAdmin() != request.superAdmin())) {
            throw new BusinessException(ErrorCode.BUILT_IN_ROLE);
        }
        // 엔티티를 바꾸기 전에 검사한다. 변경 후 조회하면 auto flush로 unique 제약 위반이 먼저 터진다.
        if (roleRepository.existsByCodeAndIdNot(code, id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ROLE_CODE);
        }

        role.update(code, request.name(), request.description(), request.superAdmin(), request.used());
        // 즉시 update를 실행해 갱신된 updatedAt으로 응답한다.
        roleRepository.flush();
        return RoleResponse.from(role);
    }

    /**
     * 역할을 삭제한다. 역할이 가진 메뉴 권한도 함께 지운다.
     *
     * @throws BusinessException BUILT_IN_ROLE (400), ROLE_NOT_FOUND (404), ROLE_IN_USE (409)
     */
    @Transactional
    public void delete(Long id) {
        Role role = getRole(id);
        if (role.isBuiltIn()) {
            throw new BusinessException(ErrorCode.BUILT_IN_ROLE);
        }
        // DB FK 위반(500)에 맡기지 않고 먼저 확인해 409로 응답한다.
        if (roleRepository.isAssignedToAnyUser(id)) {
            throw new BusinessException(ErrorCode.ROLE_IN_USE);
        }
        roleMenuRepository.deleteAllByRoleId(id);
        roleMenuRepository.flush();
        roleRepository.delete(role);
    }

    /**
     * 역할의 메뉴 권한 목록.
     *
     * @throws BusinessException ROLE_NOT_FOUND (404)
     */
    public List<RoleMenuPermission> findPermissions(Long roleId) {
        getRole(roleId);
        return roleMenuRepository.findAllByRoleId(roleId).stream().map(RoleMenuPermission::from).toList();
    }

    /**
     * 역할의 메뉴 권한을 통째로 교체한다.
     *
     * @throws BusinessException ROLE_NOT_FOUND, MENU_NOT_FOUND (404)
     */
    @Transactional
    public List<RoleMenuPermission> replacePermissions(Long roleId, RoleMenuPermissionRequest request) {
        Role role = getRole(roleId);
        // 권한이 하나도 없는 항목은 저장하지 않는다 (없는 것과 같다).
        List<RoleMenuPermission> requested = request.permissions().stream()
                .filter(permission -> !permission.isEmpty())
                .toList();

        // 지정한 메뉴가 모두 있는지 먼저 확인한다 (하나라도 없으면 아무것도 바꾸지 않는다).
        List<Long> menuIds = requested.stream().map(RoleMenuPermission::menuId).distinct().toList();
        Map<Long, Menu> menus = menuRepository.findAllById(menuIds).stream()
                .collect(Collectors.toMap(Menu::getId, Function.identity()));
        if (menus.size() != menuIds.size()) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }

        roleMenuRepository.deleteAllByRoleId(roleId);
        // Hibernate는 insert를 delete보다 먼저 실행하므로, 삭제를 먼저 내보내지 않으면
        // 같은 (역할, 메뉴) 조합을 다시 넣을 때 unique 제약(uk_app_role_menu)에 걸린다.
        roleMenuRepository.flush();
        roleMenuRepository.saveAll(requested.stream()
                .map(permission -> new RoleMenu(role, menus.get(permission.menuId()), permission.canRead(),
                        permission.canCreate(), permission.canUpdate(), permission.canDelete()))
                .toList());

        return findPermissions(roleId);
    }

    /**
     * ID 목록으로 역할을 찾는다. 하나라도 없으면 ROLE_NOT_FOUND(404).
     * 사용자에게 역할을 부여할 때 쓴다 ({@code AppUserService}).
     */
    public List<Role> getRoles(List<Long> roleIds) {
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.stream().distinct().count()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return roles;
    }

    /** ID로 역할을 찾고, 없으면 ROLE_NOT_FOUND(404)를 던진다. */
    private Role getRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
    }
}
