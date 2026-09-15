-- =====================================================================
-- V3: 표준 용어 (std_term) + 용어 구성 단어 (std_term_word)
-- ---------------------------------------------------------------------
-- 용어는 단어를 순서대로 조합하고 도메인을 하나 연결한다.
--   예) 고객 + 번호 → 용어명 '고객번호', 영문약어 'CUST_NO', 도메인 '번호V20'
-- 용어명과 영문약어는 사용자가 입력하지 않고 구성 단어에서 자동 생성한다.
-- 마지막 단어는 분류어여야 하고, 도메인의 분류어와 같아야 한다(서비스에서 검증).
-- =====================================================================
CREATE TABLE std_term (
    -- 용어 ID (자동 증가 기본키)
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    -- 용어명. 구성 단어명을 이어 붙여 생성한다. 전체에서 유일, 최대 200자. 예) 고객번호
    name         VARCHAR(200)  NOT NULL,
    -- 영문약어(물리명). 구성 단어 약어를 '_'로 이어 생성한다. 전체에서 유일.
    -- 컬럼명의 기준이 되므로 MySQL 식별자 최대 길이인 64자로 제한한다. 예) CUST_NO
    abbreviation VARCHAR(64)   NOT NULL,
    -- 도메인 ID (std_domain.id). 컬럼 비표준 검출 시 타입/길이 기준이 된다.
    domain_id    BIGINT        NOT NULL,
    -- 설명 (선택)
    description  VARCHAR(1000),
    -- 생성 일시 (JPA 감사)
    created_at   DATETIME(6)   NOT NULL,
    -- 최종 수정 일시 (JPA 감사)
    updated_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    -- 용어명 유일 제약 (409 DUPLICATE_TERM_NAME)
    CONSTRAINT uk_std_term_name UNIQUE (name),
    -- 영문약어 유일 제약 (409 DUPLICATE_TERM_ABBREVIATION)
    CONSTRAINT uk_std_term_abbreviation UNIQUE (abbreviation),
    -- 도메인 참조. 참조 중인 도메인은 서비스에서 먼저 막아 409(DOMAIN_IN_USE)로 응답한다.
    CONSTRAINT fk_std_term_domain FOREIGN KEY (domain_id) REFERENCES std_domain (id)
);

-- ---------------------------------------------------------------------
-- 용어 구성 단어 (조인 테이블)
-- 한 행이 "용어의 seq번째 단어"를 뜻한다. 같은 단어가 여러 번 나올 수 있으므로
-- (term_id, word_id)가 아니라 (term_id, seq)를 기본키로 둔다.
-- 엔티티에서는 Term.words(@ManyToMany + @OrderColumn(seq) + @ListIndexBase(1))로 매핑한다.
-- ---------------------------------------------------------------------
CREATE TABLE std_term_word (
    -- 용어 ID (std_term.id)
    term_id BIGINT NOT NULL,
    -- 조합 순서. 1부터 시작한다 (@ListIndexBase(1))
    seq     INT    NOT NULL,
    -- 단어 ID (std_word.id)
    word_id BIGINT NOT NULL,
    PRIMARY KEY (term_id, seq),
    -- 용어 참조. 용어를 삭제하면 JPA가 이 테이블의 행을 먼저 지운다.
    CONSTRAINT fk_std_term_word_term FOREIGN KEY (term_id) REFERENCES std_term (id),
    -- 단어 참조. 용어에서 쓰는 단어는 서비스에서 먼저 막아 409(WORD_IN_USE)로 응답한다.
    CONSTRAINT fk_std_term_word_word FOREIGN KEY (word_id) REFERENCES std_word (id)
);
