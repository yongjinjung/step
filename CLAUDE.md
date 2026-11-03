# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

데이터 표준 용어사전 관리 백엔드. 표준 단어, 도메인, 용어와 테이블/컬럼 정의를 관리하고 비표준 컬럼을 검출한다. 공통 코드(코드 그룹·코드값)와 코드성 컬럼 매핑, 신청/승인, 변경 이력, 엑셀 업·다운로드, 로그인과 역할 기반 권한(메뉴별 조회·등록·수정·삭제)도 포함한다. 병원 약제 업무(처방·조제·검수·불출·반납)의 마스터/품목 데이터와 API도 있다(13단계, 화면은 아직 없음).

**설계와 단계별 진행 상황은 `docs/design.md`에 있다** (개념, 업무 규칙, 전체 ERD, 구현 단계 표). 기능을 구현하기 전에 읽고, 단계를 마치면 상태 표와 규칙을 갱신한다.

## 언어

이 프로젝트의 작업 언어는 한국어다. 답변, 설명, 커밋 메시지, 문서(`docs/`, 이 파일), 코드 주석, 테스트 `@DisplayName`, 화면 문구와 오류 메시지는 한국어로 쓴다. 클래스·메서드·변수 이름 같은 식별자는 영어로 쓴다.

## 기술 스택 (검증된 버전)

- Java 21, Spring Boot 4.1.1, Gradle 9.7.1 (Kotlin DSL, wrapper 포함)
- MyBatis 3.5 (`mybatis-spring-boot-starter` 4.1.0, Spring Boot 4.1 기준) + Spring Data Commons(`Pageable`/`Page`만, `spring-boot-data-commons`)
- MySQL 8.4 (로컬, `compose.yaml`) / H2 (`testRuntimeOnly`, 테스트 전용)
- Flyway, springdoc-openapi 3.1.1
- SQL 로그: P6Spy (`p6spy-spring-boot-starter` 2.0.1, Spring Boot 4 기준) + `sql-formatter` 2.0.5 (local 프로파일에서만 켬)
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

docker compose up -d                              # 로컬 MySQL 실행
./gradlew bootRun                                 # 기본 프로파일 local (MySQL 필요)
```

- 커버리지 리포트: `build/reports/jacoco/test/html/index.html`. `*Application`, `global/config`는 측정에서 제외한다.
- Swagger UI: `http://localhost:8080/swagger-ui/index.html` (공개). API 호출은 `POST /api/auth/login`으로 받은 토큰을 Authorize에 넣는다.

## 코드 구조

```
com.step.dictionary
├── global/            공통: config(MyBatisConfig, OpenAPI, SecurityConfig), entity(BaseTimeEntity, CreatedAtAware),
│                      mybatis(AuditingInterceptor 생성·수정 일시, YesNoTypeHandler, Likes, SqlParams, Pages),
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
├── menu/              화면 메뉴(Menu): TOP/LEFT 2단 메뉴 조회·관리 API, 기본 메뉴 생성(MenuInitializer) (10단계),
│                      사용자별 메뉴 지정(UserMenu, MenuPermissionService) (11단계)
├── role/              역할(Role)과 역할별 메뉴 권한(RoleMenu), 권한 계산(PermissionService, UserPermissions),
│                      메뉴 경로 상수(MenuPaths), 기본 권한 생성(RoleInitializer) (12단계)
└── pharmacy/          약제 업무 (13단계): prescription/(처방), dispensing/(조제), inspection/(검수), issue/(불출),
                       drugreturn/(반납, `return`이 예약어라 DrugReturn). 업무마다 마스터 + 품목 엔티티.
                       공통: PharmacyItems(품목 검사), Quantities(수량 소수 3자리). 매퍼 XML의 공통 조각(처방 컬럼·검색어·기간)은
                       PrescriptionMapper.xml에 있다
```

- **기능 단위 패키지:** 한 기능 패키지 안에 Entity(일반 도메인 클래스), `XxxMapper`(MyBatis 매퍼 인터페이스, `@Mapper`), `XxxRepository`(`@Repository` 클래스: 매퍼로 애그리거트를 저장·조회), `XxxService`, `XxxController`, `dto/`(record)를 둔다. SQL은 `src/main/resources/mapper/{기능 경로}/XxxMapper.xml`에 둔다. 새 기능은 `standard/word`를 따라 만든다.
- **오류 처리:** 서비스는 `BusinessException(ErrorCode)`를 던진다. `GlobalExceptionHandler`가 `{code, message, fieldErrors}` 형식으로 응답한다. 새 오류는 `ErrorCode` enum에 HTTP 상태와 함께 추가한다.
- **목록 조회:** Spring `Page`를 그대로 반환하지 않고 `PageResponse`로 변환한다.
- 생성자 주입을 쓰고 Lombok은 쓰지 않는다. 요청/응답 DTO는 record로 만든다.

## 구현 규칙

