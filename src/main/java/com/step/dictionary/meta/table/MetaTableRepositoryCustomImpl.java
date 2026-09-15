package com.step.dictionary.meta.table;

import static com.step.dictionary.meta.table.QMetaTable.metaTable;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.step.dictionary.meta.table.dto.MetaTableSearchCondition;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

/** {@link MetaTableRepositoryCustom}의 Querydsl 구현. */
public class MetaTableRepositoryCustomImpl implements MetaTableRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MetaTableRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<MetaTable> search(MetaTableSearchCondition condition, Pageable pageable) {
        // 연관 엔티티가 없어 조인 없이 조회한다.
        List<MetaTable> content = queryFactory
                .selectFrom(metaTable)
                .where(keywordContains(condition.keyword()))
                .orderBy(metaTable.physicalName.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(metaTable.count())
                .from(metaTable)
                .where(keywordContains(condition.keyword()));

        // 필요한 경우에만 count 쿼리를 실행한다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 물리명 부분 일치(대소문자 구분 없음) OR 논리명 부분 일치.
     * 물리명은 대문자로 저장돼 있으므로 소문자 키워드로도 찾을 수 있게 대소문자를 무시한다.
     */
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        return metaTable.physicalName.containsIgnoreCase(trimmed).or(metaTable.logicalName.contains(trimmed));
    }
}
