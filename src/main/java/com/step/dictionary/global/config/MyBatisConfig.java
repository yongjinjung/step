package com.step.dictionary.global.config;

import com.step.dictionary.global.mybatis.AuditingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 공통 설정.
 *
 * <ul>
 *   <li>{@link AuditingInterceptor}: 생성·수정 일시를 insert/update 직전에 채운다. MyBatis 자동 설정이
 *       {@code Interceptor} 빈을 찾아 등록한다.</li>
 * </ul>
 * 나머지 설정(매퍼 XML 위치, 밑줄→카멜 변환, Y/N 타입 핸들러)은 {@code application.yml}의 {@code mybatis.*}에 있다.
 *
 * <p>{@code @MybatisTest}는 이 설정을 자동으로 불러오지 않으므로 리포지토리 테스트에서 {@code @Import(MyBatisConfig.class)}가 필요하다.
 * 빠뜨리면 생성 일시가 비어 {@code created_at NOT NULL} 제약으로 insert가 실패한다.
 */
@Configuration
public class MyBatisConfig {

    @Bean
    public AuditingInterceptor auditingInterceptor() {
        return new AuditingInterceptor();
    }
}