- **MyBatis 데이터 접근 (JPA에서 전환):**
  - 서비스는 매퍼를 직접 쓰지 않고 `XxxRepository`를 쓴다. 리포지토리 메서드 이름은 `save`/`findById`/`existsBy...`/`search`/`delete`로 통일한다. `save()`는 ID가 없으면 insert(생성된 ID가 `useGeneratedKeys`로 채워진다), 있으면 update다.
  - **변경 감지가 없다.** 엔티티를 바꾸는 메서드(`update()`, `cancel()`, 상태 전이 등)를 부른 뒤 반드시 `repository.save(entity)`를 호출한다. 다른 서비스의 엔티티 상태를 바꿀 때는 그 서비스의 메서드를 거친다(`PrescriptionService.markDispensed`, `DispensingService.applyInspection`/`markIssued`).
  - **보안: 매퍼 XML에서 `${}`(문자열 치환)를 쓰지 않는다.** 값은 모두 `#{}` 바인딩으로 넘긴다. `<include>`의 property 치환도 `${}`라 쓰지 않는다. 재사용할 열 조각이 필요하면 테이블 별칭과 결과 열 이름을 고정한다(약제 매퍼: 처방 `p`/`p_id`, 조제 `d`/`d_id` …, `PrescriptionMapper.xml` 머리말). `MapperXmlRuleTest`가 검사한다.
  - 연관(다대일)은 SQL에서 조인해 `<association columnPrefix>`로 채운다. 접두사는 중첩될수록 이어 붙는다(용어 → 도메인 `d_` → 분류어 `d_cw_`). 다른 매퍼의 `resultMap`·`<sql>` 조각은 네임스페이스 전체 이름으로 참조한다. 3단계 이상 중첩되는 약제 매퍼는 접두사를 쌓지 않고 결과 매핑에 고정 열 이름을 직접 적는다.
  - 일대다·다대다(용어의 구성 단어, 사용자의 역할, 약제 품목)는 조인하면 행이 늘어 LIMIT 페이징이 깨지므로 따로 읽어 리포지토리가 붙인다(`Term.loadWords`, `AppUser.loadRoles`, `loadItems`). 목록은 IN 쿼리 한 번으로 모은다. 저장은 리포지토리가 함께 한다(조인 테이블은 지우고 다시 넣는다).
  - 동적 검색은 `<where>` + `<if>`로 쓰고 `search`(`LIMIT #{p.pageSize} OFFSET #{p.offset}`)와 `count`가 같은 `<sql id="searchWhere">`를 쓴다. 리포지토리는 `Pages.of(content, pageable, count)`로 묶는다(필요할 때만 count 실행).
  - 부분 일치는 `<bind value="@...Likes@contains(c.keyword)">` + `LIKE #{keyword} ESCAPE '!'`로 쓴다(`%`, `_` 이스케이프). 대소문자 무시는 `LOWER(col) LIKE LOWER(#{keyword})`. 아이디·코드 정규화와 기간(`from`/`to` → 0시/다음 날 0시)은 `SqlParams`를 쓴다.
  - 생성·수정 일시는 `AuditingInterceptor`가 insert/update 직전에 채운다(`BaseTimeEntity`, 수정 일시가 없는 테이블은 `CreatedAtAware`). insert/update SQL에 `#{createdAt}`, `#{updatedAt}`을 넣어야 한다.
  - 존재 여부는 `COUNT(*)`를 `long`으로 받아 비교한다(전역 boolean 타입 핸들러가 Y/N을 기대하므로 SQL 결과를 boolean으로 받지 않는다). 연관이 null일 수 있는 값은 `#{term.id}`처럼 써도 null로 들어간다.
  - 세션 1차 캐시는 끄고(`local-cache-scope: statement`) 조회마다 새 객체가 나온다. 객체 동일성(`==`)이 아니라 ID로 비교한다.
