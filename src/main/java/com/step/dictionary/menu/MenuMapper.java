package com.step.dictionary.menu;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 화면 메뉴 SQL 매퍼. 조회 결과에 상위 메뉴({@code parent}, 한 단계)를 함께 채운다. */
@Mapper
public interface MenuMapper {

    Optional<Menu> findById(Long id);

    /** 전체 메뉴 (정렬 순서, 같으면 ID 오름차순). */
    List<Menu> findAll();

    List<Menu> findAllById(@Param("ids") Collection<Long> ids);

    long count();

    long countByPath(@Param("path") String path, @Param("excludeId") Long excludeId);

    long countByParentId(Long parentId);

    void insert(Menu menu);

    void update(Menu menu);

    void deleteById(Long id);
}
