plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.step"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val mybatisVersion = "4.1.0" // mybatis-spring-boot-starter (Spring Boot 4.1 기준)
val p6spyStarterVersion = "2.0.1" // Boot BOM이 관리하지 않음 (Spring Boot 4 기준 빌드)
val sqlFormatterVersion = "2.0.5"
val springdocVersion = "3.1.1"
val restAssuredVersion = "6.0.1" // Boot 4 BOM이 관리하지 않음
val poiVersion = "5.5.1" // Boot 4 BOM이 관리하지 않음

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // 데이터 접근: MyBatis (SQL은 src/main/resources/mapper/**/*.xml). JDBC 스타터와 트랜잭션 관리자를 함께 가져온다.
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:$mybatisVersion")
    // Pageable/Page와 웹 요청 파라미터(page, size, sort) 변환. JPA 없이 Spring Data 공통 모듈만 쓴다.
    implementation("org.springframework.boot:spring-boot-data-commons")
    // SQL 로그: 실제 실행 SQL에 파라미터 값을 채워 정렬해 보여준다. local 프로파일에서만 켠다 (decorator.datasource.enabled).
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:$p6spyStarterVersion")
    implementation("com.github.vertical-blank:sql-formatter:$sqlFormatterVersion")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // 인증/권한: Spring Security + JWT(Bearer) 검증. Nimbus JOSE가 포함되어 별도 JWT 라이브러리가 필요 없다.
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    // Flyway 10+는 DB별 모듈이 따로 있다 (Oracle)
    implementation("org.flywaydb:flyway-database-oracle")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    // 엑셀(.xlsx) 업로드/다운로드 (8단계)
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    runtimeOnly("com.oracle.database.jdbc:ojdbc11")
    testRuntimeOnly("com.h2database:h2")

    testImplementation("org.mybatis.spring.boot:mybatis-spring-boot-starter-test:$mybatisVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val jacocoExcludes = listOf(
    "**/*Application.class",
    "**/global/config/**",
)

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required = true
        xml.required = true
    }
    classDirectories.setFrom(
        sourceSets.main.get().output.classesDirs.asFileTree.matching { exclude(jacocoExcludes) }
    )
}