- **여부(Y/N) 컬럼:** DB는 `CHAR(1)`, 엔티티는 `boolean`이다. `YesNoTypeHandler`가 모든 boolean 파라미터·결과를 Y/N으로 바꾼다(`mybatis.type-handlers-package`). 컬럼명이 필드명과 다르면(`used_yn` → `used`) 결과 매핑에 `<result>`로 적는다.
- **enum 컬럼:** DB는 `VARCHAR`이고 MyBatis 기본 `EnumTypeHandler`가 이름(`name()`)으로 저장·조회한다.
- **순서 있는 연관 목록:** 조인 테이블(`std_term_word`)에 `seq`(1부터)로 순서를 저장한다. `TermRepository.save()`가 행을 지우고 목록 순서대로 다시 넣고, 조회는 `ORDER BY term_id, seq`로 읽어 붙인다.
- **비표준 검출 규칙은 두 곳에 있다:** 엔티티의 `MetaColumn.inspect()`(응답의 issues)와 `MetaColumnMapper.xml`의 `issueCondition` SQL 조각(비표준 검색)이다. 프론트엔드 컬럼 폼의 미리보기(`frontend/src/features/tables/tableFields.ts`의 `inspectColumn`)도 같은 규칙을 따른다. 규칙을 바꾸면 모두 고치고 `MetaColumnRepositoryTest.searchAgreesWithInspect`로 일치를 확인한다. nullable 컬럼 비교는 SQL `<>`가 NULL에서 참이 되지 않으므로 NULL 안전 비교를 쓴다.
- **참조 삭제 차단:** 다른 엔티티가 참조하는 대상은 서비스에서 `existsBy...` 로 먼저 검사해 `XXX_IN_USE`(409)를 던진다. DB FK 위반(500)에 맡기지 않는다.
- **중복 검사 순서:** 값을 정규화(예: 영문약어 대문자화)한 뒤 검사한다. 수정 시에는 엔티티를 **변경하기 전에** 검사해, 중복이면 저장하지 않고 409로 끝낸다.
- **권한 검사(12단계):** 기능 권한은 DB의 역할별 메뉴 권한이다. `SecurityConfig`는 공개(로그인·문서)와 로그인만 필요한 경로(`/api/auth/**`, `GET /api/menus`)만 정하고, 나머지는 `MenuAuthorizationManager`가 검사한다. 요청 경로를 메뉴+동작으로 옮기는 표는 `global/security/ApiPermissionRules`에 있고 **규칙이 없는 경로는 막힌다**. 새 API를 추가하면 이 표에 규칙을 더하고 `SecurityApiIntegrationTest`·`RoleApiIntegrationTest`로 확인한다. 메서드 보안(`@PreAuthorize`)은 쓰지 않는다.
- **역할:** 사용자는 역할을 여러 개 가지고 권한은 합집합이다(`PermissionService.of(username)` → `UserPermissions`). 전권 역할(`superAdmin`)은 모든 검사를 통과한다(권한 설정 실수로 잠기는 것을 막는 장치). 기본 제공 역할(ADMIN, USER)은 삭제·코드·전권 변경이 막히고, 부여된 역할은 삭제할 수 없다. 서비스에서 "관리자인가" 대신 권한을 묻는다(예: 신청 검토는 `can(MenuPaths.REQUESTS, UPDATE)`, 엑셀 직접 등록은 `can(대상 메뉴, CREATE)`). 권한은 요청마다 DB에서 계산하므로 역할 변경이 바로 반영된다(토큰의 `roles`는 표시용).
- **메뉴 경로는 권한의 이름이기도 하다** (`MenuPaths`). 이미 쓰는 메뉴의 화면 경로를 바꾸면 그 메뉴에 걸린 권한이 API 규칙과 이어지지 않아 전권 역할만 쓸 수 있게 된다.
- **인증 오류 응답:** 401/403은 보안 필터에서 나므로 `GlobalExceptionHandler`가 아니라 `SecurityErrorHandler`가 `ErrorResponse` 형식으로 쓴다. JSON 직렬화는 Boot가 등록한 Jackson 3 `tools.jackson.databind.json.JsonMapper` 빈을 주입받는다.
- **로그인한 사용자:** 컨트롤러에서 `@AuthenticationPrincipal Jwt jwt`로 받고 `jwt.getSubject()`(소문자 아이디)를 쓴다. 역할은 JWT `roles` 클레임이 `ROLE_*` 권한으로 바뀐 것이다.
- **신청 승인 반영:** 승인은 `StandardDataApplier`가 신청 내용 JSON을 원래 요청 DTO로 되돌려 기존 서비스(`WordService` 등)를 호출한다. 반영 로직을 따로 만들지 않으므로 표준 데이터 업무 규칙은 서비스 한 곳만 고치면 직접 API와 승인 양쪽에 적용된다. 신청 대상을 늘리면 `TargetType`과 `StandardDataApplier`의 switch를 함께 늘린다. 서비스에서 로그인 사용자의 역할이 필요하면 `CurrentUser.from(jwt)`로 넘긴다.
- **화면 메뉴:** 메뉴는 DB(`app_menu`)에서 관리하고 화면 구성이 서버에서 온다. 상위(TOP)-하위(LEFT) 2단 고정이며, 상위는 경로가 없고 하위만 경로를 가진다(`MenuService.validate`). 조회는 두 가지다: 화면 표시용(`GET /api/menus`, 사용 중 + 조회 권한 + 빈 상위 제외)과 관리용(`GET /api/menus/all`). 두 응답 모두 메뉴마다 그 사용자의 권한(`canRead` 등)을 담아 화면이 버튼을 정할 수 있다. 메뉴가 없으면 기동 시 `MenuInitializer`가 기본 메뉴를 만든다(이미 있으면 건드리지 않는다). 기본 메뉴에는 초급 개발자 온보딩용 "샘플" 묶음(`features/samples`로 이어짐)도 포함되고, `RoleInitializer`의 USER 기본 권한에도 조회로 열려 있다. 메뉴는 변경 이력을 남기지 않는다.
- **사용자별 메뉴 권한:** `app_user_menu`에 행이 있으면 그 메뉴만, 없으면 기본 규칙대로 보여준다(`MenuService.findNavigation`이 `UserMenuRepository`로 조회). 지정은 좁히기만 하므로 미사용·관리자 전용 규칙은 그대로 살아 있고, 기능 권한은 API가 따로 검사한다. 권한 저장은 통째로 교체한다. 사용자의 행을 먼저 지우고 다시 넣으므로 같은 (사용자, 메뉴) 조합도 unique 제약에 걸리지 않는다(`MenuPermissionService.replace`). 메뉴 삭제 시 걸린 권한을 먼저 지운다(`MenuService.delete`).
- **공통 코드 매핑:** 컬럼의 코드 그룹 매핑(`MetaColumn.codeGroup`)은 비표준 검출과 무관해 생성자·`update()`가 아니라 `mapCodeGroup()`으로 바꾼다. 서비스의 `getCodeGroup(요청 ID, 현재 그룹)`이 새로 매핑하는 그룹만 사용 여부를 검사하므로(`CODE_GROUP_NOT_USED`), 이미 매핑된 미사용 그룹은 PUT에서 그대로 유지된다. 컬럼 응답에 코드ID·코드명이 들어가므로 컬럼 조회 SQL(`MetaColumnMapper.xml`)은 모두 코드 그룹을 조인해 함께 가져온다. 코드값 엔티티 필드는 H2·HQL 예약어 `value`를 피해 `codeValue`/`valueName`으로 둔다.
- **변경 이력:** 단어/도메인/용어/테이블/컬럼/코드 그룹/코드값을 바꾸는 서비스 메서드는 이력을 직접 남긴다. 수정은 엔티티를 바꾸기 **전에** `XxxSnapshot.from(entity)`로 떠 두고, 변경·`save()` 후 `historyRecorder.recordUpdate(before, XxxSnapshot.from(entity))`를 호출한다. 등록은 save 후 `recordCreate`, 삭제는 delete 전에 `recordDelete`. 변경자와 신청 ID는 기록기가 보안 컨텍스트와 `HistoryRequestContext`에서 알아서 채운다. 이력 대상을 늘리면 `HistoryTargetType`과 `history/snapshot`의 sealed record를 함께 늘린다. 스냅샷에는 수정 일시 같은 자동 값을 넣지 않는다(넣으면 모든 수정이 변경으로 보인다).
- **삭제 순서:** DB가 cascade하지 않으므로 하위 행(컬럼, 코드값, 조인 테이블, 권한 행)을 먼저 지우고 상위를 지운다. 이력을 남기는 삭제는 대상을 불러와 이력을 남긴 뒤 한 건씩 지운다(`MetaTableService.delete`).
- **엑셀 업로드:** 행마다 `ExcelRow`로 셀을 변환해 API 요청 DTO를 만들고, `row.validate(validator, dto, 필드→열 번호)`로 같은 Bean Validation을 적용한 뒤 기존 서비스로 반영한다. 반영 로직을 따로 만들지 않는다. 변환·검증·업무 규칙 오류는 모두 `ExcelErrors`에 모으고 마지막에 `throwIfAny()`로 던져 전체를 롤백한다. 행 루프에서 `BusinessException`을 잡고 계속하면 트랜잭션이 롤백 전용으로 표시되므로, 오류가 있으면 **반드시 예외를 던져** 커밋을 시도하지 않게 한다. 열 순서를 바꾸면 `ExcelSheet` 헤더와 서비스의 열 번호 상수를 함께 고친다.
- **약제 업무(13단계):** 처방 → 조제 → 검수 → 불출 → 반납이 상태로 이어진다(처방 `ORDERED→DISPENSED/CANCELLED`, 조제 `DISPENSED→INSPECTED/REJECTED→ISSUED`). 상태 전이와 "할 수 있는가"는 엔티티 메서드(`isCancellable`, `isInspectable`, `isIssuable` 등)에 두고 서비스가 확인 후 오류를 던진다. 하위 품목은 `addItem`으로만 만들고, 리포지토리 `save()`가 마스터를 넣은 뒤 품목을 함께 넣는다. 등록 후 바뀌는 것은 상태뿐이라 수정 SQL은 상태만 바꾼다(`updateStatus`). 조제·검수 품목은 앞 단계 품목을 빠짐없이 한 번씩(`PharmacyItems.requireExactly`), 반납은 일부만(`requireSubset`) 다룬다. 등록자는 `jwt.getSubject()`, 일시는 서버 시각이다. 수량은 `Quantities.normalize`로 소수 3자리로 맞춰 저장 직후 응답과 조회 응답의 모양을 같게 한다. 변경 이력은 남기지 않는다(업무 기록이 곧 이력). 세부 규칙은 `docs/design.md` 13단계.
- **비밀번호:** `PasswordEncoder` 빈(DelegatingPasswordEncoder, BCrypt)으로만 인코딩/비교한다. 입력 규칙은 `PasswordPolicy`(8~64자 ASCII, BCrypt 72바이트 한도)를 쓴다. 응답 DTO에 비밀번호 필드를 넣지 않는다.

