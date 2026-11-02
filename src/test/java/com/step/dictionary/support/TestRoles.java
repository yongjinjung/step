package com.step.dictionary.support;

import com.step.dictionary.role.Role;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 단위 테스트용 역할.
 *
 * <p>저장하지 않고 쓰는 객체라 ID를 직접 채워 준다(권한 계산이 ID를 쓴다).
 * 통합 테스트는 {@link IntegrationTest}가 DB에 실제 역할을 만든다.
 */
public final class TestRoles {

    /** 전권 관리자 역할 (기본 제공). */
    public static Role admin() {
        return role(1L, Role.ADMIN_CODE, "관리자", true);
    }

    /** 일반 사용자 역할 (기본 제공, 전권 아님). */
    public static Role user() {
        return role(2L, Role.USER_CODE, "일반 사용자", false);
    }

    /** 관리자 역할만 가진 계정을 만들 때 쓴다. */
    public static List<Role> adminOnly() {
        return List.of(admin());
    }

    /** 일반 사용자 역할만 가진 계정을 만들 때 쓴다. */
    public static List<Role> userOnly() {
        return List.of(user());
    }

    private static Role role(long id, String code, String name, boolean superAdmin) {
        Role role = new Role(code, name, null, superAdmin, true, true);
        ReflectionTestUtils.setField(role, "id", id);
        return role;
    }

    /** 인스턴스 생성 방지. */
    private TestRoles() {
    }
}
