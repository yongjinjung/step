package com.step.dictionary.standard.word;

import com.step.dictionary.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;
import org.hibernate.type.YesNoConverter;

/**
 * 표준 단어. 용어를 구성하는 최소 단위 (예: 고객 / CUST / Customer).
 * 분류어(classifier)는 용어의 마지막에 붙어 도메인을 결정하는 단어다 (예: 번호, 명, 일자).
 *
 * <p>테이블: {@code std_word} (V1 마이그레이션). 단어명과 영문약어는 각각 전체에서 유일하다.
 *
 * <p>참조 관계:
 * <ul>
 *   <li>도메인({@code Domain.classifierWord})이 분류어로 참조한다.</li>
 *   <li>용어({@code Term.words})가 구성 단어로 참조한다.</li>
 * </ul>
 * 참조 중인 단어의 삭제·분류어 해제·이름 변경 제한은 {@link WordService}에서 검사한다.
 */
@Entity
@Table(name = "std_word")
public class Word extends BaseTimeEntity {

    /** 단어 ID. MySQL AUTO_INCREMENT로 생성되므로 save() 시점에 바로 insert된다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 단어명(한글). 예: 고객. 용어명은 이 값을 이어 붙여 만든다. */
    @Column(nullable = false, length = 100)
    private String name;

    /** 영문약어. 항상 대문자로 저장한다. 예: CUST. 용어 영문약어는 이 값을 '_'로 이어 만든다. */
    @Column(nullable = false, length = 50)
    private String abbreviation;

    /** 영문명(전체 표기). 예: Customer. 용어와 무관하므로 용어에서 쓰는 단어도 자유롭게 바꿀 수 있다. */
    @Column(nullable = false, length = 200)
    private String englishName;

    /** 설명 (선택). */
    @Column(length = 1000)
    private String description;

    /**
     * 분류어 여부. DB에는 'Y'/'N' 문자로 저장한다.
     * MySQL BOOLEAN(TINYINT)은 ddl-auto=validate에서 타입 불일치가 날 수 있어 CHAR(1) + YesNoConverter를 쓴다.
     */
    @Convert(converter = YesNoConverter.class)
    @Column(nullable = false, length = 1)
    private boolean classifier;

    /** JPA가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. 코드에서는 쓰지 않는다. */
    protected Word() {
    }

    /**
     * 새 단어를 만든다. 영문약어는 대문자로 정규화한다.
     *
     * @param name         단어명
     * @param abbreviation 영문약어 (대소문자 무관, 저장 시 대문자)
     * @param englishName  영문명
     * @param description  설명 (null 가능)
     * @param classifier   분류어 여부
     */
    public Word(String name, String abbreviation, String englishName, String description, boolean classifier) {
        this.name = name;
        this.abbreviation = normalizeAbbreviation(abbreviation);
        this.englishName = englishName;
        this.description = description;
        this.classifier = classifier;
    }

    /** 영문약어는 대문자로 저장한다. 중복 검사 전에도 같은 규칙을 적용해야 한다. */
    public static String normalizeAbbreviation(String abbreviation) {
        // Locale.ROOT: 터키어 로케일 등에서 'i'가 'İ'로 바뀌는 문제를 막는다.
        return abbreviation.toUpperCase(Locale.ROOT);
    }

    /**
     * 모든 값을 요청 값으로 교체한다 (PUT 의미). 트랜잭션 안에서 호출하면 변경 감지로 update된다.
     * 중복·참조 검사는 호출 전에 서비스에서 끝내야 한다.
     */
    public void update(String name, String abbreviation, String englishName, String description, boolean classifier) {
        this.name = name;
        this.abbreviation = normalizeAbbreviation(abbreviation);
        this.englishName = englishName;
        this.description = description;
        this.classifier = classifier;
    }

    // ---- 조회용 getter (setter는 두지 않고 변경은 update()로만 한다) ----

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isClassifier() {
        return classifier;
    }
}
