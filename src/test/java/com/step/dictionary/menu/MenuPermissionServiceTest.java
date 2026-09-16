package com.step.dictionary.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.menu.dto.UserMenuPermissionRequest;
import com.step.dictionary.menu.dto.UserMenuPermissionResponse;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.AppUserRepository;
import com.step.dictionary.user.Role;
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
class MenuPermissionServiceTest {

    @Mock
    private UserMenuRepository userMenuRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private MenuRepository menuRepository;

    @InjectMocks
    private MenuPermissionService permissionService;

    private final AppUser user = user();

    private static AppUser user() {
        AppUser user = new AppUser("hong", "hash", "홍길동", Role.USER);
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }

    private static Menu menu(long id, String name, String path) {
        Menu menu = new Menu(null, name, path, null, 1, true, false);
        ReflectionTestUtils.setField(menu, "id", id);
        return menu;
    }

    @Test
    @DisplayName("지정된 메뉴가 없으면 기본 규칙을 쓰는 사용자로 돌려준다")
    void findDefault() {
        given(userRepository.findById(7L)).willReturn(Optional.of(user));
        given(userMenuRepository.findMenuIdsByUserId(7L)).willReturn(List.of());

        UserMenuPermissionResponse response = permissionService.findByUser(7L);

        assertThat(response.useDefault()).isTrue();
        assertThat(response.menuIds()).isEmpty();
        assertThat(response.username()).isEqualTo("hong");
    }

    @Test
    @DisplayName("메뉴 권한을 저장하면 기존 권한을 지우고 지정한 메뉴로 교체한다")
    void replace() {
        given(userRepository.findById(7L)).willReturn(Optional.of(user));
        given(menuRepository.findAllById(List.of(2L, 3L))).willReturn(List.of(menu(2L, "표준 단어", "/words"),
                menu(3L, "표준 도메인", "/domains")));

        UserMenuPermissionResponse response =
                permissionService.replace(7L, new UserMenuPermissionRequest(false, List.of(2L, 3L, 2L)));

        assertThat(response.useDefault()).isFalse();
        assertThat(response.menuIds()).containsExactly(2L, 3L);
        verify(userMenuRepository).deleteAllByUserId(7L);
        // 같은 조합을 다시 넣기 전에 삭제를 먼저 내보내야 unique 제약에 걸리지 않는다.
        verify(userMenuRepository).flush();
        verify(userMenuRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("기본 규칙으로 되돌리면 권한을 모두 지운다")
    void replaceWithDefault() {
        given(userRepository.findById(7L)).willReturn(Optional.of(user));
        given(menuRepository.findAllById(List.of())).willReturn(List.of());

        UserMenuPermissionResponse response =
                permissionService.replace(7L, new UserMenuPermissionRequest(true, List.of(2L)));

        assertThat(response.useDefault()).isTrue();
        verify(userMenuRepository).deleteAllByUserId(7L);
    }

    @Test
    @DisplayName("없는 메뉴를 지정하면 아무것도 바꾸지 않는다")
    void replaceWithUnknownMenu() {
        given(userRepository.findById(7L)).willReturn(Optional.of(user));
        given(menuRepository.findAllById(List.of(2L, 99L))).willReturn(List.of(menu(2L, "표준 단어", "/words")));

        assertThatThrownBy(() -> permissionService.replace(7L, new UserMenuPermissionRequest(false, List.of(2L, 99L))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MENU_NOT_FOUND);
        verify(userMenuRepository, never()).deleteAllByUserId(any());
        verify(userMenuRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("없는 사용자의 메뉴 권한은 다룰 수 없다")
    void unknownUser() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.findByUser(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
