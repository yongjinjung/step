package com.step.dictionary.history;

import static com.step.dictionary.history.QChangeHistory.changeHistory;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.step.dictionary.history.dto.ChangeHistorySearchCondition;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.QAppUser;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

/** {@link ChangeHistoryRepositoryCustom}의 Querydsl 구현. */
public class ChangeHistoryRepositoryCustomImpl implements ChangeHistoryRepositoryCustom {

    /** 변경자 조인 별칭. */
    private static final QAppUser changedBy = new QAppUser("changedBy");

    private final JPAQueryFactory queryFactory;

    public ChangeHistoryRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<ChangeHistory> search(ChangeHistorySearchCondition condition, Pageable pageable) {
        // 변경자는 없을 수 있어(시스템 작업) left join한다. 응답에 쓰므로 fetch join. 다대일이라 페이징에 영향이 없다.
        List<ChangeHistory> content = queryFactory
                .selectFrom(changeHistory)
                .leftJoin(changeHistory.changedBy, changedBy).fetchJoin()
                .where(conditions(condition))
                .orderBy(changeHistory.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 변경자 아이디 조건이 조인 별칭을 쓰므로 count 쿼리도 같은 left join을 둔다 (행 수는 같다).
        JPAQuery<Long> countQuery = queryFactory
                .select(changeHistory.count())
                .from(changeHistory)
                .leftJoin(changeHistory.changedBy, changedBy)
                .where(conditions(condition));

        // 필요한 경우에만 count 쿼리를 실행한다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /** 모든 검색 조건. null 항목은 Querydsl이 무시한다. */
    private BooleanExpression[] conditions(ChangeHistorySearchCondition condition) {
        return new BooleanExpression[] {
                condition.targetType() == null ? null : changeHistory.targetType.eq(condition.targetType()),
                condition.targetId() == null ? null : changeHistory.targetId.eq(condition.targetId()),
                condition.changeType() == null ? null : changeHistory.changeType.eq(condition.changeType()),
                condition.requestId() == null ? null : changeHistory.requestId.eq(condition.requestId()),
                changedByEq(condition.changedBy()),
                changedFrom(condition.from()),
                changedTo(condition.to())
        };
    }

    /** 변경자 아이디 일치. 아이디는 소문자로 저장돼 있으므로 같은 규칙으로 정규화해 비교한다. */
    private BooleanExpression changedByEq(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return changedBy.username.eq(AppUser.normalizeUsername(username.trim()));
    }

    /** 시작일 00:00 이후 (포함). */
    private BooleanExpression changedFrom(LocalDate from) {
        return from == null ? null : changeHistory.changedAt.goe(from.atStartOfDay());
    }

    /** 종료일 다음 날 00:00 이전. 종료일 하루 전체를 포함하기 위해 {@code <= 23:59:59} 대신 이렇게 쓴다. */
    private BooleanExpression changedTo(LocalDate to) {
        return to == null ? null : changeHistory.changedAt.lt(to.plusDays(1).atStartOfDay());
    }
}
