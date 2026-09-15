package com.step.dictionary.standard.term;

import static com.step.dictionary.standard.domain.QDomain.domain;
import static com.step.dictionary.standard.term.QTerm.term;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.step.dictionary.standard.term.dto.TermSearchCondition;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

/** {@link TermRepositoryCustom}의 Querydsl 구현. */
public class TermRepositoryCustomImpl implements TermRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public TermRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Term> search(TermSearchCondition condition, Pageable pageable) {
        // 도메인은 fetch join한다. 구성 단어는 컬렉션이라 fetch join하면 페이징이 메모리에서 일어나므로 @BatchSize로 가져온다.
        List<Term> content = queryFactory
                .selectFrom(term)
                .join(term.domain, domain).fetchJoin()
                .where(
                        keywordContains(condition.keyword()),
                        domainIdEq(condition.domainId()),
                        containsWord(condition.wordId()))
                .orderBy(term.name.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // count 쿼리는 도메인을 조인하지 않는다 (domain_id가 NOT NULL FK라 건수가 같다).
        JPAQuery<Long> countQuery = queryFactory
                .select(term.count())
                .from(term)
                .where(
                        keywordContains(condition.keyword()),
                        domainIdEq(condition.domainId()),
                        containsWord(condition.wordId()));

        // 필요한 경우에만 count 쿼리를 실행한다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /** 용어명 부분 일치 OR 영문약어 부분 일치(대소문자 구분 없음). 비어 있으면 조건 없음. */
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        return term.name.contains(trimmed).or(term.abbreviation.containsIgnoreCase(trimmed));
    }

    /** 도메인 ID 일치. FK 컬럼을 바로 비교한다. */
    private BooleanExpression domainIdEq(Long domainId) {
        return domainId == null ? null : term.domain.id.eq(domainId);
    }

    /**
     * 이 단어를 포함하는 용어.
     * {@code any()}는 조인 대신 EXISTS 서브쿼리를 만들므로, 같은 단어가 여러 번 들어간 용어도 한 번만 조회된다.
     */
    private BooleanExpression containsWord(Long wordId) {
        return wordId == null ? null : term.words.any().id.eq(wordId);
    }
}