## 설정 / 프로파일

- `application.yml`의 기본 프로파일은 `local`(MySQL)이다. **스프링 컨텍스트를 띄우는 모든 테스트는 `@ActiveProfiles("test")`가 필요하다.** 없으면 MySQL 접속을 시도하다 실패한다.
- `test` 프로파일은 H2 in-memory(`MODE=MySQL;DATABASE_TO_LOWER=TRUE`)를 쓰고, 테스트에서도 Flyway 마이그레이션이 실행된다. 마이그레이션 SQL은 MySQL과 H2 양쪽에서 동작해야 한다. `ENGINE=`, 테이블/컬럼 `COMMENT` 같은 MySQL 전용 구문은 쓰지 않는다.
- 스키마는 Flyway만 관리한다. MyBatis는 스키마를 검사하지 않으므로, 컬럼을 바꾸면 매퍼 XML의 SQL도 함께 고치고 테스트로 확인한다.
- **SQL 로그:** `decorator.datasource.enabled`(기본 `false`, local만 `true`)로 켠다. P6Spy가 실제 실행 SQL에 파라미터 값을 채우고 `global/logging/P6SpySqlFormatter`가 줄바꿈·들여쓰기해 `p6spy` 로거(INFO)로 남긴다. 형식 지정은 `src/main/resources/spy.properties`(`logMessageFormat`)에 있다. 값이 그대로 찍히므로(비밀번호 해시 포함) 운영에서는 켜지 않는다.
- `app.jwt.secret`(32바이트 이상)이 없으면 기동에 실패한다. 기본 `application.yml`은 환경변수 `JWT_SECRET`을 쓰고, `local`/`test` 프로파일 yml에 개발용 키가 있다. `@ConfigurationProperties` record는 `@ConfigurationPropertiesScan`으로 등록된다.
- 최초 관리자: 사용자 테이블이 비어 있으면 기동 시 `app.initial-admin.*`로 ADMIN을 만든다. `test` 프로파일은 비밀번호를 두지 않아 만들지 않는다.
- **`application-local.yml`은 git에 올리지 않는다**(`.gitignore`). DB 비밀번호, JWT 키, 초기 관리자 비밀번호가 들어가기 때문이다. 새로 받은 저장소에서는 `src/main/resources/application-local.yml.example`을 같은 폴더에 `application-local.yml`로 복사해 값을 채운 뒤 `bootRun`한다. 로컬 설정 항목을 추가하면 예시 파일에도 반영한다.

