package com.step.dictionary.standard.domain;

import com.step.dictionary.global.entity.BaseTimeEntity;
import com.step.dictionary.standard.word.Word;

/**
 * 표준 도메인. 분류어 단어에 속하며 데이터 타입과 길이 규칙을 정한다 (예: 번호V20 = 번호, VARCHAR(20)).
 *
 * <p>테이블: {@code std_domain} (V2 마이그레이션). 도메인명은 사용자가 입력하며 전체에서 유일하다.
 *
 * <p>참조 관계:
 * <ul>
 *   <li>분류어 단어({@link Word}, classifier=true)를 하나 참조한다.</li>
 *   <li>용어({@code Term.domain})가 이 도메인을 참조하고, 컬럼 비표준 검출 시 타입/길이 기준이 된다.</li>
 * </ul>
 * 분류어 여부 확인, 타입/길이 검증, 참조 시 삭제·분류어 변경 제한은 {@link DomainService}에서 한다.
 */
public class Domain extends BaseTimeEntity {

    /** 도메인 ID (AUTO_INCREMENT). */
    private Long id;

    /** 도메인명. 예: 번호V20. 자동 생성하지 않는다. */
    private String name;

    /**
     * 분류어 단어. 조회 쿼리에서 조인해 함께 가져온다
     * ({@link DomainRepositoryCustomImpl}).
     */
    private Word classifierWord;

    // MySQL 방언은 STRING enum을 ENUM 타입으로 보므로 VARCHAR 컬럼과 validate가 어긋나지 않게 지정한다.
    /** 데이터 타입. DB에는 enum 이름 문자열로 저장한다. */
    private DataType dataType;

    /** 길이. 길이를 쓰지 않는 타입이면 null. */
    private Integer dataLength;

    /** 소수점 자리수. DECIMAL은 생략 시 0, 나머지 타입은 null. */
    private Integer dataScale;

    /** 설명 (선택). */
    private String description;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected Domain() {
    }

    /**
     * 새 도메인을 만든다. 길이/소수점 조합은 호출 전에 {@link DataType#isValidSpec}으로 검증돼 있어야 한다.
     *
     * @param name           도메인명
     * @param classifierWord 분류어 단어 (classifier=true여야 한다)
     * @param dataType       데이터 타입
     * @param dataLength     길이 (null 가능)
     * @param dataScale      소수점 자리수 (null 가능, DECIMAL이면 0으로 정규화)
     * @param description    설명 (null 가능)
     */
    public Domain(String name, Word classifierWord, DataType dataType, Integer dataLength, Integer dataScale,
            String description) {
        this.name = name;
        this.classifierWord = classifierWord;
        this.dataType = dataType;
        this.dataLength = dataLength;
        this.dataScale = dataType.normalizeScale(dataScale);
        this.description = description;
    }

    /**
     * 모든 값을 교체한다 (PUT 의미). 타입/길이 변경은 이 도메인을 쓰는 용어와 컬럼 검출 결과에 바로 반영된다.
     * 중복·분류어 변경 제한 검사는 호출 전에 서비스에서 끝내야 한다.
     */
    public void update(String name, Word classifierWord, DataType dataType, Integer dataLength, Integer dataScale,
            String description) {
        this.name = name;
        this.classifierWord = classifierWord;
        this.dataType = dataType;
        this.dataLength = dataLength;
        this.dataScale = dataType.normalizeScale(dataScale);
        this.description = description;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Word getClassifierWord() {
        return classifierWord;
    }

    public DataType getDataType() {
        return dataType;
    }

    public Integer getDataLength() {
        return dataLength;
    }

    public Integer getDataScale() {
        return dataScale;
    }

    public String getDescription() {
        return description;
    }
}
