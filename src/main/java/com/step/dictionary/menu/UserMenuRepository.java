package com.step.dictionary.menu;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 사용자별 메뉴 권한 리포지토리.
 * 권한은 사용자 한 명 단위로 통째로 읽고 교체하므로 메뉴 ID 목록만 다룬다.
 */
public interface UserMenuRepository extends JpaRepository<UserMenu, Long> {

    /** 관리 화면에서 보여줄, 그 사용자에게 지정된 메뉴 ID 목록. 비어 있으면 기본 규칙을 쓰는 사용자다. */
    @Query("select um.menu.id from UserMenu um where um.user.id = :userId order by um.menu.id")
    List<Long> findMenuIdsByUserId(@Param("userId") Long userId);

    /**
     * 로그인한 사용자(아이디)에게 지정된 메뉴 ID 목록. 메뉴 트리를 거를 때 쓴다.
     * 사용자 테이블을 따로 읽지 않도록 아이디로 바로 조회한다.
     */
    @Query("select um.menu.id from UserMenu um where um.user.username = :username")
    List<Long> findMenuIdsByUsername(@Param("username") String username);

    /** 사용자의 권한을 모두 지운다 (권한 교체, 기본 규칙으로 되돌리기). */
    void deleteAllByUserId(Long userId);

    /** 메뉴에 걸린 권한을 모두 지운다. 메뉴 삭제 전에 호출해 FK 위반을 막는다. */
    void deleteAllByMenuId(Long menuId);
}
