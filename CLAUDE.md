# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

데이터 표준 용어사전 관리 백엔드. 표준 단어, 도메인, 용어와 테이블/컬럼 정의를 관리하고 비표준 컬럼을 검출한다. 공통 코드(코드 그룹·코드값)와 코드성 컬럼 매핑, 신청/승인, 변경 이력, 엑셀 업·다운로드, 로그인/권한도 포함한다.

**설계와 단계별 진행 상황은 `docs/design.md`에 있다** (개념, 업무 규칙, 전체 ERD, 구현 단계 표). 기능을 구현하기 전에 읽고, 단계를 마치면 상태 표와 규칙을 갱신한다.

## 기술 스택 (검증된 버전)

- Java 21, Spring Boot 4.1.1, Gradle 9.7.1 (Kotlin DSL, wrapper 포함)
- Spring Data JPA (Hibernate 7) + Querydsl `io.github.openfeign.querydsl` 7.6
- MySQL 8.4 (로컬, `compose.yaml`) / H2 (`testRuntimeOnly`, 테스트 전용)
- Flyway, springdoc-openapi 3.1.1
- Spring Security 7.1 + OAuth2 Resource Server (JWT HS256, Nimbus JOSE 10.9)
- Apache POI `poi-ooxml` 5.5.1 (엑셀 .xlsx, Boot BOM이 관리하지 않아 버전 명시)
- 프론트엔드(`frontend/`): React 19 + TypeScript 5.9 + Vite 8, Ant Design 6, TanStack Query 5, React Router 7, openapi-typescript + openapi-fetch + openapi-react-query (Node 22.16 기준으로 검증)
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
├── excel/             엑셀 다운로드/업로드 API, ExcelSheet(시트 양식), 서비스(Standard/Meta), support/(Reader·Writer·Row·Errors) (8단계)
├── code/              공통 코드: group/(코드 그룹 CodeGroup, 매핑 컬럼 조회), value/(코드값 CodeValue, 그룹 하위 리소스) (9단계)
└── menu/              화면 메뉴(Menu): TOP/LEFT 2단 메뉴 조회·관리 API, 기본 메뉴 생성(MenuInitializer) (10단계),
                       사용자별 메뉴 권한(UserMenu, MenuPermissionService) (11단계)
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
- **비표준 검출 규칙은 두 곳에 있다:** 엔티티의 `MetaColumn.inspect()`(응답의 issues)와 `MetaColumnRepositoryCustomImpl`의 Querydsl 조건(비표준 검색)이다. 프론트엔드 컬럼 폼의 미리보기(`frontend/src/features/tables/tableFields.ts`의 `inspectColumn`)도 같은 규칙을 따른다. 규칙을 바꾸면 모두 고치고 `MetaColumnRepositoryTest.searchAgreesWithInspect`로 일치를 확인한다. nullable 컬럼 비교는 SQL `<>`가 NULL에서 참이 되지 않으므로 NULL 안전 비교를 쓴다.
- **참조 삭제 차단:** 다른 엔티티가 참조하는 대상은 서비스에서 `existsBy...` 로 먼저 검사해 `XXX_IN_USE`(409)를 던진다. DB FK 위반(500)에 맡기지 않는다.
- **중복 검사 순서:** 값을 정규화(예: 영문약어 대문자화)한 뒤 검사한다. 수정 시에는 엔티티를 **변경하기 전에** 검사한다. 변경 후 exists 쿼리를 날리면 auto flush 때문에 unique 제약 위반이 먼저 발생한다.
- **권한 규칙은 `SecurityConfig`의 URL 규칙 한 곳에 있다.** 새 API를 추가하면 공개/로그인/ADMIN 중 어디에 속하는지 확인하고, 필요하면 규칙과 `SecurityApiIntegrationTest`를 함께 고친다. 메서드 보안(`@PreAuthorize`)은 쓰지 않는다.
- **인증 오류 응답:** 401/403은 보안 필터에서 나므로 `GlobalExceptionHandler`가 아니라 `SecurityErrorHandler`가 `ErrorResponse` 형식으로 쓴다. JSON 직렬화는 Boot가 등록한 Jackson 3 `tools.jackson.databind.json.JsonMapper` 빈을 주입받는다.
- **로그인한 사용자:** 컨트롤러에서 `@AuthenticationPrincipal Jwt jwt`로 받고 `jwt.getSubject()`(소문자 아이디)를 쓴다. 역할은 JWT `roles` 클레임이 `ROLE_*` 권한으로 바뀐 것이다.
- **신청 승인 반영:** 승인은 `StandardDataApplier`가 신청 내용 JSON을 원래 요청 DTO로 되돌려 기존 서비스(`WordService` 등)를 호출한다. 반영 로직을 따로 만들지 않으므로 표준 데이터 업무 규칙은 서비스 한 곳만 고치면 직접 API와 승인 양쪽에 적용된다. 신청 대상을 늘리면 `TargetType`과 `StandardDataApplier`의 switch를 함께 늘린다. 서비스에서 로그인 사용자의 역할이 필요하면 `CurrentUser.from(jwt)`로 넘긴다.
- **화면 메뉴:** 메뉴는 DB(`app_menu`)에서 관리하고 화면 구성이 서버에서 온다. 상위(TOP)-하위(LEFT) 2단 고정이며, 상위는 경로가 없고 하위만 경로를 가진다(`MenuService.validate`). 조회는 두 가지다: 화면 표시용(`GET /api/menus`, 사용 중 + 역할 필터 + 빈 상위 제외)과 관리용(`GET /api/menus/all`). 메뉴가 없으면 기동 시 `MenuInitializer`가 기본 메뉴를 만든다(이미 있으면 건드리지 않는다). 메뉴는 변경 이력을 남기지 않는다.
- **사용자별 메뉴 권한:** `app_user_menu`에 행이 있으면 그 메뉴만, 없으면 기본 규칙대로 보여준다(`MenuService.findNavigation`이 `UserMenuRepository`로 조회). 지정은 좁히기만 하므로 미사용·관리자 전용 규칙은 그대로 살아 있고, 기능 권한은 API가 따로 검사한다. 권한 저장은 통째로 교체하는데, Hibernate가 insert를 delete보다 먼저 실행하므로 삭제 후 `flush()`를 해야 unique 제약에 걸리지 않는다(`MenuPermissionService.replace`). 메뉴 삭제 시 걸린 권한을 먼저 지운다(`MenuService.delete`).
- **공통 코드 매핑:** 컬럼의 코드 그룹 매핑(`MetaColumn.codeGroup`)은 비표준 검출과 무관해 생성자·`update()`가 아니라 `mapCodeGroup()`으로 바꾼다. 서비스의 `getCodeGroup(요청 ID, 현재 그룹)`이 새로 매핑하는 그룹만 사용 여부를 검사하므로(`CODE_GROUP_NOT_USED`), 이미 매핑된 미사용 그룹은 PUT에서 그대로 유지된다. 컬럼 응답에 코드ID·코드명이 들어가므로 컬럼 목록 조회(`@EntityGraph`, 비표준 검색 fetch join)에 `codeGroup`을 함께 가져온다. 코드값 엔티티 필드는 H2·HQL 예약어 `value`를 피해 `codeValue`/`valueName`으로 둔다.
- **변경 이력:** 단어/도메인/용어/테이블/컬럼/코드 그룹/코드값을 바꾸는 서비스 메서드는 이력을 직접 남긴다. 수정은 엔티티를 바꾸기 **전에** `XxxSnapshot.from(entity)`로 떠 두고, 변경·flush 후 `historyRecorder.recordUpdate(before, XxxSnapshot.from(entity))`를 호출한다. 등록은 save 후 `recordCreate`, 삭제는 delete 전에 `recordDelete`. 변경자와 신청 ID는 기록기가 보안 컨텍스트와 `HistoryRequestContext`에서 알아서 채운다. 이력 대상을 늘리면 `HistoryTargetType`과 `history/snapshot`의 sealed record를 함께 늘린다. 스냅샷에는 수정 일시 같은 자동 값을 넣지 않는다(넣으면 모든 수정이 변경으로 보인다).
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
- **예시 데이터는 마이그레이션에 넣지 않는다.** 공통 코드 샘플(코드 그룹 19개·코드값 100개)은 `src/main/resources/db/seed/common_code_seed.sql`에 있고, Flyway 기본 경로(`db/migration`)가 아니라 자동 실행되지 않는다. 자동 실행되면 테스트용 H2에도 들어가 검색 결과를 기대하는 테스트가 깨진다. 넣으려면 파일 머리말의 명령을 쓴다(한 번만 실행, SQL 직접 삽입이라 변경 이력은 남지 않는다).

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

