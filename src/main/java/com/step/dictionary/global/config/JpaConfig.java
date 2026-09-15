package com.step.dictionary.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 공통 설정.
 *
 * <ul>
 *   <li>{@link EnableJpaAuditing}: {@code BaseTimeEntity}의 {@code createdAt}/{@code updatedAt}을 자동으로 채운다.</li>
 *   <li>{@link JPAQueryFactory}: 각 {@code XxxRepositoryCustomImpl}이 Querydsl 쿼리를 만들 때 주입받는다.</li>
 * </ul>
 *
 * <p>{@code @DataJpaTest}는 이 설정을 자동으로 불러오지 않으므로 리포지토리 테스트에서 {@code @Import(JpaConfig.class)}가 필요하다.
 * 빠뜨리면 감사 필드가 비어 {@code created_at NOT NULL} 제약으로 insert가 실패한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * Querydsl 쿼리 팩토리.
     * 주입되는 {@link EntityManager}는 트랜잭션마다 실제 EntityManager로 위임하는 프록시이므로 싱글턴 빈으로 둬도 안전하다.
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
