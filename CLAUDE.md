# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

데이터 표준 용어사전 관리 백엔드. 표준 단어, 도메인, 용어와 테이블/컬럼 정의를 관리하고 비표준 컬럼을 검출한다. 신청/승인, 변경 이력, 엑셀 업·다운로드, 로그인/권한도 포함한다.

**설계와 단계별 진행 상황은 `docs/design.md`에 있다** (개념, 업무 규칙, 전체 ERD, 구현 단계 표). 기능을 구현하기 전에 읽고, 단계를 마치면 상태 표와 규칙을 갱신한다.

## 기술 스택 (검증된 버전)

- Java 21, Spring Boot 4.1.1, Gradle 9.7.1 (Kotlin DSL, wrapper 포함)
- Spring Data JPA (Hibernate 7) + Querydsl `io.github.openfeign.querydsl` 7.6
- MySQL 8.4 (로컬, `compose.yaml`) / H2 (`testRuntimeOnly`, 테스트 전용)
- Flyway, springdoc-openapi 3.1.1
- Spring Security 7.1 + OAuth2 Resource Server (JWT HS256, Nimbus JOSE 10.9)
- Apache POI `poi-ooxml` 5.5.1 (엑셀 .xlsx, Boot BOM이 관리하지 않아 버전 명시)
- JUnit 5 + Mockito + AssertJ, REST Assured 6.0.1, JaCoCo

## 명령어

PowerShell에서는 `.\gradlew.bat`, Git Bash에서는 `./gradlew`를 쓴다.

```bash
./gradlew build                                   # 컴파일 + 전체 테스트 + 커버리지
./gradlew test                                    # 전체 테스트 (끝나면 jacocoTestReport 자동 실행)
./gradlew test --tests "com.step.dictionary.standard.word.WordServiceTest"          # 단일 클래스
./gradlew test --tests "com.step.dictionary.standard.word.WordServiceTest.create"   # 단일 메서드
./gradlew test --tests "*IntegrationTest"         # 통합 테스트만
./gradlew compileJava                             # Querydsl Q클래스 재생성 (build/generated)

docker compose up -d                              # 로컬 MySQL 실행
./gradlew bootRun                                 # 기본 프로파일 local (MySQL 필요)
```

- 커버리지 리포트: `build/reports/jacoco/test/html/index.html`. Q클래스, `*Application`, `global/config`는 측정에서 제외한다.
- Swagger UI: `http://localhost:8080/swagger-ui/index.html` (공개). API 호출은 `POST /api/auth/login`으로 받은 토큰을 Authorize에 넣는다.

## 코드 구조

```
com.step.dictionary
├── global/            공통: config(JPA 감사·JPAQueryFactory, OpenAPI, SecurityConfig), entity(BaseTimeEntity),
│                      error(ErrorCode, BusinessException, GlobalExceptionHandler), response(PageResponse),
│                      security(SecurityErrorHandler: 401/403 JSON 응답)
├── standard/word/     표준 단어 (1단계, 기능 패키지의 기준 예시)
├── standard/domain/   표준 도메인 (2단계, 분류어 단어 참조)
├── standard/term/     표준 용어 (3단계, 단어 조합 + 도메인 참조)
├── meta/              PhysicalNames(물리명 규칙), table/(테이블 정의), column/(컬럼 정의 + 비표준 검출) (4단계)
├── user/              사용자 계정(AppUser, Role), 관리자 전용 계정 관리 API, 최초 관리자 생성 (5단계)
├── auth/              로그인·내 정보·비밀번호 변경 API, JWT 발급(JwtTokenProvider), JwtProperties, CurrentUser (5단계)
├── request/           표준 데이터 변경 신청/승인(ChangeRequest), 대상별 조회·반영(StandardDataApplier) (6단계)
├── history/           변경 이력(ChangeHistory) 기록(ChangeHistoryRecorder)·조회 API, snapshot/(대상별 스냅샷 record) (7단계)
└── excel/             엑셀 다운로드/업로드 API, ExcelSheet(시트 양식), 서비스(Standard/Meta), support/(Reader·Writer·Row·Errors) (8단계)
```