## Flyway

- `src/main/resources/db/migration/V{번호}__{설명}.sql`
- 이미 적용된 파일은 수정하지 않고 새 버전 파일을 추가한다.
- 긴 문자열 컬럼에 `TEXT`를 쓰지 않는다(MySQL과 H2의 타입 차이를 피한다). `VARCHAR(n)`을 쓴다(`std_request.payload`는 4000).
- 엔티티를 바꾸면 같은 작업 안에서 마이그레이션과 매퍼 XML도 고친다.
- **예시 데이터는 마이그레이션에 넣지 않는다.** 공통 코드 샘플(코드 그룹 19개·코드값 100개)은 `src/main/resources/db/seed/common_code_seed.sql`, 표준 단어·도메인·용어 샘플(단어 105개·도메인 17개·용어 1000개)은 `standard_term_seed.sql`, 병원 약제(처방·조제·검수·불출·반납) 단어 3000개는 `pharmacy_word_seed.sql`(용어 샘플 다음에 실행), 약제 용어 5000개와 약제 분류어 도메인 10개는 `pharmacy_term_seed.sql`(약제 단어 다음에 실행)에 있고, Flyway 기본 경로(`db/migration`)가 아니라 자동 실행되지 않는다. 자동 실행되면 테스트용 H2에도 들어가 검색 결과를 기대하는 테스트가 깨진다. 넣으려면 파일 머리말의 명령을 쓴다(한 번만 실행, SQL 직접 삽입이라 변경 이력은 남지 않는다).

## 테스트

| 종류 | 방식 | 예시 |
|---|---|---|
| 단위 | 스프링 없이 도메인 테스트, 또는 `@ExtendWith(MockitoExtension.class)`로 서비스 테스트 | `WordTest`, `WordServiceTest` |
| 리포지토리 | `@MybatisTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import({MyBatisConfig.class, 쓰는 리포지토리.class})` + `@ActiveProfiles("test")` | `WordRepositoryTest` |
| 통합 | `support.IntegrationTest` 상속 (RANDOM_PORT + REST Assured) | `WordApiIntegrationTest` |

