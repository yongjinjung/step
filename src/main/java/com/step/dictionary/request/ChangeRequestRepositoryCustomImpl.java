package com.step.dictionary.request;

import static com.step.dictionary.request.QChangeRequest.changeRequest;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.step.dictionary.request.dto.ChangeRequestSearchCondition;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.QAppUser;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

/** {@link ChangeRequestRepositoryCustom}의 Querydsl 구현. */
public class ChangeRequestRepositoryCustomImpl implements ChangeRequestRepositoryCustom {

    // 같은 AppUser 엔티티를 두 번 조인하므로 별칭을 따로 둔다. 기본 QAppUser.appUser 하나로는 구분할 수 없다.
    private static final QAppUser requester = new QAppUser("requester");
    private static final QAppUser reviewer = new QAppUser("reviewer");

    private final JPAQueryFactory queryFactory;

    public ChangeRequestRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<ChangeRequest> search(ChangeRequestSearchCondition condition, Pageable pageable) {
        // 신청자는 필수라 inner join, 검토자는 없을 수 있어 left join. 응답에 둘 다 쓰므로 fetch join한다.
        // 모두 다대일이라 행 수가 늘지 않아 DB 페이징이 그대로 동작한다.
        List<ChangeRequest> content = queryFactory
                .selectFrom(changeRequest)
                .join(changeRequest.requester, requester).fetchJoin()
                .leftJoin(changeRequest.reviewer, reviewer).fetchJoin()
                .where(
                        statusEq(condition),
                        targetTypeEq(condition),
                        requestTypeEq(condition),
                        requesterEq(condition.requester()))
                .orderBy(changeRequest.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 신청자 아이디 조건이 신청자 조인을 쓰므로 count 쿼리도 조인한다 (NOT NULL FK라 건수는 같다).
        JPAQuery<Long> countQuery = queryFactory
                .select(changeRequest.count())
                .from(changeRequest)
                .join(changeRequest.requester, requester)
                .where(
                        statusEq(condition),
                        targetTypeEq(condition),
                        requestTypeEq(condition),
                        requesterEq(condition.requester()));

        // 필요한 경우에만 count 쿼리를 실행한다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /** 상태 일치. null이면 전체. */
    private BooleanExpression statusEq(ChangeRequestSearchCondition condition) {
        return condition.status() == null ? null : changeRequest.status.eq(condition.status());
    }

    /** 대상 종류 일치. null이면 전체. */
    private BooleanExpression targetTypeEq(ChangeRequestSearchCondition condition) {
        return condition.targetType() == null ? null : changeRequest.targetType.eq(condition.targetType());
    }

    /** 신청 종류 일치. null이면 전체. */
    private BooleanExpression requestTypeEq(ChangeRequestSearchCondition condition) {
        return condition.requestType() == null ? null : changeRequest.requestType.eq(condition.requestType());
    }

    /** 신청자 아이디 일치. 아이디는 소문자로 저장돼 있으므로 같은 규칙으로 정규화해 비교한다. */
    private BooleanExpression requesterEq(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return requester.username.eq(AppUser.normalizeUsername(username.trim()));
    }
}