- **기능 단위 패키지:** 한 기능 패키지 안에 Entity, `XxxRepository`, `XxxRepositoryCustom`/`XxxRepositoryCustomImpl`(Querydsl), `XxxService`, `XxxController`, `dto/`(record)를 둔다. 새 기능은 `standard/word`를 따라 만든다.
- **오류 처리:** 서비스는 `BusinessException(ErrorCode)`를 던진다. `GlobalExceptionHandler`가 `{code, message, fieldErrors}` 형식으로 응답한다. 새 오류는 `ErrorCode` enum에 HTTP 상태와 함께 추가한다.
- **목록 조회:** Spring `Page`를 그대로 반환하지 않고 `PageResponse`로 변환한다.
- 생성자 주입을 쓰고 Lombok은 쓰지 않는다. 요청/응답 DTO는 record로 만든다.

## 구현 규칙

- **Querydsl:** 커스텀 구현체 이름은 반드시 `{Repository}CustomImpl`이다(Spring Data fragment 규칙). 동적 조건은 `null`을 반환하는 `BooleanExpression` 메서드로 조합하고, count 쿼리는 `PageableExecutionUtils.getPage`로 처리한다.
- **여부(Y/N) 컬럼:** DB는 `CHAR(1)`, 엔티티는 `boolean` + `@Convert(converter = YesNoConverter.class)`로 매핑한다. MySQL `BOOLEAN(TINYINT)`는 `ddl-auto=validate`에서 타입 불일치가 날 수 있다.
- **enum 컬럼:** DB는 `VARCHAR`, 엔티티는 `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.VARCHAR)`로 매핑한다. MySQL 방언은 STRING enum을 `ENUM` 타입으로 간주해 validate가 어긋날 수 있다.
- **순서 있는 연관 목록:** 조인 테이블은 `@ManyToMany` + `@OrderColumn(name = "seq")` + `@ListIndexBase(1)`로 매핑하고(`Term.words`), 목록 조회용으로 `@BatchSize`를 붙인다. 컬렉션은 페이징 쿼리에서 fetch join하지 않는다(메모리 페이징). 변경은 `clear()` 후 `addAll()`로 한다.
- **비표준 검출 규칙은 두 곳에 있다:** 엔티티의 `MetaColumn.inspect()`(응답의 issues)와 `MetaColumnRepositoryCustomImpl`의 Querydsl 조건(비표준 검색)이다. 규칙을 바꾸면 둘 다 고치고 `MetaColumnRepositoryTest.searchAgreesWithInspect`로 일치를 확인한다. nullable 컬럼 비교는 SQL `<>`가 NULL에서 참이 되지 않으므로 NULL 안전 비교를 쓴다.
- **참조 삭제 차단:** 다른 엔티티가 참조하는 대상은 서비스에서 `existsBy...` 로 먼저 검사해 `XXX_IN_USE`(409)를 던진다. DB FK 위반(500)에 맡기지 않는다.
- **중복 검사 순서:** 값을 정규화(예: 영문약어 대문자화)한 뒤 검사한다. 수정 시에는 엔티티를 **변경하기 전에** 검사한다. 변경 후 exists 쿼리를 날리면 auto flush 때문에 unique 제약 위반이 먼저 발생한다.
- **권한 규칙은 `SecurityConfig`의 URL 규칙 한 곳에 있다.** 새 API를 추가하면 공개/로그인/ADMIN 중 어디에 속하는지 확인하고, 필요하면 규칙과 `SecurityApiIntegrationTest`를 함께 고친다. 메서드 보안(`@PreAuthorize`)은 쓰지 않는다.
- **인증 오류 응답:** 401/403은 보안 필터에서 나므로 `GlobalExceptionHandler`가 아니라 `SecurityErrorHandler`가 `ErrorResponse` 형식으로 쓴다. JSON 직렬화는 Boot가 등록한 Jackson 3 `tools.jackson.databind.json.JsonMapper` 빈을 주입받는다.
- **로그인한 사용자:** 컨트롤러에서 `@AuthenticationPrincipal Jwt jwt`로 받고 `jwt.getSubject()`(소문자 아이디)를 쓴다. 역할은 JWT `roles` 클레임이 `ROLE_*` 권한으로 바뀐 것이다.
- **신청 승인 반영:** 승인은 `StandardDataApplier`가 신청 내용 JSON을 원래 요청 DTO로 되돌려 기존 서비스(`WordService` 등)를 호출한다. 반영 로직을 따로 만들지 않으므로 표준 데이터 업무 규칙은 서비스 한 곳만 고치면 직접 API와 승인 양쪽에 적용된다. 신청 대상을 늘리면 `TargetType`과 `StandardDataApplier`의 switch를 함께 늘린다. 서비스에서 로그인 사용자의 역할이 필요하면 `CurrentUser.from(jwt)`로 넘긴다.
- **변경 이력:** 단어/도메인/용어/테이블/컬럼을 바꾸는 서비스 메서드는 이력을 직접 남긴다. 수정은 엔티티를 바꾸기 **전에** `XxxSnapshot.from(entity)`로 떠 두고, 변경·flush 후 `historyRecorder.recordUpdate(before, XxxSnapshot.from(entity))`를 호출한다. 등록은 save 후 `recordCreate`, 삭제는 delete 전에 `recordDelete`. 변경자와 신청 ID는 기록기가 보안 컨텍스트와 `HistoryRequestContext`에서 알아서 채운다. 이력 대상을 늘리면 `HistoryTargetType`과 `history/snapshot`의 sealed record를 함께 늘린다. 스냅샷에는 수정 일시 같은 자동 값을 넣지 않는다(넣으면 모든 수정이 변경으로 보인다).
- **벌크 JPQL 삭제 주의:** 이력을 위해 엔티티를 불러온 뒤 같은 트랜잭션에서 JPQL 벌크 delete를 쓰면, 불러온 엔티티가 영속성 컨텍스트에 남아 연관 대상 삭제 시 `TransientPropertyValueException`이 난다. 이력을 남기는 삭제는 `deleteAll(entities)`처럼 엔티티 단위로 한다(`MetaTableService.delete`).
- **엑셀 업로드:** 행마다 `ExcelRow`로 셀을 변환해 API 요청 DTO를 만들고, `row.validate(validator, dto, 필드→열 번호)`로 같은 Bean Validation을 적용한 뒤 기존 서비스로 반영한다. 반영 로직을 따로 만들지 않는다. 변환·검증·업무 규칙 오류는 모두 `ExcelErrors`에 모으고 마지막에 `throwIfAny()`로 던져 전체를 롤백한다. 행 루프에서 `BusinessException`을 잡고 계속하면 트랜잭션이 롤백 전용으로 표시되므로, 오류가 있으면 **반드시 예외를 던져** 커밋을 시도하지 않게 한다. 열 순서를 바꾸면 `ExcelSheet` 헤더와 서비스의 열 번호 상수를 함께 고친다.
- **비밀번호:** `PasswordEncoder` 빈(DelegatingPasswordEncoder, BCrypt)으로만 인코딩/비교한다. 입력 규칙은 `PasswordPolicy`(8~64자 ASCII, BCrypt 72바이트 한도)를 쓴다. 응답 DTO에 비밀번호 필드를 넣지 않는다.