- `@MybatisTest`는 매퍼와 데이터소스·Flyway·트랜잭션(테스트마다 롤백)만 띄운다. `@Repository` 클래스는 스캔하지 않으므로 `@Import`에 적는다. `replace = NONE`을 빼면 MySQL 모드가 아닌 임베디드 H2로 바뀐다. `MyBatisConfig`를 import하지 않으면 생성 일시(`created_at` NOT NULL)가 비어 insert가 실패한다. SQL로 직접 확인할 때는 `JdbcTemplate`을 주입받는다.
- 통합 테스트 요청은 서버 스레드에서 처리되어 롤백되지 않는다. `IntegrationTest`가 매 테스트 전후에 `DatabaseCleaner`로 모든 테이블을 TRUNCATE한다. H2 메모리 DB(`DB_CLOSE_DELAY=-1`)는 모든 테스트 컨텍스트가 공유하므로, 뒤이어 실행되는 `@MybatisTest`가 남은 데이터와 충돌하지 않게 끝날 때도 비운다.
- **통합 테스트 인증·권한:** 테이블을 모두 비우면 역할·메뉴·권한도 사라지므로, `IntegrationTest`가 매 테스트 전에 기본 역할(ADMIN 전권, USER)을 만들고 운영과 같은 초기화 코드(`MenuInitializer`, `RoleInitializer`)로 기본 메뉴와 일반 사용자 권한을 채운다("설치 직후" 상태). 그 뒤 `admin`(ADMIN 역할), `user`(USER 역할) 계정(비밀번호 `PASSWORD`)을 만들고 토큰(`adminToken`, `userToken`)을 발급해, 기본으로 모든 요청에 관리자 토큰을 붙인다. 역할 ID가 필요하면 `adminRole`·`userRole`을 쓴다. 단위 테스트는 `support.TestRoles`로 역할 객체를 만든다. 다른 권한은 `authenticate(userToken)`, 토큰 없음은 `authenticate(null)`로 바꾼다. `RequestSpecBuilder`는 생성 시 현재 `RestAssured.requestSpecification`을 복사하므로 새 스펙을 만들기 전에 반드시 null로 비운다(`authenticate` 참고).
- 엑셀 업로드 통합 테스트는 `ExcelWriter.write(ExcelSheet.X, rows)`로 파일을 만들고 `given().multiPart("file", "upload.xlsx", bytes, XLSX_MIME)`으로 올린다. 행에 null을 넣으려면 `List.of` 대신 `Arrays.asList`를 쓴다.
- 이력을 남기는 서비스의 단위 테스트는 `@Mock ChangeHistoryRecorder`를 선언해야 한다. 빠뜨리면 `@InjectMocks`가 생성자에 null을 넣어 NPE가 난다.
- REST Assured는 본문 없는 POST에 `application/x-www-form-urlencoded` Content-Type을 붙인다. `@RequestBody(required = false)` 엔드포인트(예: 신청 승인)는 이때 415를 주므로 `contentType(ContentType.JSON)`을 명시한다.
- `Jwt.getIssuer()`는 `iss`를 URL로 변환하려 해서 `dictionary` 같은 문자열 발급자에는 예외가 난다. 테스트에서는 `getClaimAsString("iss")`를 쓴다.
- REST Assured 요청 본문은 text block JSON 문자열로 작성하고, 응답은 `body("path", matcher)`나 `extract().jsonPath()`로 검증한다.
- 테스트 이름은 `@DisplayName`에 한글로 적는다.

## Spring Boot 4 주의사항

Boot 3 기준 자료와 다른 부분이다.

