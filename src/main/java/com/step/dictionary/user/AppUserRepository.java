package com.step.dictionary.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * 역할과 활성 여부로 계정 수를 센다.
     * {@code countByRoleAndEnabled(ADMIN, true)}로 활성 관리자 수를 구해 마지막 관리자 보호에 쓴다.
     * boolean 파라미터에도 엔티티의 YesNoConverter가 적용되어 'Y'/'N'으로 비교된다.
     */
    long countByRoleAndEnabled(Role role, boolean enabled);
}
