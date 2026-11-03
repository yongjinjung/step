package com.step.dictionary.user;

import com.step.dictionary.role.Role;
import com.step.dictionary.user.dto.UserSearchCondition;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/**
 * 사용자 계정 SQL 매퍼. 사용자 행과 역할(조인 테이블 {@code app_user_role})을 따로 읽고,
 * {@link AppUserRepository}가 둘을 묶는다.
 */
@Mapper
public interface AppUserMapper {

    Optional<AppUser> findById(Long id);

    Optional<AppUser> findByUsername(String username);

    /** 여러 사용자의 역할을 한 번에 읽는다. */
    List<UserRoleRow> findRolesByUserIds(@Param("userIds") Collection<Long> userIds);

    long count();

    long countByUsername(String username);

    /** 사용 중인 전권 역할을 가졌으면 1 이상. */
    long countSuperAdminRoles(String username);

    List<AppUser> search(@Param("c") UserSearchCondition condition, @Param("p") Pageable pageable);

    long countSearch(@Param("c") UserSearchCondition condition);

    void insert(AppUser user);

    /** 아이디는 바꾸지 않는다. */
    void update(AppUser user);

    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    void deleteUserRoles(Long userId);

    /** {@link #findRolesByUserIds} 결과 한 줄. */
    class UserRoleRow {

        private Long userId;
        private Role role;

        public Long getUserId() {
            return userId;
        }

        public Role getRole() {
            return role;
        }
    }
}
