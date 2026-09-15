-- =====================================================================
-- V2: 표준 도메인 (std_domain)
-- ---------------------------------------------------------------------
-- 분류어 단어별 데이터 타입과 길이 규칙. 예) 번호V20 = 분류어 '번호', VARCHAR(20)
-- 용어는 도메인을 하나 참조하고, 컬럼 정의의 타입/길이는 이 값과 비교해 비표준을 검출한다.
--
-- 타입별 길이/소수점 규칙(DataType enum에서 검증):
--   CHAR     길이 1~255,   소수점 불가
--   VARCHAR  길이 1~65535, 소수점 불가
--   DECIMAL  길이 1~65,    소수점 0~min(길이, 30), 생략 시 0으로 저장
--   INTEGER, BIGINT, DATE, DATETIME, TEXT  길이/소수점 모두 NULL
-- =====================================================================
CREATE TABLE std_domain (
    -- 도메인 ID (자동 증가 기본키)
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    -- 도메인명. 사용자가 직접 입력하며 전체에서 유일하다. 예) 번호V20
    name               VARCHAR(100)  NOT NULL,
    -- 분류어 단어 ID (std_word.id). 반드시 classifier = 'Y'인 단어여야 한다(서비스에서 검증).
    classifier_word_id BIGINT        NOT NULL,
    -- 데이터 타입. enum 이름을 문자열로 저장한다 (CHAR, VARCHAR, DECIMAL, INTEGER, BIGINT, DATE, DATETIME, TEXT)
    -- MySQL ENUM 타입을 쓰지 않는 이유: 방언이 STRING enum을 ENUM으로 간주해 validate가 어긋나기 때문이다.
    data_type          VARCHAR(20)   NOT NULL,
    -- 길이. 길이를 쓰지 않는 타입이면 NULL
    data_length        INT,
    -- 소수점 자리수. DECIMAL만 값을 가지며 나머지 타입은 NULL
    data_scale         INT,
    -- 설명 (선택)
    description        VARCHAR(1000),
    -- 생성 일시 (JPA 감사)
    created_at         DATETIME(6)   NOT NULL,
    -- 최종 수정 일시 (JPA 감사)
    updated_at         DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    -- 도메인명 유일 제약 (409 DUPLICATE_DOMAIN_NAME)
    CONSTRAINT uk_std_domain_name UNIQUE (name),
    -- 분류어 단어 참조. 참조 중인 단어는 서비스에서 먼저 막아 409(WORD_IN_USE)로 응답한다.
    CONSTRAINT fk_std_domain_classifier_word FOREIGN KEY (classifier_word_id) REFERENCES std_word (id)
);
