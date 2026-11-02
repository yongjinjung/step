package com.step.dictionary.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.menu.Menu;
import com.step.dictionary.menu.MenuRepository;
import com.step.dictionary.role.dto.RoleMenuPermission;
import com.step.dictionary.role.dto.RoleMenuPermissionRequest;
import com.step.dictionary.role.dto.RoleRequest;
import com.step.dictionary.role.dto.RoleResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMenuRepository roleMenuRepository;

    @Mock
    private MenuRepository menuRepository;

    @InjectMocks
    private RoleService roleService;

    private static Role role(long id, String code, boolean superAdmin, boolean builtIn) {
        Role role = new Role(code, code + " 역할", null, superAdmin, true, builtIn);
        ReflectionTestUtils.setField(role, "id", id);
        return role;
    }

    private static Menu menu(long id, String path) {
        Menu menu = new Menu(null, "메뉴" + id, path, null, 1, true);
        ReflectionTestUtils.setField(menu, "id", id);
        return menu;
    }

    private static RoleRequest request(String code, boolean superAdmin) {
        return new RoleRequest(code, "이름", null, superAdmin, true);
    }

    @Test
    @DisplayName("역할을 등록하면 코드를 대문자로 정규화하고 기본 제공 역할이 아니게 만든다")
    void create() {
        given(roleRepository.save(any(Role.class))).willAnswer(invocation -> invocation.getArgument(0));

        RoleResponse response = roleService.create(request("std_manager", false));

        assertThat(response.code()).isEqualTo("STD_MANAGER");
        assertThat(response.builtIn()).isFalse();
        verify(roleRepository).existsByCode("STD_MANAGER");
    }

    @Test
    @DisplayName("역할 코드는 대소문자 구분 없이 중복을 검사한다")
    void createDuplicateCode() {
        given(roleRepository.existsByCode("ADMIN")).willReturn(true);

        assertThatThrownBy(() -> roleService.create(request("admin", false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_ROLE_CODE);
        verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("기본 제공 역할은 코드와 전권 여부를 바꿀 수 없다")
    void updateBuiltInRole() {
        given(roleRepository.findById(1L)).willReturn(Optional.of(role(1L, "ADMIN", true, true)));

        assertThatThrownBy(() -> roleService.update(1L, request("SUPER", true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUILT_IN_ROLE);
    }

    @Test
    @DisplayName("기본 제공 역할도 이름과 사용 여부는 바꿀 수 있다")
    void updateBuiltInRoleName() {
        given(roleRepository.findById(1L)).willReturn(Optional.of(role(1L, "ADMIN", true, true)));

        RoleResponse response = roleService.update(1L, new RoleRequest("ADMIN", "시스템 관리자", "설명", true, true));

        assertThat(response.name()).isEqualTo("시스템 관리자");
    }

    @Test
    @DisplayName("기본 제공 역할은 삭제할 수 없다")
    void deleteBuiltInRole() {
        given(roleRepository.findById(1L)).willReturn(Optional.of(role(1L, "USER", false, true)));

        assertThatThrownBy(() -> roleService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUILT_IN_ROLE);
        verify(roleRepository, never()).delete(any());
    }

    @Test
    @DisplayName("사용자에게 부여된 역할은 삭제할 수 없다")
    void deleteAssignedRole() {
        given(roleRepository.findById(5L)).willReturn(Optional.of(role(5L, "STD_MANAGER", false, false)));
        given(roleRepository.isAssignedToAnyUser(5L)).willReturn(true);

        assertThatThrownBy(() -> roleService.delete(5L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROLE_IN_USE);
        verify(roleRepository, never()).delete(any());
    }

    @Test
    @DisplayName("권한을 저장하면 기존 권한을 지우고 교체하며, 권한이 하나도 없는 항목은 저장하지 않는다")
    void replacePermissions() {
        Role role = role(5L, "STD_MANAGER", false, false);
        given(roleRepository.findById(5L)).willReturn(Optional.of(role));
        given(menuRepository.findAllById(List.of(2L))).willReturn(List.of(menu(2L, "/words")));
        given(roleMenuRepository.findAllByRoleId(5L)).willReturn(List.of(
                new RoleMenu(role, menu(2L, "/words"), true, true, false, false)));

        List<RoleMenuPermission> saved = roleService.replacePermissions(5L, new RoleMenuPermissionRequest(List.of(
                new RoleMenuPermission(2L, null, null, true, true, false, false),
                // 권한이 하나도 없는 항목은 저장 대상이 아니다.
                new RoleMenuPermission(3L, null, null, false, false, false, false))));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).canCreate()).isTrue();
        verify(roleMenuRepository).deleteAllByRoleId(5L);
        // 같은 조합을 다시 넣기 전에 삭제를 먼저 내보내야 unique 제약에 걸리지 않는다.
        verify(roleMenuRepository).flush();
        verify(roleMenuRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("없는 메뉴에 권한을 주려 하면 아무것도 바꾸지 않는다")
    void replacePermissionsWithUnknownMenu() {
        given(roleRepository.findById(5L)).willReturn(Optional.of(role(5L, "STD_MANAGER", false, false)));
        given(menuRepository.findAllById(List.of(99L))).willReturn(List.of());

        assertThatThrownBy(() -> roleService.replacePermissions(5L, new RoleMenuPermissionRequest(List.of(
                new RoleMenuPermission(99L, null, null, true, false, false, false)))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MENU_NOT_FOUND);
        verify(roleMenuRepository, never()).deleteAllByRoleId(any());
    }

    @Test
    @DisplayName("역할 ID 목록에 없는 역할이 섞이면 거부한다 (사용자에게 역할 부여)")
    void getRolesWithUnknownId() {
        given(roleRepository.findAllById(List.of(1L, 99L))).willReturn(List.of(role(1L, "ADMIN", true, true)));

        assertThatThrownBy(() -> roleService.getRoles(List.of(1L, 99L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROLE_NOT_FOUND);
    }
}
