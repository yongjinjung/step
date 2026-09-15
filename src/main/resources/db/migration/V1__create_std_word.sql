-- =====================================================================
-- V1: 표준 단어 (std_word)
-- ---------------------------------------------------------------------
-- 용어를 구성하는 최소 단위. 예) 고객 / CUST / Customer
-- 분류어(classifier = 'Y')는 용어의 마지막에 붙어 데이터 성격을 나타내는
-- 단어다 (번호, 명, 일자, 금액, 여부 등). 도메인은 분류어 단어에만 연결된다.
--
-- 주의: 이 파일은 MySQL 8.4와 테스트용 H2(MODE=MySQL) 양쪽에서 실행된다.
--       ENGINE=, 테이블/컬럼 COMMENT 같은 MySQL 전용 구문은 쓰지 않는다.
-- =====================================================================
CREATE TABLE std_word (
    -- 단어 ID (자동 증가 기본키)
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    -- 단어명(한글). 전체에서 유일하다. 예) 고객
    name         VARCHAR(100)  NOT NULL,
    -- 영문약어. 전체에서 유일하며 애플리케이션에서 대문자로 정규화해 저장한다.
    -- 용어 영문약어(물리명)를 만들 때 '_'로 이어 붙이므로 영문/숫자만 허용한다. 예) CUST
    abbreviation VARCHAR(50)   NOT NULL,
    -- 영문명(전체 표기). 유일하지 않다. 예) Customer
    english_name VARCHAR(200)  NOT NULL,
    -- 설명 (선택)
    description  VARCHAR(1000),
    -- 분류어 여부. 'Y' 또는 'N' (엔티티에서 boolean + YesNoConverter로 매핑)
    -- BOOLEAN(TINYINT)을 쓰지 않는 이유: MySQL에서 ddl-auto=validate 타입 불일치를 피하기 위해서다.
    classifier   CHAR(1)       NOT NULL,
    -- 생성 일시 (JPA 감사 기능이 채운다)
    created_at   DATETIME(6)   NOT NULL,
    -- 최종 수정 일시 (JPA 감사 기능이 채운다)
    updated_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    -- 단어명 유일 제약. 서비스에서 먼저 검사해 409(DUPLICATE_WORD_NAME)로 응답한다.
    CONSTRAINT uk_std_word_name UNIQUE (name),
    -- 영문약어 유일 제약. 서비스에서 먼저 검사해 409(DUPLICATE_WORD_ABBREVIATION)로 응답한다.
    CONSTRAINT uk_std_word_abbreviation UNIQUE (abbreviation)
);
