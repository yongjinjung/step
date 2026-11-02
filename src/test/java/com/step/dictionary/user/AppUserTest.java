package com.step.dictionary.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.support.TestRoles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppUserTest {

    @Test
    @DisplayName("계정을 만들면 아이디는 소문자로 저장되고 활성 상태다")
    void create() {
        AppUser user = new AppUser("Hong.GilDong", "{bcrypt}hash", "홍길동", TestRoles.userOnly());

        assertThat(user.getUsername()).isEqualTo("hong.gildong");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getRoles()).hasSize(1);
    }

    @Test
    @DisplayName("전권 역할을 가졌는지 알 수 있다")
    void hasSuperAdminRole() {
        AppUser admin = new AppUser("admin", "hash", "관리자", TestRoles.adminOnly());
        AppUser user = new AppUser("user", "hash", "사용자", TestRoles.userOnly());

        assertThat(admin.hasSuperAdminRole()).isTrue();
        assertThat(user.hasSuperAdminRole()).isFalse();
    }

    @Test
    @DisplayName("수정하면 역할이 보낸 목록으로 교체된다")
    void update() {
        AppUser admin = new AppUser("admin", "hash", "관리자", TestRoles.adminOnly());

        admin.update("관리자", TestRoles.userOnly(), false);

        assertThat(admin.hasSuperAdminRole()).isFalse();
        assertThat(admin.isEnabled()).isFalse();
        assertThat(admin.getRoles()).extracting("code").containsExactly("USER");
    }

    @Test
    @DisplayName("비밀번호 변경은 인코딩된 값을 그대로 저장한다")
    void changePassword() {
        AppUser user = new AppUser("user", "old-hash", "사용자", TestRoles.userOnly());

        user.changePassword("new-hash");

        assertThat(user.getPassword()).isEqualTo("new-hash");
    }
}