- 스타터가 모듈화되어 있다. `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`(+ `flyway-mysql`)를 쓰고, 테스트는 기능별 `spring-boot-starter-*-test`를 쓴다.
- 테스트 어노테이션 패키지가 바뀌었다: `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase` (`spring-boot-starter-jdbc-test`). MyBatis 슬라이스는 `org.mybatis.spring.boot.test.autoconfigure.MybatisTest`.
- Jackson 3를 쓴다. 기본 패키지가 `tools.jackson`이고, 어노테이션만 `com.fasterxml.jackson.annotation` 그대로다.
- REST Assured는 Boot BOM이 버전을 관리하지 않으므로 `build.gradle.kts`에 버전을 명시한다(6.x는 Groovy 5, Jackson 3 호환).
- JPA 스타터 없이 `Pageable`/`Page`와 웹 파라미터(page, size, sort) 변환을 쓰려면 `spring-boot-data-commons`가 필요하다(API 문서의 페이징 파라미터도 여기서 나온다).

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
│               menus/(MenuManagePage·MenuFormModal·MenuPermissionPage(사용자별 메뉴 권한)·menuFields — 관리자 전용),
│               users/(UserListPage·UserFormModal(역할 다중 선택)·PasswordResetModal·userFields),
│               roles/(RoleListPage(역할 목록 + 메뉴 권한 매트릭스)·RoleFormModal·roleFields),
│               samples/(개발 참고용 샘플 화면 — 초급 개발자 온보딩 자료: ReactSamplePage(/react)·TypeScriptSamplePage(/typescript)·
│               ViteSamplePage(/vite)·AntdSamplePage(/antdesign)·RouterSamplePage(/router, RouterParamDemoPage는
│               router-demo/:id 전용 useParams 데모)·DataFetchingSamplePage(/data-fetching, 조회 데모는 실제 백엔드를 호출)·
│               FormsSamplePage(/forms)·BackendGuideSamplePage(/backend-guide, Spring Boot 계층 구조·오류 처리·
│               MyBatis 매퍼·테스트, standard/word 기준 예시. 조회·오류 데모는 실제 백엔드를 호출)·
│               SampleSection(설명→실행 결과→코드)·SampleCode(복사 가능한 코드)·
│               dynamicSample(동적 import 데모 전용, 정적 import 금지))
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
- **권한으로 화면 제어:** `useMenus().can('/words', 'create')`처럼 메뉴 권한을 물어 버튼을 보여줄지 정한다(권한 값은 `/api/menus` 응답에서 온다). 화면 자체는 `layouts/screens.tsx`의 `RequireMenu`가 그 메뉴의 조회 권한으로 막는다. `useAuth().user.isAdmin`은 상단 역할 표시에만 쓰고 기능 분기에는 쓰지 않는다(권한 검사는 서버가 한다).
- **역할별 동작:** 표준 데이터(단어·도메인·용어) 화면은 같은 버튼이 권한이 있으면 직접 API, 없으면 `/api/requests/{words|domains|terms}` 신청이 된다. 신청 API의 필드 오류 경로는 `payload.name`이므로 `applyFieldErrors(form, error, { stripPrefix: 'payload.' })`를 쓴다. 중복 같은 업무 오류는 `applyCodeError(form, error, 코드→필드)`로 필드 아래에 보여준다.
- **Ant Design 6 이름 변경:**
  - 확인된 새 이름: `Alert`의 `title`(`message` 대신), `Space`의 `orientation`(`direction` 대신), `Modal`의 `destroyOnHidden`, `Timeline` 항목의 `content`.
  - 모달 안 폼은 `forceRender`로 미리 렌더링해 form 인스턴스를 연결한다.
