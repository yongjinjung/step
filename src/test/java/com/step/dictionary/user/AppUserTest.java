package com.step.dictionary.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppUserTest {

    @Test
    @DisplayName("계정을 만들면 아이디는 소문자로 저장되고 활성 상태다")
    void create() {
        AppUser user = new AppUser("Hong.GilDong", "{bcrypt}hash", "홍길동", Role.USER);

        assertThat(user.getUsername()).isEqualTo("hong.gildong");
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("활성 상태의 ADMIN만 활성 관리자로 본다")
    void isActiveAdmin() {
        AppUser admin = new AppUser("admin", "hash", "관리자", Role.ADMIN);
        AppUser user = new AppUser("user", "hash", "사용자", Role.USER);

        assertThat(admin.isActiveAdmin()).isTrue();
        assertThat(user.isActiveAdmin()).isFalse();

        admin.update("관리자", Role.ADMIN, false);
        assertThat(admin.isActiveAdmin()).isFalse();
    }

    @Test
    @DisplayName("비밀번호 변경은 인코딩된 값을 그대로 저장한다")
    void changePassword() {
        AppUser user = new AppUser("user", "old-hash", "사용자", Role.USER);

        user.changePassword("new-hash");

        assertThat(user.getPassword()).isEqualTo("new-hash");
    }
}