## 프론트엔드 (`frontend/`)

```bash
cd frontend
npm install
npm run dev            # http://localhost:5173, /api는 백엔드(기본 8080)로 프록시
npm run build          # tsc -b 타입 검사 + vite build (dist/)
npm run typecheck      # 타입 검사만
npm run api:generate   # 백엔드 API 문서 받기(api:fetch) + 타입 생성(api:types)
```

- 백엔드가 8080이 아니면 `frontend/.env.local`에 `VITE_API_PROXY_TARGET=http://localhost:8081`을 적는다. API 문서를 다른 주소에서 받으려면 `API_DOCS_URL=http://localhost:8081/v3/api-docs npm run api:fetch`.

```
frontend/src
├── api/        client.ts($api 훅·apiClient, 인증 미들웨어), schema.d.ts(생성, 직접 수정 금지), errors.ts, download.ts
├── auth/       token.ts(sessionStorage·JWT 해석), AuthContext.tsx(useAuth), RequireAuth.tsx(보호 라우트)
├── layouts/    AppLayout.tsx(TOP 메뉴·LEFT 메뉴·헤더), MenuContext.tsx(서버 메뉴 로딩·경로로 메뉴 찾기),
│               menuIcons.tsx(아이콘 이름→아이콘), HomeRedirect.tsx(첫 화면), screens.tsx(경로→화면 등록),
│               WorkspaceTabs.tsx(화면 탭 줄·열어 둔 화면), WorkspaceTabsContext.tsx(탭 상태·useTabTitle),
│               workspaceTabsStorage.ts(탭 저장·로그아웃 시 삭제)
├── features/   기능별 화면. words/(WordListPage·WordFormModal·WordDetailDrawer·wordFields)가 목록 화면의 기준 예시, domains/, terms/,
│               tables/(TableListPage·TableDetailPage(/tables/:id)·NonStandardColumnListPage(/non-standard-columns)·
│               TableFormModal·ColumnFormModal·columnCells(검출 결과 셀)·useDeleteTable·tableFields),
│               requests/(RequestListPage·RequestDetailDrawer·RequestPayloadCompare(현재 값↔신청 값)·requestFields),
│               histories/(HistoryListPage·HistoryChangeDetail(펼친 행의 변경 내용)·historyFields),
│               codes/(CodeGroupListPage·CodeGroupDetailPage(/codes/:id)·CodeGroupFormModal·CodeValueFormModal·useDeleteCodeGroup·codeFields),
│               menus/(MenuManagePage·MenuFormModal·MenuPermissionPage(사용자별 메뉴 권한)·menuFields — 관리자 전용)
├── utils/      format(일시, 이력 스냅샷 값), dataType(타입별 길이 규칙·표기·도메인명 추천. 백엔드 DataType과 같게 유지)
├── pages/      LoginPage, MyPage, PlaceholderPage(준비 중), NotFoundPage
├── components/ PageHeader, ExcelActions(엑셀 다운로드·양식·업로드), ChangeHistoryTimeline, ReasonModal,
│               WordSelect(단어 서버 검색 선택, classifierOnly), ClassifierWordSelect(WordSelect 분류어 전용),
│               DomainSelect(도메인 서버 검색 선택, 분류어 필터), TermSelect(용어 서버 검색 선택),
│               TableSelect(테이블 서버 검색 선택), CodeGroupSelect(사용 중인 코드 그룹 서버 검색 선택), DataTypeSelect
├── hooks/      useListSearchParams(검색 조건·페이지를 URL에), useInvalidateApi(조회 캐시 무효화), useDebouncedValue
├── router.tsx  라우트 구성
└── main.tsx    Provider 조립 (ConfigProvider → AntdApp → QueryClient → AuthProvider → Router)
```

