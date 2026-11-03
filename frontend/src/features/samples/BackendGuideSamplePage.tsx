import { Alert, Button, Skeleton, Space, Table, Tag, Typography } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { isApiError } from '@/api/errors'
import { PageHeader } from '@/components/PageHeader'
import { SampleCode } from './SampleCode'
import { SampleSection } from './SampleSection'

/**
 * 서버(백엔드) 개발 가이드 샘플 화면.
 *
 * 이 프로젝트의 백엔드는 Spring Boot 4 계층형 구조(Controller → Service → Repository)를 기능 단위
 * 패키지(`standard/word` 등)로 나눠 반복한다. 표준 단어(`standard/word`)를 기준 예시로 삼아 설명한다.
 * "실행 결과" 중 일부는 실제 백엔드를 호출해 진짜 요청/응답 JSON을 그대로 보여준다(읽기 전용, 안전).
 */
export function BackendGuideSamplePage() {
  return (
    <>
      <PageHeader title="서버 개발 가이드" subTitle="Spring Boot 4 · 계층 구조 · MyBatis · 오류 처리 · 테스트" />

      <Space orientation="vertical" size="large" className="full-width">
        <Alert
          type="info"
          showIcon
          title="기준 예시: standard/word"
          description={
            <>
              새 기능을 만들 때 무엇부터 볼지 모르겠으면 <code>standard/word</code> 패키지를 그대로 따라 한다. 이
              화면의 모든 코드는 그 패키지(<code>Word</code>, <code>WordRepository</code>, <code>WordService</code>,{' '}
              <code>WordController</code>)와 매퍼 XML(<code>WordMapper.xml</code>)에서 그대로 가져왔다. API 문서는{' '}
              <a href="http://localhost:8080/swagger-ui/index.html" target="_blank" rel="noreferrer">
                Swagger UI
              </a>
              에서 직접 눌러 볼 수 있다(공개, 로그인 없이 열람 가능).
            </>
          }
        />

        <SampleSection
          title="1. 계층 구조 — Controller → Service → Repository"
          summary={
            <>
              한 기능은 <b>패키지 하나</b>에 Entity, Mapper(SQL), Repository, Service, Controller,{' '}
              <code>dto/</code>를 모두 둔다. 여러 기능이 공유하는 코드만 <code>global/</code>에 둔다.
            </>
          }
          points={[
            <>
              <b>Controller</b>: HTTP만 안다. 요청 검증(<code>@Valid</code>)과 응답 형태(상태 코드, Location 헤더)만
              맡고 업무 규칙은 없다.
            </>,
            <>
              <b>Service</b>: 업무 규칙 전부(중복 검사, 참조 검사, 변경 이력)가 여기 있다. 클래스 기본은 읽기 전용
              트랜잭션(<code>@Transactional(readOnly = true)</code>)이고, 바꾸는 메서드에만{' '}
              <code>@Transactional</code>을 다시 붙인다.
            </>,
            <>
              <b>Repository</b>: 서비스가 쓰는 저장·조회 창구(<code>save</code>, <code>findById</code>,{' '}
              <code>existsByName</code>, <code>search</code>). SQL은 직접 쓰지 않고 <b>Mapper</b>(MyBatis)를 부른다.
              SQL은 <code>resources/mapper/</code>의 XML에 있다.
            </>,
            <>
              직접 API 호출과 변경 신청 승인(<code>StandardDataApplier</code>) 둘 다 <b>같은 Service</b>를 거친다. 업무
              규칙을 두 번 만들지 않는다.
            </>,
          ]}
          code={`com.step.dictionary.standard.word
├── Word.java                    엔티티 (일반 자바 클래스 + 업무 메서드)
├── WordMapper.java              MyBatis 매퍼 인터페이스 (@Mapper, 메서드 하나 = SQL 하나)
├── WordRepository.java          저장·조회 창구 (@Repository, 매퍼 호출)
├── WordService.java             업무 규칙 (여기가 핵심)
├── WordController.java          HTTP 매핑만
└── dto/
    ├── WordRequest.java         등록/수정 요청 (record)
    ├── WordResponse.java        응답 (record)
    └── WordSearchCondition.java 검색 조건 (record)

src/main/resources/mapper/standard/word
└── WordMapper.xml               SQL (namespace = WordMapper 전체 이름)`}
        />

        <SampleSection
          title="2. 요청 하나의 흐름 — 실제로 호출해서 확인"
          summary={
            <>
              아래 버튼을 누르면 이 화면이 실제로 <code>GET /api/words?size=2</code>를 부른다. 브라우저가 받은 JSON을
              그대로 보여준다 — 이게 <code>WordController.search()</code>가 돌려준 진짜 응답이다.
            </>
          }
          points={[
            <>
              <code>WordController.search()</code> → <code>WordService.search()</code> →{' '}
              <code>WordRepository.search()</code> → <code>WordMapper.search()</code>·<code>count()</code>(XML의 SQL)
              순서로 호출되고, 결과가{' '}
              <code>PageResponse</code>로 감싸져 나온다(4번에서 자세히).
            </>,
            <>
              같은 값이라도 <code>@ParameterObject</code>로 받은 <code>WordSearchCondition</code>·
              <code>Pageable</code>이 Swagger 문서에는 <code>keyword</code>, <code>classifier</code>, <code>page</code>,{' '}
              <code>size</code> 개별 항목으로 펼쳐져 보인다.
            </>,
          ]}
          demo={<LiveRequestDemo />}
          code={`@GetMapping
public PageResponse<WordResponse> search(
        @ParameterObject WordSearchCondition condition,
        @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return wordService.search(condition, pageable);
}

// WordService
public PageResponse<WordResponse> search(WordSearchCondition condition, Pageable pageable) {
    return PageResponse.from(wordRepository.search(condition, pageable).map(WordResponse::from));
}`}
          codeTitle="WordController.java · WordService.java"
        />

        <SampleSection
          title="3. 엔티티와 DB 매핑 규칙"
          summary={
            <>
              엔티티는 어노테이션 없는 일반 자바 클래스다. 컬럼 ↔ 필드 연결은 매퍼 XML이 하고,{' '}
              <code>boolean</code> Y/N 컬럼, 생성·수정 일시, 정규화 로직이 어디에 있는지가 핵심이다.
            </>
          }
          points={[
            <>
              <b>Y/N 컬럼</b>: DB는 <code>CHAR(1)</code>, 엔티티는 <code>boolean</code>. 전역 타입 핸들러(
              <code>YesNoTypeHandler</code>)가 저장·조회 때 'Y'/'N'으로 바꿔 준다. 컬럼명은{' '}
              <code>map-underscore-to-camel-case</code> 설정으로 <code>english_name</code> → <code>englishName</code>
              처럼 자동으로 이어진다.
            </>,
            <>
              <b>생성·수정 일시</b>: 직접 채우지 않는다. <code>BaseTimeEntity</code>를 상속하면 MyBatis 인터셉터(
              <code>AuditingInterceptor</code>)가 insert/update 직전에 채운다. SQL에는 <code>#{'{createdAt}'}</code>,{' '}
              <code>#{'{updatedAt}'}</code>을 적어 둔다.
            </>,
            <>
              <b>정규화</b>: 영문약어를 대문자로 바꾸는 규칙은 엔티티 안(<code>normalizeAbbreviation</code>)에 두고,
              생성자와 <code>update()</code> 양쪽에서 같이 쓴다 — Service에서 각자 대문자로 바꾸면 한쪽에서 빠뜨리기
              쉽다.
            </>,
            <>
              스키마는 <b>Flyway만</b> 관리한다. 엔티티를 바꾸면 같은 작업에서 <code>V{'{다음 번호}'}__설명.sql</code>과
              매퍼 XML의 SQL도 고친다. 이미 적용된 파일은 절대 고치지 않는다.
            </>,
          ]}
          code={`public class Word extends BaseTimeEntity {   // createdAt/updatedAt은 인터셉터가 채운다

    private Long id;               // insert 후 useGeneratedKeys로 채워진다
    private String name;
    private boolean classifier;    // DB CHAR(1) 'Y'/'N' ↔ boolean (YesNoTypeHandler)

    protected Word() {}            // MyBatis가 조회 결과로 객체를 만들 때 쓴다

    public Word(String name, String abbreviation, ...) {
        this.abbreviation = normalizeAbbreviation(abbreviation);   // 정규화는 엔티티 안에 한 곳
    }

    static String normalizeAbbreviation(String abbreviation) {
        return abbreviation.toUpperCase(Locale.ROOT);
    }
}

-- V1__create_std_word.sql (MySQL·H2 양쪽에서 동작해야 한다: ENGINE=, COMMENT 금지)
CREATE TABLE std_word (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    classifier   CHAR(1)      NOT NULL,          -- Y/N
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_std_word_name UNIQUE (name)
);`}
          codeTitle="Word.java · V1__create_std_word.sql"
        />

        <SampleSection
          title="4. 요청/응답 DTO — record + Bean Validation"
          summary={
            <>
              요청·응답은 전부 <code>record</code>다. 검증 규칙은 <code>jakarta.validation</code> 애너테이션으로
              선언하고, 컨트롤러의 <code>@Valid</code>가 실패하면 400(<code>INVALID_INPUT</code>)이 자동으로 나간다
              (직접 if문으로 검사하지 않는다).
            </>
          }
          points={[
            <>
              목록 응답은 Spring <code>Page</code>를 그대로 주지 않는다. 내부 구조가 그대로 노출되고 버전마다 모양이
              바뀔 수 있어 <code>PageResponse</code>로 필요한 값만 고정된 형태로 감싼다.
            </>,
            <>
              길이 제한(<code>@Size(max = 100)</code>)은 <b>DB 컬럼 길이와 반드시 맞춘다</b>. 다르면 화면에서는
              통과했는데 DB에서 잘리거나 오류가 나는 상황이 생긴다.
            </>,
            <>
              기본형 <code>boolean</code> 대신 래퍼 <code>Boolean</code> + <code>@NotNull</code>을 쓰는 이유: 기본형은
              값이 없으면 자동으로 <code>false</code>가 들어가 "안 보냄"과 "false로 보냄"을 구분할 수 없다.
            </>,
          ]}
          demo={<PageResponseShapeDemo />}
          code={`public record WordRequest(
        @NotBlank @Size(max = 100)                     // std_word.name VARCHAR(100)과 같은 길이
        String name,

        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9]*$")
        String abbreviation,

        @NotNull                                        // boolean이 아니라 Boolean — 누락을 구분한다
        Boolean classifier
) {}

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PageResponse<T> from(Page<T> page) { ... }
}`}
        />

        <SampleSection
          title="5. MyBatis 동적 검색"
          summary={
            <>
              검색 조건이 여러 개고 각각 있을 수도 없을 수도 있을 때 매퍼 XML의 <code>{'<where>'}</code> +{' '}
              <code>{'<if>'}</code>로 조건을 붙인다. <b>값이 있을 때만</b> 조건이 붙고, 첫 <code>AND</code>는{' '}
              <code>{'<where>'}</code>가 알아서 지운다. 목록 SQL과 건수 SQL이 같은 조건 조각을 쓴다.
            </>
          }
          points={[
            <>
              부분 일치는 <code>Likes.contains()</code>로 패턴을 만들고 <code>ESCAPE '!'</code>를 붙인다. 사용자가
              입력한 <code>%</code>, <code>_</code>가 와일드카드가 아니라 글자로 검색된다.
            </>,
            <>
              리포지토리는 <code>Pages.of(목록, pageable, () -&gt; 건수)</code>로 묶는다. 첫 페이지인데 결과가 페이지
              크기보다 적으면 건수 SQL을 아예 안 돌린다(성능).
            </>,
            <>
              MyBatis는 객체 변경을 감지하지 않는다. 엔티티를 바꾼 뒤에는 반드시 <code>repository.save()</code>를
              불러야 DB에 반영된다.
            </>,
            <>
              아래 데모는 조건을 바꿔 가며 실제로 호출해, 조건에 따라 <code>totalElements</code>가 달라지는 것을
              보여준다.
            </>,
          ]}
          demo={<DynamicSearchDemo />}
          code={`<!-- WordMapper.xml -->
<sql id="searchWhere">
    <bind name="keyword" value="@com.step.dictionary.global.mybatis.Likes@contains(c.keyword)"/>
    <where>                                                   <!-- 값이 있는 조건만 붙는다 -->
        <if test="keyword != null">
            AND (w.name LIKE #{keyword} ESCAPE '!'
                 OR LOWER(w.abbreviation) LIKE LOWER(#{keyword}) ESCAPE '!')
        </if>
        <if test="c.classifier != null">AND w.classifier = #{c.classifier}</if>
    </where>
</sql>

<select id="search" resultMap="word">
    SELECT <include refid="columns"/> FROM std_word w
    <include refid="searchWhere"/>
    ORDER BY w.name ASC
    LIMIT #{p.pageSize} OFFSET #{p.offset}
</select>

<select id="count" resultType="long">
    SELECT COUNT(*) FROM std_word w <include refid="searchWhere"/>
</select>

// WordMapper.java — 메서드 이름 = XML의 id
List<Word> search(@Param("c") WordSearchCondition condition, @Param("p") Pageable pageable);
long count(@Param("c") WordSearchCondition condition);

// WordRepository.java
public Page<Word> search(WordSearchCondition condition, Pageable pageable) {
    return Pages.of(mapper.search(condition, pageable), pageable, () -> mapper.count(condition));
}`}
          codeTitle="WordMapper.xml · WordMapper.java · WordRepository.java"
        />

        <SampleSection
          title="6. 오류 처리 — BusinessException + ErrorCode"
          summary={
            <>
              서비스는 실패하면 <code>throw new BusinessException(ErrorCode.XXX)</code> 한 줄이면 된다.{' '}
              <code>GlobalExceptionHandler</code>가 어디서 잡을지, 어떤 JSON을 만들지 전부 대신한다.
            </>
          }
          points={[
            <>
              HTTP 상태 기준: <Tag color="blue">400</Tag> 입력값 자체가 규칙 위반, <Tag color="orange">404</Tag> 지정한
              대상이 없음, <Tag color="red">409</Tag> 입력은 맞는데 현재 데이터와 충돌(중복·참조 중).
            </>,
            <>
              401·403은 컨트롤러에 도달하기 전 보안 필터에서 나므로 <code>GlobalExceptionHandler</code>가 아니라{' '}
              <code>SecurityErrorHandler</code>가 <b>같은 응답 형식</b>으로 처리한다. 화면에서는 구분할 필요 없다.
            </>,
            <>
              모든 오류 응답은 <code>{'{ code, message, fieldErrors }'}</code> 한 형식이다. 프론트는 문자열 메시지가
              아니라 <code>code</code>로 분기한다(<code>isApiError</code>, "API 연동" 샘플 8번).
            </>,
          ]}
          demo={<LiveErrorDemo />}
          code={`// ErrorCode.java
WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "단어를 찾을 수 없습니다."),
DUPLICATE_WORD_NAME(HttpStatus.CONFLICT, "이미 등록된 단어명입니다."),

// WordService.java
private Word getWord(Long id) {
    return wordRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));   // 이 한 줄이면 끝
}

// GlobalExceptionHandler.java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();
    return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
}`}
        />

        <SampleSection
          title="7. 업무 규칙 순서 — 중복 검사와 참조 삭제 차단"
          summary={
            <>
              두 가지는 순서를 틀리면 엉뚱한 오류(500)가 난다: <b>정규화 후 비교</b>와{' '}
              <b>엔티티를 바꾸기 전에 검사</b>.
            </>
          }
          points={[
            <>
              수정은 <b>엔티티를 바꾸기 전에</b> 중복·참조를 검사한다. 검사에 지금 값(예: 분류어였는지, 이름이
              바뀌는지)이 필요하고, 검사에 걸리면 저장하지 않고 409로 끝내야 하기 때문이다.
            </>,
            <>
              다른 엔티티가 참조하는 대상은 <code>existsBy...</code>로 먼저 확인해 409(<code>XXX_IN_USE</code>)를
              던진다. DB FK 위반(500)에 그냥 맡기지 않는다 — 500은 사용자에게 원인을 설명할 수 없다.
            </>,
            <>
              영문약어처럼 대소문자를 가리지 않는 값은 <b>정규화한 값으로</b> 비교한다. 그래야 <code>cust</code>와{' '}
              <code>CUST</code>를 같은 값으로 판단한다.
            </>,
          ]}
          code={`@Transactional
public WordResponse update(Long id, WordRequest request) {
    Word word = getWord(id);
    String abbreviation = Word.normalizeAbbreviation(request.abbreviation());   // 1) 정규화

    // 2) 엔티티를 바꾸기 전에 검사한다 (지금 값과 비교하고, 걸리면 저장 전에 409로 끝낸다)
    if (wordRepository.existsByAbbreviationAndIdNot(abbreviation, id)) {
        throw new BusinessException(ErrorCode.DUPLICATE_WORD_ABBREVIATION);
    }
    // 3) 참조 중인지 먼저 확인하고 409로 던진다 (DB FK 위반=500에 맡기지 않는다)
    if (word.isClassifier() && !request.classifier() && domainRepository.existsByClassifierWordId(id)) {
        throw new BusinessException(ErrorCode.CLASSIFIER_WORD_IN_USE);
    }

    word.update(request.name(), abbreviation, ...);   // 4) 검사를 통과한 뒤에야 바꾸고
    wordRepository.save(word);                         // 5) 직접 저장한다 (변경 감지 없음)
    ...
}`}
        />

        <SampleSection
          title="8. 변경 이력 기록"
          summary={
            <>
              단어·도메인·용어·테이블·컬럼·코드 그룹·코드값을 바꾸는 메서드는 <b>직접</b> 이력을 남긴다(AOP나
              이벤트가 아니라 명시적 호출).
            </>
          }
          points={[
            <>
              수정은 엔티티를 바꾸기 <b>전에</b> <code>before</code> 스냅샷을 떠 두고, 변경·저장 후{' '}
              <code>after</code> 스냅샷과 함께 기록한다. 등록은 저장 후, 삭제는 지우기 전에 기록한다(지우면 값을 읽을
              수 없으므로).
            </>,
            <>
              스냅샷에 수정 일시 같은 자동 값을 넣지 않는다 — 넣으면 내용이 안 바뀐 저장도 "변경"으로 보인다.
            </>,
            <>
              변경자·신청 ID는 기록기가 보안 컨텍스트에서 알아서 채운다. Service는 무엇이 바뀌었는지만 넘긴다.
            </>,
          ]}
          code={`@Transactional
public WordResponse update(Long id, WordRequest request) {
    Word word = getWord(id);
    WordSnapshot before = WordSnapshot.from(word);      // 바뀌기 전 스냅샷 (반드시 먼저)
    ...
    word.update(request.name(), abbreviation, ...);
    wordRepository.save(word);                           // update 실행 (updatedAt도 함께 갱신된다)
    historyRecorder.recordUpdate(before, WordSnapshot.from(word));   // 바뀐 뒤 스냅샷과 함께 기록
    return WordResponse.from(word);
}`}
        />

        <SampleSection
          title="9. 권한 검사 — 새 API를 추가하면 꼭 할 일"
          summary={
            <>
              메서드 보안(<code>@PreAuthorize</code>)은 쓰지 않는다. 대신 요청 경로 → 메뉴 권한으로 옮기는 규칙표{' '}
              <code>ApiPermissionRules</code> 하나에 전부 모여 있다.
            </>
          }
          points={[
            <>
              기본은 HTTP 메서드로 동작이 정해진다: <code>GET</code>=조회, <code>POST</code>=등록,{' '}
              <code>PUT</code>/<code>PATCH</code>=수정, <code>DELETE</code>=삭제(<code>crud()</code> 헬퍼).
            </>,
            <>
              <b>규칙이 없는 경로는 전권 역할이 아니면 무조건 막힌다.</b> 새 컨트롤러를 만들고 규칙 추가를 잊으면
              403만 나서 바로 눈에 띈다.
            </>,
            <>
              동작이 메서드로 안 정해지는 예외(승인=수정 권한, 취소=조회 권한 등)만 규칙표 위쪽에 따로 적는다.
            </>,
            <>
              새 API를 추가하면: ① <code>ApiPermissionRules</code>에 규칙 추가 → ② 메뉴가 필요하면{' '}
              <code>MenuInitializer</code>/메뉴 관리 화면에 추가 → ③{' '}
              <code>SecurityApiIntegrationTest</code>·해당 기능의 <code>RoleApiIntegrationTest</code>로 확인.
            </>,
          ]}
          code={`// ApiPermissionRules.java
rules.add(new Rule(HttpMethod.POST, "/api/requests/*/approve",
        req(MenuPaths.REQUESTS, PermissionAction.UPDATE)));   // 예외: 승인은 항상 "수정" 권한

rules.addAll(crud("/api/words/**", MenuPaths.WORDS));          // 나머지는 메서드로 자동 결정

private static List<Rule> crud(String pattern, String menuPath) {
    return List.of(
        new Rule(HttpMethod.GET, pattern, req(menuPath, PermissionAction.READ)),
        new Rule(HttpMethod.POST, pattern, req(menuPath, PermissionAction.CREATE)),
        new Rule(HttpMethod.PUT, pattern, req(menuPath, PermissionAction.UPDATE)),
        new Rule(HttpMethod.DELETE, pattern, req(menuPath, PermissionAction.DELETE)));
}

// MenuAuthorizationManager.java — 실제 검사
List<Requirement> required = ApiPermissionRules.requirementsFor(method, path);
if (required.isEmpty()) return new AuthorizationDecision(false);   // 규칙 없으면 거부
UserPermissions permissions = permissionService.of(auth.getName());
boolean allowed = required.stream().anyMatch(r -> permissions.can(r.menuPath(), r.action()));`}
        />

        <SampleSection
          title="10. 테스트 3단계"
          summary={<>테스트는 검사 범위에 따라 세 가지를 구분해서 쓴다. 전부 만들 필요는 없고, 규칙이 복잡할수록 아래로 내려간다.</>}
          points={[
            <>
              <b>단위</b>(<code>WordServiceTest</code>): 스프링 없이 <code>@Mock</code> 리포지토리로 서비스 로직만
              검사한다. 가장 빠르고 많이 쓴다.
            </>,
            <>
              <b>리포지토리</b>(<code>WordRepositoryTest</code>): <code>@MybatisTest</code> + 실제 SQL(매퍼 XML의 검색
              조건)을 DB에 대고 검사한다. <code>@AutoConfigureTestDatabase(replace = NONE)</code>를 꼭 붙이고(빼면
              MySQL 모드가 아닌 임베디드 H2로 바뀐다), <code>@Import</code>에 <code>MyBatisConfig</code>와 쓰는
              리포지토리를 적는다.
            </>,
            <>
              <b>통합</b>(<code>WordApiIntegrationTest</code>): <code>support.IntegrationTest</code>를 상속해 실제
              HTTP 요청(REST Assured)으로 컨트롤러부터 DB까지 전부 검사한다. 상태 코드·오류 코드까지 확인할 때 쓴다.
            </>,
            <>
              변경 이력을 남기는 서비스의 단위 테스트는 <code>@Mock ChangeHistoryRecorder</code>를 꼭 선언한다.
              빠뜨리면 <code>@InjectMocks</code>가 생성자에 <code>null</code>을 넣어 NPE가 난다.
            </>,
          ]}
          code={`// 단위 — 스프링 없이 순수 자바 + Mockito
@ExtendWith(MockitoExtension.class)
class WordServiceTest {
    @Mock private WordRepository wordRepository;
    @Mock private ChangeHistoryRecorder historyRecorder;   // 빠뜨리면 NPE
    @InjectMocks private WordService wordService;

    @Test
    @DisplayName("단어를 등록하면 영문약어를 대문자로 정규화해 저장한다")
    void create() {
        given(wordRepository.save(any(Word.class))).willAnswer(invocation -> invocation.getArgument(0));
        WordResponse response = wordService.create(new WordRequest("고객", "cust", "Customer", null, false));
        assertThat(response.abbreviation()).isEqualTo("CUST");
    }
}

// 통합 — 실제 HTTP 요청
class WordApiIntegrationTest extends IntegrationTest {
    @Test
    @DisplayName("영문약어가 중복되면 409를 반환한다")
    void createDuplicateAbbreviation() {
        createWord("고객", "CUST", "Customer", false);
        given().contentType(ContentType.JSON).body(wordJson("손님", "cust", "Guest", false))
        .when().post("/api/words")
        .then().statusCode(409).body("code", equalTo("DUPLICATE_WORD_ABBREVIATION"));
    }
}`}
        />

        <SampleSection
          title="11. 새 기능 하나 만드는 순서"
          summary={
            <>
              <code>standard/word</code>를 본떠 새 기능(예: <code>standard/xxx</code>)을 만들 때의 순서다. 프론트엔드
              연동은 "API 연동" 샘플과 이어진다.
            </>
          }
          points={[
            <>① 엔티티(<code>Xxx.java</code>) — 필드, 정규화·검증 로직(3번)</>,
            <>② Flyway 마이그레이션(<code>V{'{n}'}__create_xxx.sql</code>) — 엔티티와 같은 작업에서 함께</>,
            <>③ <code>XxxMapper</code> + <code>mapper/.../XxxMapper.xml</code> + <code>XxxRepository</code> — 저장·조회 SQL과 동적 검색(5번)</>,
            <>④ <code>ErrorCode</code>에 이 기능의 오류 추가 — 404/409/400 기준(6번)</>,
            <>⑤ <code>dto/XxxRequest</code>·<code>XxxResponse</code>·<code>XxxSearchCondition</code>(4번)</>,
            <>⑥ <code>XxxService</code> — 중복 검사·참조 차단(7번) + 변경 이력(8번)</>,
            <>⑦ <code>XxxController</code> — HTTP 매핑만</>,
            <>⑧ <code>ApiPermissionRules</code>에 권한 규칙 추가(9번), 필요하면 메뉴도 추가</>,
            <>⑨ 테스트(10번) — 최소한 서비스 단위 테스트와 통합 테스트</>,
            <>
              ⑩ 백엔드를 띄우고 <code>npm run api:generate</code> — 프론트가 새 API를 쓸 수 있게 타입을 만든다("API
              연동" 샘플 참고)
            </>,
          ]}
        />
      </Space>
    </>
  )
}

/* ------------------------------------------------------------------ */
/* 데모 컴포넌트 — 실제 백엔드를 호출한다(GET만, 읽기 전용)                  */
/* ------------------------------------------------------------------ */

/** 2번 데모: 진짜 GET /api/words 응답 JSON을 그대로 보여준다 */
function LiveRequestDemo() {
  const [run, setRun] = useState(false)
  const query = $api.useQuery('get', '/api/words', { params: { query: { size: 2 } } }, { enabled: run })

  return (
    <Space orientation="vertical" className="full-width">
      <Button type="primary" onClick={() => setRun(true)}>
        GET /api/words?size=2 실제로 호출하기
      </Button>
      {!run ? (
        <Typography.Text type="secondary">버튼을 누르면 이 화면이 실제 백엔드를 호출한다.</Typography.Text>
      ) : query.isPending ? (
        <Skeleton active paragraph={{ rows: 3 }} />
      ) : query.isError ? (
        <Alert type="error" showIcon title="호출에 실패했습니다. 로그인 상태와 권한을 확인하세요." />
      ) : (
        <SampleCode title="실제 응답 (WordController.search()가 돌려준 그대로)" code={JSON.stringify(query.data, null, 2)} />
      )}
    </Space>
  )
}

/** 4번 데모: PageResponse의 필드 하나하나를 실제 응답에서 짚어 보여준다 */
function PageResponseShapeDemo() {
  const query = $api.useQuery('get', '/api/words', { params: { query: { size: 1 } } })

  if (query.isPending) {
    return <Skeleton active paragraph={{ rows: 2 }} />
  }
  if (query.isError || !query.data) {
    return <Alert type="error" showIcon title="호출에 실패했습니다." />
  }
  const { content, page, size, totalElements, totalPages } = query.data

  const rows = [
    { field: 'content', value: `단어 ${content?.length ?? 0}건 (배열)`, note: '이 페이지의 실제 데이터' },
    { field: 'page', value: String(page), note: '현재 페이지 (0부터)' },
    { field: 'size', value: String(size), note: '요청한 페이지 크기' },
    { field: 'totalElements', value: String(totalElements), note: '조건에 맞는 전체 건수' },
    { field: 'totalPages', value: String(totalPages), note: '전체 페이지 수' },
  ]

  return (
    <Table
      size="small"
      pagination={false}
      rowKey="field"
      dataSource={rows}
      columns={[
        { title: '필드', dataIndex: 'field', width: 140, render: (v: string) => <code>{v}</code> },
        { title: '지금 실제 값', dataIndex: 'value', width: 200 },
        { title: '의미', dataIndex: 'note' },
      ]}
    />
  )
}

type DemoFilter = 'all' | 'classifier' | 'keyword'

const FILTER_LABEL: Record<DemoFilter, string> = { all: '전체', classifier: '분류어만', keyword: "'고' 포함" }

/** 5번 데모: 조건을 바꿔 가며 실제 호출해 totalElements가 달라지는 것을 보여준다 */
function DynamicSearchDemo() {
  const [filter, setFilter] = useState<DemoFilter>('all')
  const query = $api.useQuery('get', '/api/words', {
    params: {
      query: {
        size: 1,
        classifier: filter === 'classifier' ? true : undefined,
        keyword: filter === 'keyword' ? '고' : undefined,
      },
    },
  })

  return (
    <Space orientation="vertical" className="full-width">
      <Space wrap>
        {(['all', 'classifier', 'keyword'] as const).map((value) => (
          <Button key={value} type={value === filter ? 'primary' : 'default'} onClick={() => setFilter(value)}>
            {FILTER_LABEL[value]}
          </Button>
        ))}
      </Space>
      {query.isPending ? (
        <Skeleton active paragraph={{ rows: 1 }} />
      ) : query.isError ? (
        <Alert type="error" showIcon title="호출에 실패했습니다." />
      ) : (
        <Typography.Text>
          조건 <Tag>{FILTER_LABEL[filter]}</Tag>의 실제 전체 건수(<code>totalElements</code>):{' '}
          <Tag color="blue">{query.data?.totalElements ?? 0}건</Tag>
        </Typography.Text>
      )}
    </Space>
  )
}

/** 6번 데모: 실제로 없는 id를 조회해 진짜 404 ErrorResponse 형태를 보여준다 */
function LiveErrorDemo() {
  const [run, setRun] = useState(false)
  // 전역 재시도 정책(main.tsx)이 이미 업무 오류(ApiError)는 재시도하지 않으므로 여기서 따로 끄지 않는다.
  const query = $api.useQuery('get', '/api/words/{id}', { params: { path: { id: 999_999_999 } } }, { enabled: run })

  // 오류 응답은 API 문서에 선언돼 있지 않아 생성 타입이 없다(unknown으로 받아 isApiError로 좁힌다).
  const error: unknown = query.error
  const errorJson = isApiError(error) ? JSON.stringify({ code: error.code, message: error.message, fieldErrors: error.fieldErrors }, null, 2) : undefined

  return (
    <Space orientation="vertical" className="full-width">
      <Button type="primary" danger onClick={() => setRun(true)}>
        GET /api/words/999999999 실제로 호출하기 (없는 id)
      </Button>
      {!run ? (
        <Typography.Text type="secondary">버튼을 누르면 실제로 404 오류를 받아 그 형태를 보여준다.</Typography.Text>
      ) : query.isPending ? (
        <Skeleton active paragraph={{ rows: 2 }} />
      ) : errorJson ? (
        <SampleCode title="실제 오류 응답 (HTTP 404)" code={errorJson} />
      ) : (
        <Alert type="warning" showIcon title="이 id의 단어가 실제로 있습니다. 다른 환경에서 시도해 보세요." />
      )}
    </Space>
  )
}
