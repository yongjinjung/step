package com.step.dictionary.menu;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 사용자별 메뉴 지정 SQL 매퍼. 화면 표시와 관리에는 메뉴 ID만 필요해 ID 목록으로 조회한다. */
@Mapper
public interface UserMenuMapper {

    /** 사용자에게 지정된 메뉴 ID (오름차순). */
    List<Long> findMenuIdsByUserId(Long userId);

    List<Long> findMenuIdsByUsername(String username);

    void insert(UserMenu userMenu);

    void deleteByUserId(Long userId);

    void deleteByMenuId(Long menuId);
}
