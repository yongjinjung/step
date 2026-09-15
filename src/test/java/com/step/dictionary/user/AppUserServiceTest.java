package com.step.dictionary.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.user.dto.PasswordResetRequest;
import com.step.dictionary.user.dto.UserCreateRequest;
import com.step.dictionary.user.dto.UserResponse;
import com.step.dictionary.user.dto.UserUpdateRequest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserService userService;

    @Test
    @DisplayName("계정을 만들면 아이디를 소문자로 정규화하고 비밀번호는 인코딩해 저장한다")
    void create() {
        given(passwordEncoder.encode("password1!")).willReturn("{bcrypt}hash");
        given(userRepository.save(any(AppUser.class))).willAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(new UserCreateRequest("Hong", "password1!", "홍길동", Role.USER));

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(response.username()).isEqualTo("hong");
        assertThat(response.enabled()).isTrue();
        verify(userRepository).existsByUsername("hong");
    }

    @Test
    @DisplayName("아이디는 대소문자 구분 없이 중복을 검사한다")
    void createDuplicate() {
        given(userRepository.existsByUsername("hong")).willReturn(true);

        assertThatThrownBy(() -> userService.create(new UserCreateRequest("HONG", "password1!", "홍길동", Role.USER)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_USERNAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("마지막 활성 관리자는 USER로 강등할 수 없다")
    void demoteLastAdmin() {
        AppUser admin = new AppUser("admin", "hash", "관리자", Role.ADMIN);
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(userRepository.countByRoleAndEnabled(Role.ADMIN, true)).willReturn(1L);

        assertThatThrownBy(() -> userService.update(1L, new UserUpdateRequest("관리자", Role.USER, true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LAST_ADMIN_REQUIRED);
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("마지막 활성 관리자는 비활성화할 수 없다")
    void disableLastAdmin() {
        AppUser admin = new AppUser("admin", "hash", "관리자", Role.ADMIN);
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(userRepository.countByRoleAndEnabled(Role.ADMIN, true)).willReturn(1L);

        assertThatThrownBy(() -> userService.update(1L, new UserUpdateRequest("관리자", Role.ADMIN, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LAST_ADMIN_REQUIRED);
        assertThat(admin.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("다른 활성 관리자가 있으면 관리자를 강등할 수 있다")
    void demoteAdminWhenOthersExist() {
        AppUser admin = new AppUser("admin", "hash", "관리자", Role.ADMIN);
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(userRepository.countByRoleAndEnabled(Role.ADMIN, true)).willReturn(2L);

        UserResponse response = userService.update(1L, new UserUpdateRequest("관리자", Role.USER, true));

        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("관리자 권한이 바뀌지 않는 수정은 관리자 수를 세지 않는다")
    void updateWithoutLosingAdmin() {
        AppUser user = new AppUser("user", "hash", "사용자", Role.USER);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.update(1L, new UserUpdateRequest("김사용", Role.USER, false));

        assertThat(response.name()).isEqualTo("김사용");
        assertThat(response.enabled()).isFalse();
        verify(userRepository, never()).countByRoleAndEnabled(any(), any(Boolean.class));
    }

    @Test
    @DisplayName("비밀번호를 초기화하면 새 비밀번호를 인코딩해 저장한다")
    void resetPassword() {
        AppUser user = new AppUser("user", "old-hash", "사용자", Role.USER);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newpass12!")).willReturn("new-hash");

        userService.resetPassword(1L, new PasswordResetRequest("newpass12!"));

        assertThat(user.getPassword()).isEqualTo("new-hash");
    }

    @Test
    @DisplayName("없는 계정은 조회할 수 없다")
    void findByIdNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
