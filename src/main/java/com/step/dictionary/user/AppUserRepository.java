package com.step.dictionary.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 사용자 리포지토리.
 * 기본 CRUD는 {@link JpaRepository}, 동적 검색은 {@link AppUserRepositoryCustom}이 맡는다.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long>, AppUserRepositoryCustom {

    /** 로그인과 내 정보 조회에 쓴다. 소문자로 정규화한 아이디를 넘겨야 한다. */
    Optional<AppUser> findByUsername(String username);

    /** 계정 생성 시 아이디 중복 확인. 소문자로 정규화한 아이디를 넘겨야 한다. */
    boolean existsByUsername(String username);

    /**
     * 전권 역할(사용 중인 것)을 가진 사용자인지. 요청마다 권한을 계산할 때 가장 먼저 확인한다
     * ({@code PermissionService}). 전권이면 메뉴 권한을 보지 않는다.
     */
    @Query("""
            select count(u) > 0 from AppUser u join u.roles r
            where u.username = :username and r.superAdmin = true and r.used = true
            """)
    boolean hasSuperAdminRole(@Param("username") String username);
}
