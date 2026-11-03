package com.step.dictionary.role;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 역할별 메뉴 권한 SQL 매퍼. 조회 결과에 메뉴({@code menu})를 함께 채운다(역할은 채우지 않는다). */
@Mapper
public interface RoleMenuMapper {

    /** 역할의 메뉴 권한 (메뉴 ID 순). */
    List<RoleMenu> findAllByRoleId(Long roleId);

    /** 사용자가 가진 사용 중인 역할들의 메뉴 권한. 같은 메뉴가 역할마다 여러 줄 나올 수 있다(합집합은 호출자가 계산). */
    List<RoleMenu> findAllByUsername(String username);

    long count();

    void insert(RoleMenu roleMenu);

    void deleteByRoleId(Long roleId);

    void deleteByMenuId(Long menuId);
}
