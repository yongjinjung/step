package com.step.dictionary.code.group;

import static com.step.dictionary.code.group.QCodeGroup.codeGroup;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.step.dictionary.code.group.dto.CodeGroupSearchCondition;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link CodeGroupRepositoryCustom}의 Querydsl 구현. 구조는 {@code WordRepositoryCustomImpl}과 같다.
 * 클래스 이름은 반드시 {@code CodeGroupRepositoryCustomImpl}이어야 Spring Data가 fragment로 인식한다.
 */
public class CodeGroupRepositoryCustomImpl implements CodeGroupRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public CodeGroupRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<CodeGroup> search(CodeGroupSearchCondition condition, Pageable pageable) {
        // where에 null인 조건은 Querydsl이 자동으로 뺀다.
        List<CodeGroup> content = queryFactory
                .selectFrom(codeGroup)
                .where(keywordContains(condition.keyword()), usedEq(condition.used()))
                .orderBy(codeGroup.codeId.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(codeGroup.count())
                .from(codeGroup)
                .where(keywordContains(condition.keyword()), usedEq(condition.used()));

        // 필요한 경우(내용만으로 전체 건수를 알 수 없을 때)에만 count 쿼리를 실행한다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 키워드 부분 일치: 코드ID OR 코드명.
     * 코드ID는 대문자로 저장되므로 대소문자를 구분하지 않는다. 키워드가 비어 있으면 null을 돌려 조건에서 뺀다.
     */
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        return codeGroup.codeId.containsIgnoreCase(trimmed)
                .or(codeGroup.codeName.contains(trimmed));
    }

    /** 사용 여부 일치. null이면 전체 (조건 없음). */
    private BooleanExpression usedEq(Boolean used) {
        return used == null ? null : codeGroup.used.eq(used);
    }
}