## 설정 / 프로파일

- `application.yml`의 기본 프로파일은 `local`(MySQL)이다. **스프링 컨텍스트를 띄우는 모든 테스트는 `@ActiveProfiles("test")`가 필요하다.** 없으면 MySQL 접속을 시도하다 실패한다.
- `test` 프로파일은 H2 in-memory(`MODE=MySQL;DATABASE_TO_LOWER=TRUE`)를 쓰고, 테스트에서도 Flyway 마이그레이션이 실행된다. 마이그레이션 SQL은 MySQL과 H2 양쪽에서 동작해야 한다. `ENGINE=`, 테이블/컬럼 `COMMENT` 같은 MySQL 전용 구문은 쓰지 않는다.
- 스키마는 Flyway만 관리한다(`ddl-auto: validate`, `open-in-view: false`).
- `app.jwt.secret`(32바이트 이상)이 없으면 기동에 실패한다. 기본 `application.yml`은 환경변수 `JWT_SECRET`을 쓰고, `local`/`test` 프로파일 yml에 개발용 키가 있다. `@ConfigurationProperties` record는 `@ConfigurationPropertiesScan`으로 등록된다.
- 최초 관리자: 사용자 테이블이 비어 있으면 기동 시 `app.initial-admin.*`로 ADMIN을 만든다. `test` 프로파일은 비밀번호를 두지 않아 만들지 않는다.
- **`application-local.yml`은 git에 올리지 않는다**(`.gitignore`). DB 비밀번호, JWT 키, 초기 관리자 비밀번호가 들어가기 때문이다. 새로 받은 저장소에서는 `src/main/resources/application-local.yml.example`을 같은 폴더에 `application-local.yml`로 복사해 값을 채운 뒤 `bootRun`한다. 로컬 설정 항목을 추가하면 예시 파일에도 반영한다.

## Flyway