- **서버 검색 선택 박스:** Ant Design 6 `Select`는 검색 설정을 `showSearch={{ filterOption: false, onSearch }}` 객체로만 받는다(최상위 `onSearch`·`filterOption` 없음). 선택된 값이 검색 결과에 없으면(URL 복원, 수정 화면) 단건 조회로 라벨을 채운다(`ClassifierWordSelect` 참고). ID를 URL 필터로 쓸 때는 숫자로 바꾸고 잘못된 값은 조건에서 뺀다(`DomainListPage`의 `parseFilters`).
- **용어 폼:** 구성 단어는 `Form.List`(`wordIds`)로 순서를 편집하고, 용어명·영문약어는 입력받지 않고 `termFields`의 `composeTermName`/`composeTermAbbreviation`으로 미리 보여준다(백엔드 `Term.composeName`/`composeAbbreviation`과 같게 유지). 줄별 단어 정보는 `useQueries` + `$api.queryOptions('get', '/api/words/{id}')`로 가져와 `WordSelect`의 단건 조회 캐시와 공유한다. 도메인 선택은 마지막 단어가 분류어일 때만 켜고 그 분류어로 거른다. 용어명·영문약어 관련 업무 오류는 `wordIds` 목록 아래(`Form.ErrorList`)에 보여준다.
- **테이블 정의 화면:** 테이블·컬럼은 USER도 직접 반영하므로 역할별 신청 분기가 없다. 컬럼 폼은 용어를 고르면 표준 값(영문약어·용어명·도메인 타입)을 채우고(등록은 즉시, 수정은 버튼), `tableFields.inspectColumn`으로 저장 전 검출 결과를 미리 보여준다. 용어·도메인을 저장하면 검출 결과가 바뀌므로 `/api/tables/{tableId}/columns`, `/api/columns/non-standard`도 무효화한다. 컬럼 표의 물리명·타입·사유 셀은 `columnCells`를 써서 테이블 상세와 비표준 컬럼 화면이 같은 모양을 유지한다. 비표준 컬럼 화면의 사유별 건수는 사유마다 `size=1` 조회의 `totalElements`다.
- **신청/승인 화면:** 신청 응답의 `payload`는 생성 타입이 `JsonNode`로 잘못 나오므로 `requestFields.payloadOf`로 일반 객체로 꺼내 대상별 요청 DTO 타입으로 다룬다. 응답에 대상 이름이 없어 상세의 `RequestPayloadCompare`가 대기 중인 수정·삭제 신청의 현재 대상을 단건 조회해 비교하고, 참조 ID(구성 단어·도메인·분류어)는 단건 조회로 이름을 채운다. 처리가 끝난 신청은 신청 내용만 보여준다. 승인 성공 시 `TARGET_QUERY_PATHS[targetType]`로 대상 화면 조회를 무효화하고, 승인 실패(업무 오류·`REQUEST_TARGET_CHANGED`)는 신청이 대기로 남으므로 `modal.error`로 사유를 보여준다. 사유·의견 입력은 `ReasonModal`(`label`·`required`·`danger`)을 쓴다.
- **변경 이력 화면:** 이력의 `before`/`after`도 생성 타입이 `JsonNode`라 `historyFields.snapshotOf`로 꺼낸다. 대상 이름은 조회하지 않고 스냅샷에서 꺼내(삭제된 대상도 보이게) `summarizeHistoryTarget`으로 만든다. 필드 표시 이름은 기능별 `*_FIELD_LABELS`를 `HISTORY_FIELD_LABELS`로 모아 쓰므로, 스냅샷 필드를 늘리면 해당 기능의 라벨에 추가한다. 다른 화면은 `/histories?targetType=&targetId=`, `/histories?requestId=`로 연결한다. 기간 필터는 `dayjs`가 없어 `<Input type="date">`(`yyyy-MM-dd`)를 쓴다.
- **공통 코드 화면:** 공통 코드는 신청 대상이 아니라 쓰기 버튼을 ADMIN에게만 보인다(USER는 조회만). 코드값·매핑 컬럼이 주 내용이라 상세는 별도 라우트(`/codes/:id`)다. 컬럼 폼의 `CodeGroupSelect`는 `used=true`로 검색해 미사용 그룹을 새로 고르지 못하게 하고, 이미 매핑된 미사용 그룹은 단건 조회로 "미사용" 표시와 함께 보여준다. 컬럼을 저장·삭제하면 `/api/code-groups/{id}/columns`를, 코드 그룹을 수정하면 컬럼 응답의 코드ID가 바뀌므로 `/api/tables/{tableId}/columns`를 무효화한다.
- **사용자 관리 화면:** 계정은 삭제하지 않으므로 목록에 삭제 버튼이 없다(비활성으로 둔다). 아이디는 등록할 때만 입력하고 수정에서는 잠근다. 비밀번호는 초기화 모달로만 바꾸며, 규칙(8~64자 공백 없는 ASCII)은 `userFields.PASSWORD_RULES`에 두고 내 정보 화면과 같은 규칙을 쓴다. `LAST_ADMIN_REQUIRED`는 강등인지 비활성화인지에 따라 역할·활성 필드 중 맞는 자리에 보여준다. 목록의 "메뉴 권한"은 `/menu-permissions?userId=`로 열고, 그 화면은 목록에 없는 사용자면 단건 조회로 이름을 채운다.
- **데이터 타입 입력:** 길이·소수점 입력은 `utils/dataType`의 `usesLength`/`usesScale`로 켜고 끄고, 타입을 바꾸면 쓸 수 없는 값을 비운다. 규칙을 바꾸면 백엔드 `DataType.isValidSpec`과 함께 고친다.
- **엑셀 업로드**는 `ExcelActions`가 `apiClient.POST` + `bodySerializer`(FormData)로 보낸다. 오류 행은 표로 보여주고, 결과 `mode`가 REQUESTED면 신청 목록을 무효화한다.
- **샘플 화면(`features/samples`):** 초급 개발자 온보딩용 참고 화면이다(메뉴 "샘플" 아래 `/react`, `/typescript`, `/vite`, `/antdesign`, `/router`, `/data-fetching`, `/forms`, `/backend-guide`). 섹션은 `SampleSection`으로 "설명 → 실행 결과(동작하는 데모) → 복사 가능한 코드" 순서를 지킨다. 데모는 되도록 실제로 동작하게 만들되, 업무 데이터를 바꾸는 동작(등록·수정·삭제)은 실제 API를 부르지 않고 흉내만 낸다(예: `DataFetchingSamplePage`의 조회 데모는 실제 백엔드를 부르지만 저장 데모는 흉내다; `BackendGuideSamplePage`는 GET만 실제로 호출해 응답·오류 JSON을 그대로 보여준다). 역할 권한상 USER·SYSTEM 역할에도 조회 권한을 열어 뒀다(`app_role_menu`, 업무 데이터가 없는 화면이라 전 역할에 공개). 기본 메뉴 목록(`MenuInitializer`)과 USER 기본 권한(`RoleInitializer`)에도 샘플 화면 전체가 등록돼 있어 새 설치·테스트 환경에서도 같은 구성이 만들어진다. **여기 적힌 규칙은 이 문서·실제 코드와 같아야 한다.** Ant Design 6 이름 변경, 생성 타입 사용법, 목록 화면 골격, 탭·라우팅 구조, 백엔드 계층 구조 같은 규칙을 바꾸면 해당 샘플 화면의 설명과 코드도 같이 고친다.
- **버전 제약(Node 22.16):** orval은 Node 22.18+, react-router 8은 Node 22.22+가 필요해 쓰지 않았다. openapi-typescript가 `typescript ^5`를 요구해 TypeScript 7로 올리지 않는다. 의존성은 `--save-exact`로 고정한다.
