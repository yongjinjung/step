package com.step.dictionary.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.menu.dto.MenuRequest;
import com.step.dictionary.menu.dto.MenuResponse;
import com.step.dictionary.role.PermissionAction;
import com.step.dictionary.role.PermissionService;
import com.step.dictionary.role.RoleMenuRepository;
import com.step.dictionary.role.UserPermissions;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private UserMenuRepository userMenuRepository;

    @Mock
    private RoleMenuRepository roleMenuRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private MenuService menuService;

    /** 저장된 메뉴처럼 ID를 가진 엔티티를 만든다 (트리를 묶을 때 상위 메뉴 ID가 필요하다). */
    private static Menu menu(long id, Menu parent, String name, String path, boolean used) {
        Menu menu = new Menu(parent, name, path, null, 1, used);
        ReflectionTestUtils.setField(menu, "id", id);
        return menu;
    }

    private static MenuRequest request(Long parentId, String name, String path) {
        return new MenuRequest(parentId, name, path, null, 1, true);
    }

    /** 지정한 메뉴만 조회할 수 있는 권한. */
    private static UserPermissions readOnly(Long... menuIds) {
        return new UserPermissions(false, Map.of(),
                List.of(menuIds).stream().collect(java.util.stream.Collectors.toMap(
                        id -> id, id -> Set.of(PermissionAction.READ))));
    }

    @Test
    @DisplayName("상위 메뉴에는 화면 경로를 둘 수 없다")
    void createTopWithPath() {
        assertThatThrownBy(() -> menuService.create(request(null, "표준 사전", "/words")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_MENU_PATH);
        verify(menuRepository, never()).save(any());
    }

    @Test
    @DisplayName("하위 메뉴는 화면 경로가 있어야 한다")
    void createSubWithoutPath() {
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu(1L, null, "표준 사전", null, true)));

        assertThatThrownBy(() -> menuService.create(request(1L, "표준 단어", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_MENU_PATH);
    }

    @Test
    @DisplayName("하위 메뉴 아래에는 메뉴를 만들 수 없다 (2단까지)")
    void createUnderSubMenu() {
        Menu top = menu(1L, null, "표준 사전", null, true);
        given(menuRepository.findById(2L)).willReturn(Optional.of(menu(2L, top, "표준 단어", "/words", true)));

        assertThatThrownBy(() -> menuService.create(request(2L, "더 아래", "/words/sub")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_MENU_PARENT);
    }

    @Test
    @DisplayName("화면 경로가 중복되면 등록할 수 없다")
    void createDuplicatePath() {
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu(1L, null, "표준 사전", null, true)));
        given(menuRepository.existsByPath("/words")).willReturn(true);

        assertThatThrownBy(() -> menuService.create(request(1L, "표준 단어", "/words")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_MENU_PATH);
        verify(menuRepository, never()).save(any());
    }

    @Test
    @DisplayName("하위 메뉴를 가진 메뉴는 다른 메뉴 밑으로 옮길 수 없다")
    void moveMenuWithChildren() {
        Menu other = menu(1L, null, "메타 관리", null, true);
        given(menuRepository.findById(2L)).willReturn(Optional.of(menu(2L, null, "표준 사전", null, true)));
        given(menuRepository.findById(1L)).willReturn(Optional.of(other));
        given(menuRepository.existsByParentId(2L)).willReturn(true);

        assertThatThrownBy(() -> menuService.update(2L, request(1L, "표준 사전", "/words")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_MENU_PARENT);
    }

    @Test
    @DisplayName("하위 메뉴가 있는 메뉴는 삭제할 수 없다")
    void deleteWithChildren() {
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu(1L, null, "표준 사전", null, true)));
        given(menuRepository.existsByParentId(1L)).willReturn(true);

        assertThatThrownBy(() -> menuService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MENU_HAS_CHILDREN);
        verify(menuRepository, never()).delete(any());
    }

    @Test
    @DisplayName("메뉴를 삭제하면 역할 권한과 사용자 메뉴 지정도 함께 지운다")
    void deleteClearsPermissions() {
        Menu menu = menu(1L, null, "표준 사전", null, true);
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));

        menuService.delete(1L);

        verify(roleMenuRepository).deleteAllByMenuId(1L);
        verify(userMenuRepository).deleteAllByMenuId(1L);
        verify(menuRepository).delete(menu);
    }

    @Test
    @DisplayName("화면 표시용 조회는 미사용·조회 권한 없는 메뉴와 보여줄 하위가 없는 상위 메뉴를 뺀다")
    void findNavigation() {
        Menu standard = menu(1L, null, "표준 사전", null, true);
        Menu work = menu(4L, null, "업무", null, true);
        given(menuRepository.findAllByOrderBySortOrderAscIdAsc()).willReturn(List.of(
                standard,
                menu(2L, standard, "표준 단어", "/words", true),
                menu(3L, standard, "사용자 관리", "/users", true),
                work,
                menu(5L, work, "신청/승인", "/requests", false)));
        // 표준 단어와 신청/승인만 조회 권한이 있다 (신청/승인 메뉴는 미사용이라 결국 빠진다).
        given(permissionService.of("hong")).willReturn(readOnly(2L, 5L));
        given(userMenuRepository.findMenuIdsByUsername("hong")).willReturn(List.of());

        List<MenuResponse> navigation = menuService.findNavigation("hong");

        assertThat(navigation).extracting(MenuResponse::name).containsExactly("표준 사전");
        assertThat(navigation.get(0).children()).extracting(MenuResponse::name).containsExactly("표준 단어");
        assertThat(navigation.get(0).children().get(0).canRead()).isTrue();
        assertThat(navigation.get(0).children().get(0).canCreate()).isFalse();
    }

    @Test
    @DisplayName("전권 사용자는 사용 중인 메뉴를 모두 보고 모든 동작이 허용된다")
    void findNavigationAsSuperAdmin() {
        Menu standard = menu(1L, null, "표준 사전", null, true);
        given(menuRepository.findAllByOrderBySortOrderAscIdAsc()).willReturn(List.of(
                standard,
                menu(2L, standard, "표준 단어", "/words", true)));
        given(permissionService.of("admin")).willReturn(UserPermissions.all());
        given(userMenuRepository.findMenuIdsByUsername("admin")).willReturn(List.of());

        List<MenuResponse> navigation = menuService.findNavigation("admin");

        assertThat(navigation.get(0).children()).extracting(MenuResponse::name).containsExactly("표준 단어");
        assertThat(navigation.get(0).children().get(0).canDelete()).isTrue();
    }

    @Test
    @DisplayName("사용자에게 메뉴를 지정하면 역할 권한 중에서 더 좁혀 보여준다")
    void findNavigationWithUserMenus() {
        Menu standard = menu(1L, null, "표준 사전", null, true);
        given(menuRepository.findAllByOrderBySortOrderAscIdAsc()).willReturn(List.of(
                standard,
                menu(2L, standard, "표준 단어", "/words", true),
                menu(3L, standard, "표준 도메인", "/domains", true)));
        given(permissionService.of("hong")).willReturn(UserPermissions.all());
        given(userMenuRepository.findMenuIdsByUsername("hong")).willReturn(List.of(3L));

        List<MenuResponse> navigation = menuService.findNavigation("hong");

        assertThat(navigation.get(0).children()).extracting(MenuResponse::name).containsExactly("표준 도메인");
    }

    @Test
    @DisplayName("관리용 조회는 미사용 메뉴와 하위가 없는 상위 메뉴까지 모두 돌려준다")
    void findAll() {
        Menu standard = menu(1L, null, "표준 사전", null, true);
        Menu empty = menu(3L, null, "준비 중", null, false);
        given(menuRepository.findAllByOrderBySortOrderAscIdAsc()).willReturn(List.of(
                standard,
                menu(2L, standard, "표준 단어", "/words", false),
                empty));
        given(permissionService.of("admin")).willReturn(UserPermissions.all());

        List<MenuResponse> all = menuService.findAll("admin");

        assertThat(all).extracting(MenuResponse::name).containsExactly("표준 사전", "준비 중");
        assertThat(all.get(0).children()).extracting(MenuResponse::name).containsExactly("표준 단어");
        assertThat(all.get(1).children()).isEmpty();
    }
}
