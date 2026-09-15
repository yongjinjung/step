package com.step.dictionary.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.step.dictionary.auth.dto.LoginRequest;
import com.step.dictionary.auth.dto.PasswordChangeRequest;
import com.step.dictionary.auth.dto.TokenResponse;
import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.AppUserRepository;
import com.step.dictionary.user.Role;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("아이디는 대소문자 구분 없이 조회하고 비밀번호가 맞으면 토큰을 발급한다")
    void login() {
        AppUser user = new AppUser("hong", "hash", "홍길동", Role.USER);
        TokenResponse token = new TokenResponse("token", "Bearer", 3600);
        given(userRepository.findByUsername("hong")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1!", "hash")).willReturn(true);
        given(tokenProvider.issue(user)).willReturn(token);

        assertThat(authService.login(new LoginRequest("HONG", "password1!"))).isEqualTo(token);
    }

    @Test
    @DisplayName("없는 아이디도 비밀번호 비교를 한 번 하고 같은 오류를 준다")
    void loginUnknownUser() {
        given(userRepository.findByUsername("nobody")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody", "password1!")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        verify(passwordEncoder).matches(eq("password1!"), any());
        verify(tokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 로그인할 수 없다")
    void loginWrongPassword() {
        given(userRepository.findByUsername("hong")).willReturn(Optional.of(new AppUser("hong", "hash", "홍길동", Role.USER)));
        given(passwordEncoder.matches("wrong-pass", "hash")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("hong", "wrong-pass")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        verify(tokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("비밀번호가 맞아도 비활성 계정은 로그인할 수 없다")
    void loginDisabled() {
        AppUser user = new AppUser("hong", "hash", "홍길동", Role.USER);
        user.update("홍길동", Role.USER, false);
        given(userRepository.findByUsername("hong")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1!", "hash")).willReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("hong", "password1!")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_DISABLED);
        verify(tokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("현재 비밀번호가 맞으면 새 비밀번호를 인코딩해 저장한다")
    void changePassword() {
        AppUser user = new AppUser("hong", "hash", "홍길동", Role.USER);
        given(userRepository.findByUsername("hong")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1!", "hash")).willReturn(true);
        given(passwordEncoder.encode("newpass12!")).willReturn("new-hash");

        authService.changePassword("hong", new PasswordChangeRequest("password1!", "newpass12!"));

        assertThat(user.getPassword()).isEqualTo("new-hash");
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 비밀번호를 바꿀 수 없다")
    void changePasswordWrongCurrent() {
        AppUser user = new AppUser("hong", "hash", "홍길동", Role.USER);
        given(userRepository.findByUsername("hong")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-pass", "hash")).willReturn(false);

        assertThatThrownBy(() -> authService.changePassword("hong", new PasswordChangeRequest("wrong-pass", "newpass12!")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CURRENT_PASSWORD);
        assertThat(user.getPassword()).isEqualTo("hash");
    }

    @Test
    @DisplayName("토큰의 계정이 없으면 내 정보를 조회할 수 없다")
    void meNotFound() {
        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me("ghost"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