- **API 호출은 생성 타입으로만 한다.** `$api.useQuery('get', '/api/words', { params: { query } })`, `$api.useMutation('post', '/api/words')`. 경로·파라미터·본문·응답 타입이 `schema.d.ts`에서 온다. 백엔드 API(DTO, 경로)를 바꾸면 백엔드를 띄운 뒤 `npm run api:generate`를 실행하고 `openapi/api-docs.json`과 `schema.d.ts` 변경을 함께 커밋한다. 타입 오류가 곧 영향받는 화면이다.
- **백엔드 operationId 규칙:** `OpenApiConfig`가 "컨트롤러 이름 + 메서드 이름"(`wordSearch`)으로 만든다. 응답 기본 media type은 `application/json`이고 엑셀 다운로드는 xlsx로 선언한다(`OpenApiIntegrationTest`).
- **오류 타입:** 오류 응답은 API 문서에 선언돼 있지 않아 생성된 에러 타입이 `never`다. `onError: (error: unknown) => ...`로 받고 `isApiError`/`errorMessage`/`applyFieldErrors`(`api/errors.ts`)로 다룬다. 폼 필드 오류는 `applyFieldErrors(form, error)`로 표시하고, 분기는 `error.code`로 한다.
- **인증:** 토큰은 sessionStorage에 두고 모든 요청에 미들웨어가 Bearer 헤더를 붙인다. 401(로그인 요청 제외)이면 로그아웃된다. 역할별 메뉴·버튼은 `useAuth().user.isAdmin`으로 나누되, 권한 검사는 서버가 한다.
- **파일 다운로드**는 `<a href>`로 하지 않고 `downloadFile('/api/excel/words', query)`(`api/download.ts`)를 쓴다. Authorization 헤더가 필요하기 때문이다.
- **메뉴(TOP/LEFT):** 메뉴는 서버에서 온다. `MenuProvider`(라우트 레이아웃)가 `/api/menus`를 불러 `useMenus()`로 나눠 주고, `AppLayout`이 위쪽에 상위 메뉴, 왼쪽에 그 하위 메뉴를 그린다. 상위 메뉴를 누르면 그 묶음의 첫 화면이 열린다. 메뉴 경로와 화면은 `layouts/screens.tsx`의 `PAGES`로 이어지며, 등록된 화면이 없는 경로는 "준비 중" 화면이 나온다. 메뉴를 바꾸면 `/api/menus`·`/api/menus/all`을 무효화해 메뉴 막대가 바로 바뀐다.
- **화면 탭(포틀릿):** 메뉴·링크로 연 화면은 탭으로 쌓인다. `router.tsx`는 로그인 확인만 하고 모든 주소를 `AppLayout` 하나가 받으며, 어떤 화면을 그릴지는 `layouts/screens.tsx`의 `ScreenRoutes`가 정한다.
  - 탭 키는 경로(pathname)라 같은 화면은 탭 하나, 상세는 대상마다 다른 탭이다. 탭은 마지막으로 본 주소(검색 조건 포함)를 기억하고, 목록은 `sessionStorage`에 남아 새로고침해도 복원된다(최대 12개).
  - 열어 둔 탭을 **모두 그려 두고** 보고 있는 탭만 `hidden`으로 보여주므로 탭을 옮겨도 검색 조건·입력값·조회 결과가 남는다. 단 화면 수만큼 메모리에 남고 쿼리도 살아 있다. 한 번도 열지 않은(복원된) 탭은 그리지 않는다.
  - 탭마다 `<Routes location={탭 주소}>`로 그려서 `useParams`·`useSearchParams`가 그 탭의 주소를 읽는다.
  - 탭 이름은 저장하지 않고 그릴 때 메뉴에서 찾는다(메뉴가 늦게 오거나 이름이 바뀌어도 맞는다). 상세 화면은 `useTabTitle(대상 이름)`으로 대상 이름만 알려주고, 탭에는 "메뉴명 · 대상 이름"으로 나온다.
