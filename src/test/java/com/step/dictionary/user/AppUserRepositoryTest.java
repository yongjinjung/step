package com.step.dictionary.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.global.config.JpaConfig;
import com.step.dictionary.role.Role;
import com.step.dictionary.role.RoleRepository;
import com.step.dictionary.user.dto.UserSearchCondition;
import java.util.List;
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

    @Autowired
    private RoleRepository roleRepository;

    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        adminRole = roleRepository.save(new Role(Role.ADMIN_CODE, "관리자", null, true, true, true));
        userRole = roleRepository.save(new Role(Role.USER_CODE, "일반 사용자", null, false, true, true));

        userRepository.save(new AppUser("admin", "hash", "관리자", List.of(adminRole)));
        userRepository.save(new AppUser("hong", "hash", "홍길동", List.of(userRole)));
        AppUser kim = new AppUser("kim", "hash", "김철수", List.of(userRole));
        kim.update("김철수", List.of(userRole), false);
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
        assertThat(search(null, userRole.getId(), null)).containsExactly("hong", "kim");
        assertThat(search(null, userRole.getId(), false)).containsExactly("kim");
        assertThat(search(null, adminRole.getId(), null)).containsExactly("admin");
    }

    @Test
    @DisplayName("전권 역할을 가진 사용자인지 아이디로 확인한다")
    void hasSuperAdminRole() {
        assertThat(userRepository.hasSuperAdminRole("admin")).isTrue();
        assertThat(userRepository.hasSuperAdminRole("hong")).isFalse();
        assertThat(userRepository.hasSuperAdminRole("nobody")).isFalse();
    }

    @Test
    @DisplayName("전권 역할을 가진 다른 활성 사용자 수를 센다 (마지막 관리자 보호)")
    void countOtherActiveSuperAdmins() {
        Long adminId = userRepository.findByUsername("admin").orElseThrow().getId();

        assertThat(roleRepository.countOtherActiveSuperAdmins(adminId)).isZero();
        assertThat(roleRepository.countOtherActiveSuperAdmins(0L)).isEqualTo(1);
    }

    @Test
    @DisplayName("아이디로 계정을 찾는다")
    void findByUsername() {
        assertThat(userRepository.findByUsername("hong")).get().extracting(AppUser::getName).isEqualTo("홍길동");
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    private List<String> search(String keyword, Long roleId, Boolean enabled) {
        return userRepository.search(new UserSearchCondition(keyword, roleId, enabled), PageRequest.of(0, 20))
                .map(AppUser::getUsername)
                .getContent();
    }
}
