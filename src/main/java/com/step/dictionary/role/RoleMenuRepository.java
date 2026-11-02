package com.step.dictionary.role;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 역할별 메뉴 권한 리포지토리. 권한은 역할 단위로 통째로 읽고 교체한다. */
public interface RoleMenuRepository extends JpaRepository<RoleMenu, Long> {

    /** 역할의 권한 전체 (관리 화면). 메뉴 이름·경로를 함께 보여주므로 메뉴를 같이 읽는다. */
    @Query("select rm from RoleMenu rm join fetch rm.menu where rm.role.id = :roleId order by rm.menu.id")
    List<RoleMenu> findAllByRoleId(@Param("roleId") Long roleId);

    /**
     * 사용자가 가진 (사용 중인) 역할들의 권한 전체. 요청마다 권한을 계산할 때 쓴다.
     * 메뉴 경로·ID가 필요하므로 메뉴를 함께 읽는다.
     */
    @Query("""
            select rm from RoleMenu rm join fetch rm.menu
            where rm.role.used = true
              and rm.role in (select r from AppUser u join u.roles r where u.username = :username)
            """)
    List<RoleMenu> findAllByUsername(@Param("username") String username);

    /** 역할의 권한을 모두 지운다 (권한 교체, 역할 삭제). */
    void deleteAllByRoleId(Long roleId);

    /** 메뉴에 걸린 권한을 모두 지운다. 메뉴 삭제 전에 호출해 FK 위반을 막는다. */
    void deleteAllByMenuId(Long menuId);
}
