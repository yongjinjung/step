-- =====================================================================
-- V5: 사용자 (app_user)
-- ---------------------------------------------------------------------
-- 로그인 계정과 권한(역할). 계정은 공개 가입 없이 관리자(ADMIN)가 만든다.
-- 최초 기동 시 app_user가 비어 있으면 설정값(app.initial-admin.*)으로 첫 관리자를 만든다.
--
-- 역할:
--   ADMIN  표준 단어/도메인/용어 쓰기, 사용자 관리, 그 밖의 모든 기능
--   USER   조회, 테이블/컬럼 정의 쓰기 (표준 데이터 변경은 6단계에서 신청으로 한다)
--
-- 계정은 삭제하지 않고 비활성화(enabled = 'N')한다.
-- 6~7단계의 신청/변경 이력이 사용자를 참조하게 되므로 행을 지우면 이력이 끊긴다.
-- =====================================================================
CREATE TABLE app_user (
    -- 사용자 ID (자동 증가 기본키)
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    -- 로그인 아이디. 소문자로 정규화해 저장하며 전체에서 유일하다. 생성 후 바꿀 수 없다.
    -- 영문/숫자로 시작하고 영문/숫자/'.'/'_'/'-'만 쓴다. 4~50자. 예) hong.gildong
    username   VARCHAR(50)   NOT NULL,
    -- 비밀번호 해시. 평문은 저장하지 않는다.
    -- DelegatingPasswordEncoder 형식이라 '{bcrypt}$2a$10$...'처럼 알고리즘 접두어가 붙는다 (약 68자).
    password   VARCHAR(100)  NOT NULL,
    -- 표시 이름. 예) 홍길동
    name       VARCHAR(100)  NOT NULL,
    -- 역할. enum 이름 문자열 (ADMIN, USER)
    role       VARCHAR(20)   NOT NULL,
    -- 활성 여부 'Y'/'N'. 'N'이면 로그인할 수 없다.
    enabled    CHAR(1)       NOT NULL,
    -- 생성 일시 (JPA 감사)
    created_at DATETIME(6)   NOT NULL,
    -- 최종 수정 일시 (JPA 감사)
    updated_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    -- 아이디 유일 제약. 서비스에서 먼저 검사해 409(DUPLICATE_USERNAME)로 응답한다.
    CONSTRAINT uk_app_user_username UNIQUE (username)
);
