package com.step.dictionary.role;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 역할 리포지토리. */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /** 코드로 찾는다. 최초 관리자 생성과 기본 권한 채우기에 쓴다. 대문자로 정규화한 값을 넘겨야 한다. */
    Optional<Role> findByCode(String code);

    /** 코드 오름차순 전체 목록 (관리 화면). */
    List<Role> findAllByOrderByCodeAsc();

    /** 등록 시 코드 중복 확인. */
    boolean existsByCode(String code);

    /** 수정 시 코드 중복 확인. 자기 자신(id)은 제외한다. */
    boolean existsByCodeAndIdNot(String code, Long id);

    /**
     * 전권 역할을 가진 활성 사용자 수. 마지막 전권 사용자 보호({@code LAST_ADMIN_REQUIRED})에 쓴다.
     * 역할이 미사용이면 권한이 없는 것과 같으므로 세지 않는다.
     *
     * @param excludedUserId 이 사용자는 빼고 센다 (지금 바꾸려는 사용자)
     */
    @Query("""
            select count(distinct u.id) from AppUser u join u.roles r
            where r.superAdmin = true and r.used = true and u.enabled = true and u.id <> :excludedUserId
            """)
    long countOtherActiveSuperAdmins(@Param("excludedUserId") Long excludedUserId);

    /** 이 역할을 가진 사용자가 있는지. 역할 삭제 차단({@code ROLE_IN_USE})에 쓴다. */
    @Query("select count(u) > 0 from AppUser u join u.roles r where r.id = :roleId")
    boolean isAssignedToAnyUser(@Param("roleId") Long roleId);
}
