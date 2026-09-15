package com.step.dictionary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 데이터 표준 용어사전 관리 백엔드의 시작점.
 *
 * <p>표준 단어, 도메인, 용어와 테이블/컬럼 정의를 관리하고 비표준 컬럼을 검출한다.
 * 이 클래스가 있는 {@code com.step.dictionary} 패키지 아래를 컴포넌트 스캔한다.
 * {@link ConfigurationPropertiesScan}은 같은 범위의 {@code @ConfigurationProperties} record
 * ({@code JwtProperties}, {@code InitialAdminProperties})를 찾아 빈으로 등록한다.
 *
 * <p>기본 프로파일은 {@code local}(MySQL)이다. 실행 전에 {@code docker compose up -d}로 DB를 띄워야 한다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DictionaryApplication {

    /** 스프링 부트 애플리케이션을 실행한다. */
    public static void main(String[] args) {
        SpringApplication.run(DictionaryApplication.class, args);
    }
}
