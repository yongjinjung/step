package com.step.dictionary.meta.column;

import static com.step.dictionary.meta.column.QMetaColumn.metaColumn;
import static com.step.dictionary.meta.table.QMetaTable.metaTable;
import static com.step.dictionary.standard.domain.QDomain.domain;
import static com.step.dictionary.standard.term.QTerm.term;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.step.dictionary.meta.column.dto.NonStandardColumnCondition;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

/**
 * {@link MetaColumnRepositoryCustom}의 Querydsl 구현.
 *
 * <p>여기의 조건은 {@link MetaColumn#inspect()}의 규칙을 SQL로 옮긴 것이다. 규칙을 바꾸면 둘 다 고치고
 * {@code MetaColumnRepositoryTest.searchAgreesWithInspect}로 결과가 일치하는지 확인한다.
 */
public class MetaColumnRepositoryCustomImpl implements MetaColumnRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MetaColumnRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<MetaColumn> searchNonStandard(NonStandardColumnCondition condition, Pageable pageable) {
        // 테이블은 필수 연관이라 inner join, 용어는 없을 수 있어(NO_TERM) left join한다.
        // 응답 변환(inspect, 테이블 물리명)에 모두 필요하므로 fetch join한다.
        // 모두 다대일이라 행 수가 늘지 않아 DB 페이징이 그대로 동작한다.
        List<MetaColumn> content = queryFactory
                .selectFrom(metaColumn)
                .join(metaColumn.table, metaTable).fetchJoin()
                .leftJoin(metaColumn.term, term).fetchJoin()
                .leftJoin(term.domain, domain).fetchJoin()
                .where(tableIdEq(condition.tableId()), hasIssue(condition.issue()))
                .orderBy(metaTable.physicalName.asc(), metaColumn.seq.asc(), metaColumn.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // count 쿼리도 사유 조건에 용어/도메인 컬럼이 쓰이므로 같은 left join이 필요하다.
        // 테이블은 조건·정렬에 쓰지 않고 table_id가 NOT NULL FK라 조인하지 않는다.
        JPAQuery<Long> countQuery = queryFactory
                .select(metaColumn.count())
                .from(metaColumn)
                .leftJoin(metaColumn.term, term)
                .leftJoin(term.domain, domain)
                .where(tableIdEq(condition.tableId()), hasIssue(condition.issue()));

        // 필요한 경우에만 count 쿼리를 실행한다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /** 테이블 ID 일치. null이면 전체 테이블. */
    private BooleanExpression tableIdEq(Long tableId) {
        return tableId == null ? null : metaColumn.table.id.eq(tableId);
    }

    /**
     * 사유를 지정하지 않으면 사유가 하나라도 있는 컬럼을 찾는다.
     * 모든 사유 조건을 OR로 묶으므로 이 메서드는 null을 돌려주지 않는다 (항상 비표준만 조회).
     */
    private BooleanExpression hasIssue(ColumnIssue issue) {
        if (issue != null) {
            return issueCondition(issue);
        }
        return Arrays.stream(ColumnIssue.values())
                .map(this::issueCondition)
                .reduce(BooleanExpression::or)
                .orElseThrow();
    }

    /**
     * 사유 하나에 해당하는 SQL 조건.
     * NO_TERM 외 사유는 용어가 있을 때만 판정한다 ({@code inspect()}에서 용어가 없으면 NO_TERM만 반환하는 것과 같다).
     * switch에 default가 없으므로 사유를 추가하면 컴파일 오류로 여기를 고쳐야 함을 알려준다.
     */
    private BooleanExpression issueCondition(ColumnIssue issue) {
        return switch (issue) {
            // 용어 FK가 비어 있다.
            case NO_TERM -> metaColumn.term.isNull();
            // 물리명과 용어 영문약어는 둘 다 NOT NULL이라 일반 비교(<>)로 충분하다.
            case NAME_MISMATCH -> metaColumn.term.isNotNull()
                    .and(metaColumn.physicalName.ne(term.abbreviation));
            // 데이터 타입도 둘 다 NOT NULL이다.
            case TYPE_MISMATCH -> metaColumn.term.isNotNull()
                    .and(metaColumn.dataType.ne(domain.dataType));
            // 길이/소수점은 NULL일 수 있으므로 NULL 안전 비교를 쓴다.
            case LENGTH_MISMATCH -> metaColumn.term.isNotNull()
                    .and(distinct(metaColumn.dataLength, domain.dataLength)
                            .or(distinct(metaColumn.dataScale, domain.dataScale)));
        };
    }

    /**
     * NULL을 값으로 취급하는 비교 (`Objects.equals`의 부정). SQL의 `<>`는 한쪽이 NULL이면 참이 되지 않는다.
     *
     * <pre>
     * left     right    결과
     * NULL     NULL     거짓 (같다)
     * NULL     10       참   (첫 번째 항)
     * 10       NULL     참   (두 번째 항)
     * 10       20       참   (세 번째 항)
     * 10       10       거짓
     * </pre>
     * JPQL(Querydsl)에는 MySQL {@code <=>} 같은 NULL 안전 비교 연산자가 없어 이렇게 풀어 쓴다.
     */
    private static BooleanExpression distinct(NumberPath<Integer> left, NumberPath<Integer> right) {
        return left.isNull().and(right.isNotNull())
                .or(left.isNotNull().and(right.isNull()))
                .or(left.ne(right));
    }
}