- **새 화면:** `features/words`를 따라 `features/{기능}/`에 목록 페이지·폼 모달·상세 드로어·필드 정의를 만들고, `layouts/screens.tsx`의 `PAGES`에 경로와 페이지를 등록한 뒤, 메뉴 관리 화면(`/menus`)에서 그 경로로 메뉴를 만든다(메뉴가 없으면 주소로만 들어갈 수 있다). 제목 줄은 `PageHeader`를 쓴다. 하위 목록이 주 내용인 상세(테이블의 컬럼)는 드로어 대신 별도 라우트(`tables/:id`)로 만들고 `PageHeader`의 `onBack`으로 목록에 돌아간다. `AppLayout`이 경로 앞부분으로 메뉴를 선택하므로 메뉴 경로 아래에 둔다.
- **목록 화면 규칙:**
  - 검색 조건·페이지는 `useListSearchParams(FILTER_KEYS)`로 URL에 둔다. `FILTER_KEYS`는 컴포넌트 밖 상수로 선언하고, URL의 `page`는 1부터이므로 API에는 `page - 1`을 넘긴다.
  - 목록 조회에는 `placeholderData: keepPreviousData`를 준다.
  - 등록·수정·삭제 뒤에는 `useInvalidateApi()('/api/words', '/api/words/{id}', '/api/histories')`처럼 관련 경로를 무효화한다.
