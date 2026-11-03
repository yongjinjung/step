package com.step.dictionary.user;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.role.Role;
import com.step.dictionary.user.dto.UserSearchCondition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 사용자 계정 리포지토리 ({@link AppUserMapper} 사용).
 *
 * <p>사용자와 역할(조인 테이블 {@code app_user_role})을 한 묶음으로 저장·조회한다. 역할은 IN 쿼리 한 번으로 모아 붙인다.
 * 엔티티를 바꾼 뒤(이름·역할·활성·비밀번호)에는 {@link #save}를 호출한다. 계정은 삭제하지 않는다.
 */
@Repository
public class AppUserRepository {

    private final AppUserMapper mapper;

    public AppUserRepository(AppUserMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 등록, 있으면 수정한다. 역할 연결은 지우고 다시 넣는다. */
    public AppUser save(AppUser user) {
        if (user.getId() == null) {
            mapper.insert(user);
        } else {
            mapper.update(user);
            mapper.deleteUserRoles(user.getId());
        }
        user.getRoles().forEach(role -> mapper.insertUserRole(user.getId(), role.getId()));
        return user;
    }

    public Optional<AppUser> findById(Long id) {
        return mapper.findById(id).map(this::withRoles);
    }

    /** 로그인 아이디로 찾는다. 소문자로 정규화한 값을 넘긴다. */
    public Optional<AppUser> findByUsername(String username) {
        return mapper.findByUsername(username).map(this::withRoles);
    }

    public long count() {
        return mapper.count();
    }

    /** 등록 시 아이디 중복 확인. 소문자로 정규화한 값을 넘긴다. */
    public boolean existsByUsername(String username) {
        return mapper.countByUsername(username) > 0;
    }

    /** 사용 중인 전권 역할을 가졌는지. 권한 계산에서 모든 검사를 통과시키는 데 쓴다. */
    public boolean hasSuperAdminRole(String username) {
        return mapper.countSuperAdminRoles(username) > 0;
    }

    /** 조건에 맞는 사용자를 아이디 오름차순으로 페이지 조회한다 (역할 포함). */
    public Page<AppUser> search(UserSearchCondition condition, Pageable pageable) {
        List<AppUser> users = mapper.search(condition, pageable);
        if (!users.isEmpty()) {
            Map<Long, List<Role>> rolesByUser = mapper.findRolesByUserIds(users.stream().map(AppUser::getId).toList())
                    .stream()
                    .collect(groupingBy(AppUserMapper.UserRoleRow::getUserId,
                            mapping(AppUserMapper.UserRoleRow::getRole, toList())));
            users.forEach(user -> user.loadRoles(rolesByUser.getOrDefault(user.getId(), List.of())));
        }
        return Pages.of(users, pageable, () -> mapper.countSearch(condition));
    }

    private AppUser withRoles(AppUser user) {
        user.loadRoles(mapper.findRolesByUserIds(List.of(user.getId())).stream()
                .map(AppUserMapper.UserRoleRow::getRole)
                .toList());
        return user;
    }
}
