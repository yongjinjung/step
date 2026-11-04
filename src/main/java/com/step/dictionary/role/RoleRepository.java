package com.step.dictionary.role;

import com.step.dictionary.global.mybatis.InClause;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 역할 리포지토리 ({@link RoleMapper} 사용). 엔티티를 바꾼 뒤에는 {@link #save}를 호출한다. */
@Repository
public class RoleRepository {

    private final RoleMapper mapper;

    public RoleRepository(RoleMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 등록, 있으면 수정한다. */
    public Role save(Role role) {
        if (role.getId() == null) {
            mapper.insert(role);
        } else {
            mapper.update(role);
        }
        return role;
    }

    public Optional<Role> findById(Long id) {
        return mapper.findById(id);
    }

    /** 역할 코드로 찾는다. 대문자로 정규화한 값을 넘긴다. */
    public Optional<Role> findByCode(String code) {
        return mapper.findByCode(code);
    }

    /** 전체 역할 (코드 오름차순). */
    public List<Role> findAllByOrderByCodeAsc() {
        return mapper.findAll();
    }

    /** 여러 ID를 한 번에 찾는다. 없는 ID는 결과에서 빠진다. */
    public List<Role> findAllById(Collection<Long> ids) {
        return InClause.query(ids, mapper::findAllById);
    }

    public boolean existsByCode(String code) {
        return mapper.countByCode(code, null) > 0;
    }

    public boolean existsByCodeAndIdNot(String code, Long id) {
        return mapper.countByCode(code, id) > 0;
    }

    /**
     * 지정한 사용자를 빼고, 사용 중인 전권 역할을 가진 활성 사용자 수.
     * 마지막 전권 사용자를 강등·비활성화하거나 전권 역할을 끄는 것을 막는 데 쓴다.
     * 아무도 제외하지 않으려면 존재하지 않는 ID(예: -1)를 넘긴다.
     */
    public long countOtherActiveSuperAdmins(Long excludedUserId) {
        return mapper.countOtherActiveSuperAdmins(excludedUserId);
    }

    /** 역할이 한 명에게라도 부여되어 있는지. 부여된 역할은 삭제할 수 없다. */
    public boolean isAssignedToAnyUser(Long roleId) {
        return mapper.countUsersByRoleId(roleId) > 0;
    }

    /** 역할만 지운다. 역할별 메뉴 권한은 서비스가 먼저 지워야 한다 (FK). */
    public void delete(Role role) {
        mapper.deleteById(role.getId());
    }
}
