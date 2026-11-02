package com.step.dictionary.user;

import static com.step.dictionary.user.QAppUser.appUser;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.step.dictionary.user.dto.UserSearchCondition;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

/** {@link AppUserRepositoryCustom}의 Querydsl 구현. */
public class AppUserRepositoryCustomImpl implements AppUserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public AppUserRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<AppUser> search(UserSearchCondition condition, Pageable pageable) {
        // 역할은 다대다라 조인하면 행이 늘어난다. 조건은 exists 서브쿼리로 걸어 페이징이 어긋나지 않게 한다.
        List<AppUser> content = queryFactory
                .selectFrom(appUser)
                .where(
                        keywordContains(condition.keyword()),
                        hasRole(condition.roleId()),
                        enabledEq(condition.enabled()))
                .orderBy(appUser.username.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(appUser.count())
                .from(appUser)
                .where(
                        keywordContains(condition.keyword()),
                        hasRole(condition.roleId()),
                        enabledEq(condition.enabled()));

        // 필요한 경우에만 count 쿼리를 실행한다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /** 아이디 부분 일치(대소문자 구분 없음) OR 이름 부분 일치. 비어 있으면 조건 없음. */
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        return appUser.username.containsIgnoreCase(trimmed).or(appUser.name.contains(trimmed));
    }

    /** 이 역할을 가진 사용자. null이면 전체. */
    private BooleanExpression hasRole(Long roleId) {
        return roleId == null ? null : appUser.roles.any().id.eq(roleId);
    }

    /** 활성 여부 일치. null이면 전체. */
    private BooleanExpression enabledEq(Boolean enabled) {
        return enabled == null ? null : appUser.enabled.eq(enabled);
    }
}