- `src/main/resources/db/migration/V{번호}__{설명}.sql`
- 이미 적용된 파일은 수정하지 않고 새 버전 파일을 추가한다.
- 긴 문자열 컬럼에 `TEXT`를 쓰지 않는다. MySQL과 H2가 서로 다른 JDBC 타입으로 보고해 `ddl-auto=validate`가 어긋날 수 있다. `VARCHAR(n)`을 쓴다(`std_request.payload`는 4000).
- 엔티티를 바꾸면 같은 작업 안에서 마이그레이션도 만든다.

## 테스트

| 종류 | 방식 | 예시 |
|---|---|---|
| 단위 | 스프링 없이 도메인 테스트, 또는 `@ExtendWith(MockitoExtension.class)`로 서비스 테스트 | `WordTest`, `WordServiceTest` |
| 리포지토리 | `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import(JpaConfig.class)` + `@ActiveProfiles("test")` | `WordRepositoryTest` |
| 통합 | `support.IntegrationTest` 상속 (RANDOM_PORT + REST Assured) | `WordApiIntegrationTest` |

- `@DataJpaTest`에서 `replace = NONE`을 빼면 MySQL 모드가 아닌 임베디드 H2로 바뀐다. `JpaConfig`를 import하지 않으면 감사 필드(`created_at` NOT NULL)가 비어 insert가 실패한다.
- 통합 테스트 요청은 서버 스레드에서 처리되어 롤백되지 않는다. `IntegrationTest`가 매 테스트 전후에 `DatabaseCleaner`로 모든 테이블을 TRUNCATE한다. H2 메모리 DB(`DB_CLOSE_DELAY=-1`)는 모든 테스트 컨텍스트가 공유하므로, 뒤이어 실행되는 `@DataJpaTest`가 남은 데이터와 충돌하지 않게 끝날 때도 비운다.
- **통합 테스트 인증:** `IntegrationTest`가 매 테스트 전에 `admin`(ADMIN), `user`(USER) 계정(비밀번호 `PASSWORD`)을 만들고 토큰(`adminToken`, `userToken`)을 발급해, 기본으로 모든 요청에 관리자 토큰을 붙인다. 다른 권한은 `authenticate(userToken)`, 토큰 없음은 `authenticate(null)`로 바꾼다. `RequestSpecBuilder`는 생성 시 현재 `RestAssured.requestSpecification`을 복사하므로 새 스펙을 만들기 전에 반드시 null로 비운다(`authenticate` 참고).
- 엑셀 업로드 통합 테스트는 `ExcelWriter.write(ExcelSheet.X, rows)`로 파일을 만들고 `given().multiPart("file", "upload.xlsx", bytes, XLSX_MIME)`으로 올린다. 행에 null을 넣으려면 `List.of` 대신 `Arrays.asList`를 쓴다.
- 이력을 남기는 서비스의 단위 테스트는 `@Mock ChangeHistoryRecorder`를 선언해야 한다. 빠뜨리면 `@InjectMocks`가 생성자에 null을 넣어 NPE가 난다.
- REST Assured는 본문 없는 POST에 `application/x-www-form-urlencoded` Content-Type을 붙인다. `@RequestBody(required = false)` 엔드포인트(예: 신청 승인)는 이때 415를 주므로 `contentType(ContentType.JSON)`을 명시한다.
- `Jwt.getIssuer()`는 `iss`를 URL로 변환하려 해서 `dictionary` 같은 문자열 발급자에는 예외가 난다. 테스트에서는 `getClaimAsString("iss")`를 쓴다.
- REST Assured 요청 본문은 text block JSON 문자열로 작성하고, 응답은 `body("path", matcher)`나 `extract().jsonPath()`로 검증한다.
- 테스트 이름은 `@DisplayName`에 한글로 적는다.

## Spring Boot 4 주의사항

Boot 3 기준 자료와 다른 부분이다.

- 스타터가 모듈화되어 있다. `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`(+ `flyway-mysql`)를 쓰고, 테스트는 기능별 `spring-boot-starter-*-test`를 쓴다.
- 테스트 어노테이션 패키지가 바뀌었다: `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`, `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`
- Jackson 3를 쓴다. 기본 패키지가 `tools.jackson`이고, 어노테이션만 `com.fasterxml.jackson.annotation` 그대로다.
- REST Assured는 Boot BOM이 버전을 관리하지 않으므로 `build.gradle.kts`에 버전을 명시한다(6.x는 Groovy 5, Jackson 3 호환).
- 원본 `com.querydsl` 아티팩트는 쓰지 않는다. OpenFeign 포크의 패키지명은 `com.querydsl.*` 그대로다.
