package com.step.dictionary.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.global.config.JpaConfig;
import com.step.dictionary.user.dto.UserSearchCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaConfig.class)
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.save(new AppUser("admin", "hash", "관리자", Role.ADMIN));
        userRepository.save(new AppUser("hong", "hash", "홍길동", Role.USER));
        AppUser kim = new AppUser("kim", "hash", "김철수", Role.USER);
        kim.update("김철수", Role.USER, false);
        userRepository.save(kim);
    }

    @Test
    @DisplayName("키워드는 아이디(대소문자 무시)와 이름에 부분 일치하고 아이디 오름차순으로 정렬한다")
    void searchByKeyword() {
        assertThat(search("HON", null, null)).containsExactly("hong");
        assertThat(search("철수", null, null)).containsExactly("kim");
        assertThat(search(null, null, null)).containsExactly("admin", "hong", "kim");
    }

    @Test
    @DisplayName("역할과 활성 여부로 필터링한다")
    void searchByRoleAndEnabled() {
        assertThat(search(null, Role.USER, null)).containsExactly("hong", "kim");
        assertThat(search(null, Role.USER, false)).containsExactly("kim");
    }

    @Test
    @DisplayName("역할과 활성 여부로 계정 수를 센다")
    void countByRoleAndEnabled() {
        assertThat(userRepository.countByRoleAndEnabled(Role.ADMIN, true)).isEqualTo(1);
        assertThat(userRepository.countByRoleAndEnabled(Role.USER, true)).isEqualTo(1);
        assertThat(userRepository.countByRoleAndEnabled(Role.USER, false)).isEqualTo(1);
    }

    @Test
    @DisplayName("아이디로 계정을 찾는다")
    void findByUsername() {
        assertThat(userRepository.findByUsername("hong")).get().extracting(AppUser::getName).isEqualTo("홍길동");
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    private java.util.List<String> search(String keyword, Role role, Boolean enabled) {
        return userRepository.search(new UserSearchCondition(keyword, role, enabled), PageRequest.of(0, 20))
                .map(AppUser::getUsername)
                .getContent();
    }
}
