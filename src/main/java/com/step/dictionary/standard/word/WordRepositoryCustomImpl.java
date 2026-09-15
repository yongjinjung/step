package com.step.dictionary.standard.word;

import static com.step.dictionary.standard.word.QWord.word;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.step.dictionary.standard.word.dto.WordSearchCondition;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link WordRepositoryCustom}의 Querydsl 구현.
 *
 * <p>클래스 이름은 반드시 {@code WordRepositoryCustomImpl}이어야 Spring Data가 fragment로 인식한다.
 * 이 클래스를 기능 패키지의 동적 검색 구현 기준 예시로 삼는다.
 */
public class WordRepositoryCustomImpl implements WordRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public WordRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Word> search(WordSearchCondition condition, Pageable pageable) {
        // 1) 내용 조회: where에 null인 조건은 Querydsl이 자동으로 빼므로 조건 메서드를 그대로 나열한다.
        List<Word> content = queryFactory
                .selectFrom(word)
                .where(keywordContains(condition.keyword()), classifierEq(condition.classifier()))
                .orderBy(word.name.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 건수 쿼리: 실행하지 않고 정의만 해 둔다.
        JPAQuery<Long> countQuery = queryFactory
                .select(word.count())
                .from(word)
                .where(keywordContains(condition.keyword()), classifierEq(condition.classifier()));

        // 3) 첫 페이지인데 내용이 페이지 크기보다 적거나, 마지막 페이지라서 전체 건수를 계산할 수 있으면
        //    count 쿼리를 생략한다. 필요할 때만 countQuery::fetchOne이 호출된다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 키워드 부분 일치: 단어명 OR 영문약어 OR 영문명.
     * 영문 필드는 대소문자를 구분하지 않는다. 키워드가 비어 있으면 null을 돌려 조건에서 뺀다.
     */
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        return word.name.contains(trimmed)
                .or(word.abbreviation.containsIgnoreCase(trimmed))
                .or(word.englishName.containsIgnoreCase(trimmed));
    }

    /** 분류어 여부 일치. null이면 전체 (조건 없음). */
    private BooleanExpression classifierEq(Boolean classifier) {
        return classifier == null ? null : word.classifier.eq(classifier);
    }
}
