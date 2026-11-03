package com.step.dictionary.role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 역할 SQL 매퍼. */
@Mapper
public interface RoleMapper {

    Optional<Role> findById(Long id);

    Optional<Role> findByCode(String code);

    /** 전체 역할 (코드 오름차순). */
    List<Role> findAll();

    List<Role> findAllById(@Param("ids") Collection<Long> ids);

    long countByCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    /** 사용 중인 전권 역할을 가진 활성 사용자 수 (지정한 사용자 제외). */
    long countOtherActiveSuperAdmins(Long excludedUserId);

    /** 역할을 가진 사용자 수. */
    long countUsersByRoleId(Long roleId);

    void insert(Role role);

    void update(Role role);

    void deleteById(Long id);
}
