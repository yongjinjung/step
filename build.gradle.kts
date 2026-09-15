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

val querydslVersion = "7.6"
val springdocVersion = "3.1.1"
val restAssuredVersion = "6.0.1" // Boot 4 BOM이 관리하지 않음
val poiVersion = "5.5.1" // Boot 4 BOM이 관리하지 않음

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // 인증/권한: Spring Security + JWT(Bearer) 검증. Nimbus JOSE가 포함되어 별도 JWT 라이브러리가 필요 없다.
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-mysql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    // 엑셀(.xlsx) 업로드/다운로드 (8단계)
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    // Querydsl: 유지보수 중인 OpenFeign 포크 (Hibernate 7 / Jakarta 지원)
    implementation("io.github.openfeign.querydsl:querydsl-jpa:$querydslVersion")
    annotationProcessor("io.github.openfeign.querydsl:querydsl-apt:$querydslVersion:jpa")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    runtimeOnly("com.mysql:mysql-connector-j")
    testRuntimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val jacocoExcludes = listOf(
    "**/Q*.class",
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