- **역할별 동작:** 표준 데이터(단어·도메인·용어) 화면은 같은 버튼이 ADMIN이면 직접 API, USER면 `/api/requests/{words|domains|terms}` 신청이 된다. 신청 API의 필드 오류 경로는 `payload.name`이므로 `applyFieldErrors(form, error, { stripPrefix: 'payload.' })`를 쓴다. 중복 같은 업무 오류는 `applyCodeError(form, error, 코드→필드)`로 필드 아래에 보여준다.
- **Ant Design 6 이름 변경:**
  - 확인된 새 이름: `Alert`의 `title`(`message` 대신), `Space`의 `orientation`(`direction` 대신), `Modal`의 `destroyOnHidden`, `Timeline` 항목의 `content`.
  - 모달 안 폼은 `forceRender`로 미리 렌더링해 form 인스턴스를 연결한다.
- **서버 검색 선택 박스:** Ant Design 6 `Select`는 검색 설정을 `showSearch={{ filterOption: false, onSearch }}` 객체로만 받는다(최상위 `onSearch`·`filterOption` 없음). 선택된 값이 검색 결과에 없으면(URL 복원, 수정 화면) 단건 조회로 라벨을 채운다(`ClassifierWordSelect` 참고). ID를 URL 필터로 쓸 때는 숫자로 바꾸고 잘못된 값은 조건에서 뺀다(`DomainListPage`의 `parseFilters`).
- **용어 폼:** 구성 단어는 `Form.List`(`wordIds`)로 순서를 편집하고, 용어명·영문약어는 입력받지 않고 `termFields`의 `composeTermName`/`composeTermAbbreviation`으로 미리 보여준다(백엔드 `Term.composeName`/`composeAbbreviation`과 같게 유지). 줄별 단어 정보는 `useQueries` + `$api.queryOptions('get', '/api/words/{id}')`로 가져와 `WordSelect`의 단건 조회 캐시와 공유한다. 도메인 선택은 마지막 단어가 분류어일 때만 켜고 그 분류어로 거른다. 용어명·영문약어 관련 업무 오류는 `wordIds` 목록 아래(`Form.ErrorList`)에 보여준다.
- **테이블 정의 화면:** 테이블·컬럼은 USER도 직접 반영하므로 역할별 신청 분기가 없다. 컬럼 폼은 용어를 고르면 표준 값(영문약어·용어명·도메인 타입)을 채우고(등록은 즉시, 수정은 버튼), `tableFields.inspectColumn`으로 저장 전 검출 결과를 미리 보여준다. 용어·도메인을 저장하면 검출 결과가 바뀌므로 `/api/tables/{tableId}/columns`, `/api/columns/non-standard`도 무효화한다. 컬럼 표의 물리명·타입·사유 셀은 `columnCells`를 써서 테이블 상세와 비표준 컬럼 화면이 같은 모양을 유지한다. 비표준 컬럼 화면의 사유별 건수는 사유마다 `size=1` 조회의 `totalElements`다.
- **신청/승인 화면:** 신청 응답의 `payload`는 생성 타입이 `JsonNode`로 잘못 나오므로 `requestFields.payloadOf`로 일반 객체로 꺼내 대상별 요청 DTO 타입으로 다룬다. 응답에 대상 이름이 없어 상세의 `RequestPayloadCompare`가 대기 중인 수정·삭제 신청의 현재 대상을 단건 조회해 비교하고, 참조 ID(구성 단어·도메인·분류어)는 단건 조회로 이름을 채운다. 처리가 끝난 신청은 신청 내용만 보여준다. 승인 성공 시 `TARGET_QUERY_PATHS[targetType]`로 대상 화면 조회를 무효화하고, 승인 실패(업무 오류·`REQUEST_TARGET_CHANGED`)는 신청이 대기로 남으므로 `modal.error`로 사유를 보여준다. 사유·의견 입력은 `ReasonModal`(`label`·`required`·`danger`)을 쓴다.
- **변경 이력 화면:** 이력의 `before`/`after`도 생성 타입이 `JsonNode`라 `historyFields.snapshotOf`로 꺼낸다. 대상 이름은 조회하지 않고 스냅샷에서 꺼내(삭제된 대상도 보이게) `summarizeHistoryTarget`으로 만든다. 필드 표시 이름은 기능별 `*_FIELD_LABELS`를 `HISTORY_FIELD_LABELS`로 모아 쓰므로, 스냅샷 필드를 늘리면 해당 기능의 라벨에 추가한다. 다른 화면은 `/histories?targetType=&targetId=`, `/histories?requestId=`로 연결한다. 기간 필터는 `dayjs`가 없어 `<Input type="date">`(`yyyy-MM-dd`)를 쓴다.
- **공통 코드 화면:** 공통 코드는 신청 대상이 아니라 쓰기 버튼을 ADMIN에게만 보인다(USER는 조회만). 코드값·매핑 컬럼이 주 내용이라 상세는 별도 라우트(`/codes/:id`)다. 컬럼 폼의 `CodeGroupSelect`는 `used=true`로 검색해 미사용 그룹을 새로 고르지 못하게 하고, 이미 매핑된 미사용 그룹은 단건 조회로 "미사용" 표시와 함께 보여준다. 컬럼을 저장·삭제하면 `/api/code-groups/{id}/columns`를, 코드 그룹을 수정하면 컬럼 응답의 코드ID가 바뀌므로 `/api/tables/{tableId}/columns`를 무효화한다.
- **데이터 타입 입력:** 길이·소수점 입력은 `utils/dataType`의 `usesLength`/`usesScale`로 켜고 끄고, 타입을 바꾸면 쓸 수 없는 값을 비운다. 규칙을 바꾸면 백엔드 `DataType.isValidSpec`과 함께 고친다.
- **엑셀 업로드**는 `ExcelActions`가 `apiClient.POST` + `bodySerializer`(FormData)로 보낸다. 오류 행은 표로 보여주고, 결과 `mode`가 REQUESTED면 신청 목록을 무효화한다.
- **버전 제약(Node 22.16):** orval은 Node 22.18+, react-router 8은 Node 22.22+가 필요해 쓰지 않았다. openapi-typescript가 `typescript ^5`를 요구해 TypeScript 7로 올리지 않는다. 의존성은 `--save-exact`로 고정한다.
