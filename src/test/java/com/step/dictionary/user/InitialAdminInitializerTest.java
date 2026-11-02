package com.step.dictionary.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.step.dictionary.role.Role;
import com.step.dictionary.role.RoleRepository;
import com.step.dictionary.support.TestRoles;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class InitialAdminInitializerTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("사용자가 없고 비밀번호가 설정돼 있으면 관리자 역할을 가진 최초 관리자를 만든다")
    void createWhenEmpty() {
        given(userRepository.count()).willReturn(0L);
        given(roleRepository.findByCode(Role.ADMIN_CODE)).willReturn(Optional.of(TestRoles.admin()));
        given(passwordEncoder.encode("initial-pass1!")).willReturn("hash");
        given(userRepository.save(any(AppUser.class))).willAnswer(invocation -> invocation.getArgument(0));

        initializer(new InitialAdminProperties("Admin", "initial-pass1!", "관리자")).run(null);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("admin");
        assertThat(saved.getValue().getPassword()).isEqualTo("hash");
        assertThat(saved.getValue().hasSuperAdminRole()).isTrue();
    }

    @Test
    @DisplayName("사용자가 이미 있으면 아무것도 하지 않는다")
    void skipWhenUsersExist() {
        given(userRepository.count()).willReturn(1L);

        initializer(new InitialAdminProperties("admin", "initial-pass1!", "관리자")).run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 설정이 없으면 관리자를 만들지 않는다")
    void skipWithoutPassword() {
        given(userRepository.count()).willReturn(0L);

        initializer(new InitialAdminProperties("admin", " ", "관리자")).run(null);

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("기본 관리자 역할이 없으면 권한 없는 계정이 생기지 않도록 만들지 않는다")
    void skipWithoutAdminRole() {
        given(userRepository.count()).willReturn(0L);
        given(roleRepository.findByCode(Role.ADMIN_CODE)).willReturn(Optional.empty());

        initializer(new InitialAdminProperties("admin", "initial-pass1!", "관리자")).run(null);

        verify(userRepository, never()).save(any());
    }

    private InitialAdminInitializer initializer(InitialAdminProperties properties) {
        return new InitialAdminInitializer(userRepository, roleRepository, passwordEncoder, properties);
    }
}
