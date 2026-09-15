package com.step.dictionary.standard.domain;

import static com.step.dictionary.standard.domain.QDomain.domain;
import static com.step.dictionary.standard.word.QWord.word;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.step.dictionary.standard.domain.dto.DomainSearchCondition;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

/** {@link DomainRepositoryCustom}의 Querydsl 구현. */
public class DomainRepositoryCustomImpl implements DomainRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public DomainRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Domain> search(DomainSearchCondition condition, Pageable pageable) {
        // 응답에 분류어 단어명이 들어가므로 fetch join으로 N+1을 막는다.
        // 다대일(to-one) 관계라 fetch join해도 행 수가 늘지 않아 DB 페이징이 그대로 동작한다.
        List<Domain> content = queryFactory
                .selectFrom(domain)
                .join(domain.classifierWord, word).fetchJoin()
                .where(
                        keywordContains(condition.keyword()),
                        classifierWordIdEq(condition.classifierWordId()),
                        dataTypeEq(condition.dataType()))
                .orderBy(domain.name.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // count 쿼리는 단어 컬럼이 필요 없으므로 조인하지 않는다.
        // classifier_word_id가 NOT NULL FK라 조인 여부와 관계없이 건수가 같다.
        JPAQuery<Long> countQuery = queryFactory
                .select(domain.count())
                .from(domain)
                .where(
                        keywordContains(condition.keyword()),
                        classifierWordIdEq(condition.classifierWordId()),
                        dataTypeEq(condition.dataType()));

        // 필요한 경우에만 count 쿼리를 실행한다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /** 도메인명 부분 일치 (대소문자 구분 없음). 비어 있으면 조건 없음. */
    private BooleanExpression keywordContains(String keyword) {
        return StringUtils.hasText(keyword) ? domain.name.containsIgnoreCase(keyword.trim()) : null;
    }

    /** 분류어 단어 ID 일치. FK 컬럼을 바로 비교하므로 단어 테이블 조인이 필요 없다. */
    private BooleanExpression classifierWordIdEq(Long classifierWordId) {
        return classifierWordId == null ? null : domain.classifierWord.id.eq(classifierWordId);
    }

    /** 데이터 타입 일치. null이면 조건 없음. */
    private BooleanExpression dataTypeEq(DataType dataType) {
        return dataType == null ? null : domain.dataType.eq(dataType);
    }
}
