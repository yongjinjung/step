package com.step.dictionary.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.role.RoleRepository;
import com.step.dictionary.role.RoleService;
import com.step.dictionary.support.TestRoles;
import com.step.dictionary.user.dto.PasswordResetRequest;
import com.step.dictionary.user.dto.UserCreateRequest;
import com.step.dictionary.user.dto.UserResponse;
import com.step.dictionary.user.dto.UserUpdateRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    private static final Long ADMIN_ROLE_ID = 1L;
    private static final Long USER_ROLE_ID = 2L;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserService userService;

    @Test
    @DisplayName("계정을 만들면 아이디를 소문자로 정규화하고 비밀번호를 인코딩해 저장한다")
    void create() {
        given(roleService.getRoles(List.of(USER_ROLE_ID))).willReturn(List.of(TestRoles.user()));
        given(passwordEncoder.encode("password1!")).willReturn("{bcrypt}hash");
        given(userRepository.save(any(AppUser.class))).willAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(
                new UserCreateRequest("Hong", "password1!", "홍길동", List.of(USER_ROLE_ID)));

        assertThat(response.username()).isEqualTo("hong");
        assertThat(response.roles()).extracting(UserResponse.UserRole::code).containsExactly("USER");
        verify(userRepository).existsByUsername("hong");
    }

    @Test
    @DisplayName("아이디는 대소문자 구분 없이 중복을 검사한다")
    void createDuplicateUsername() {
        given(roleService.getRoles(List.of(USER_ROLE_ID))).willReturn(List.of(TestRoles.user()));
        given(userRepository.existsByUsername("hong")).willReturn(true);

        assertThatThrownBy(() -> userService.create(
                new UserCreateRequest("HONG", "password1!", "홍길동", List.of(USER_ROLE_ID))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_USERNAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("역할을 하나도 지정하지 않으면 계정을 만들 수 없다")
    void createWithoutRole() {
        assertThatThrownBy(() -> userService.create(new UserCreateRequest("hong", "password1!", "홍길동", List.of())))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_ROLE_REQUIRED);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("마지막 전권 사용자의 역할은 뺄 수 없다")
    void updateLastSuperAdminRole() {
        AppUser admin = new AppUser("admin", "hash", "관리자", TestRoles.adminOnly());
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(roleService.getRoles(List.of(USER_ROLE_ID))).willReturn(List.of(TestRoles.user()));
        given(roleRepository.countOtherActiveSuperAdmins(1L)).willReturn(0L);

        assertThatThrownBy(() -> userService.update(1L, new UserUpdateRequest("관리자", List.of(USER_ROLE_ID), true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LAST_ADMIN_REQUIRED);
        assertThat(admin.hasSuperAdminRole()).isTrue();
    }

    @Test
    @DisplayName("마지막 전권 사용자는 비활성화할 수 없다")
    void disableLastSuperAdmin() {
        AppUser admin = new AppUser("admin", "hash", "관리자", TestRoles.adminOnly());
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(roleService.getRoles(List.of(ADMIN_ROLE_ID))).willReturn(List.of(TestRoles.admin()));
        given(roleRepository.countOtherActiveSuperAdmins(1L)).willReturn(0L);

        assertThatThrownBy(() -> userService.update(1L, new UserUpdateRequest("관리자", List.of(ADMIN_ROLE_ID), false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LAST_ADMIN_REQUIRED);
        assertThat(admin.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("다른 전권 사용자가 남아 있으면 역할을 바꿀 수 있다")
    void updateRoleWithOtherSuperAdmin() {
        AppUser admin = new AppUser("admin", "hash", "관리자", TestRoles.adminOnly());
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(roleService.getRoles(List.of(USER_ROLE_ID))).willReturn(List.of(TestRoles.user()));
        given(roleRepository.countOtherActiveSuperAdmins(1L)).willReturn(1L);

        UserResponse response = userService.update(1L, new UserUpdateRequest("관리자", List.of(USER_ROLE_ID), true));

        assertThat(response.roles()).extracting(UserResponse.UserRole::code).containsExactly("USER");
        assertThat(response.superAdmin()).isFalse();
    }

    @Test
    @DisplayName("전권이 아닌 사용자는 마지막 관리자 검사 없이 수정된다")
    void updateNormalUser() {
        AppUser user = new AppUser("user", "hash", "사용자", TestRoles.userOnly());
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(roleService.getRoles(List.of(USER_ROLE_ID))).willReturn(List.of(TestRoles.user()));

        UserResponse response = userService.update(1L, new UserUpdateRequest("김사용", List.of(USER_ROLE_ID), false));

        assertThat(response.name()).isEqualTo("김사용");
        assertThat(response.enabled()).isFalse();
        verify(roleRepository, never()).countOtherActiveSuperAdmins(any());
    }

    @Test
    @DisplayName("비밀번호 초기화는 인코딩한 값으로 바꾼다")
    void resetPassword() {
        AppUser user = new AppUser("user", "old-hash", "사용자", TestRoles.userOnly());
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("new-pass1!")).willReturn("{bcrypt}new");

        userService.resetPassword(1L, new PasswordResetRequest("new-pass1!"));

        assertThat(user.getPassword()).isEqualTo("{bcrypt}new");
    }

    @Test
    @DisplayName("없는 역할을 지정하면 계정을 만들 수 없다")
    void createWithUnknownRole() {
        given(roleService.getRoles(List.of(99L)))
                .willThrow(new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        assertThatThrownBy(() -> userService.create(
                new UserCreateRequest("hong", "password1!", "홍길동", List.of(99L))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROLE_NOT_FOUND);
    }
}
