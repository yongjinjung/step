package com.step.dictionary.standard.term;

import static java.util.stream.Collectors.joining;

import com.step.dictionary.global.entity.BaseTimeEntity;
import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.word.Word;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ListIndexBase;

/**
 * 표준 용어. 단어를 순서대로 조합하고 도메인을 연결한다 (예: 고객+번호 → 고객번호 / CUST_NO, 번호V20).
 * 용어명과 영문약어(물리명)는 구성 단어에서 자동 생성한다.
 *
 * <p>테이블: {@code std_term}, 구성 단어 조인 테이블 {@code std_term_word} (V3 마이그레이션).
 *
 * <p>불변식: {@link #name}과 {@link #abbreviation}은 항상 {@link #words}에서 계산한 값과 같다.
 * 그래서 단어 목록은 {@link #changeWords}로만 바꾸고, 용어에서 쓰는 단어의 이름/약어 변경은 {@code WordService}가 막는다.
 * 마지막 단어가 분류어이고 도메인 분류어와 같다는 조합 규칙은 {@link TermService}에서 검증한다.
 */
@Entity
@Table(name = "std_term")
public class Term extends BaseTimeEntity {

    /** 용어명 최대 길이 ({@code std_term.name} VARCHAR(200)). */
    public static final int MAX_NAME_LENGTH = 200;
    /** 영문약어는 컬럼명의 기준이므로 MySQL 식별자 최대 길이를 따른다. */
    public static final int MAX_ABBREVIATION_LENGTH = 64;

    /** 영문약어 구분자. 단어 약어에는 '_'를 쓸 수 없으므로 조합 결과가 모호해지지 않는다. */
    private static final String ABBREVIATION_DELIMITER = "_";

    /** 용어 ID (AUTO_INCREMENT). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 용어명. 단어명을 이어 붙인 값. 예: 고객번호 */
    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    /** 영문약어(물리명). 단어 약어를 '_'로 이은 값. 컬럼 물리명 비교 기준. 예: CUST_NO */
    @Column(nullable = false, length = MAX_ABBREVIATION_LENGTH)
    private String abbreviation;

    /** 도메인. 타입/길이 기준. 지연 로딩이며 목록 조회에서는 fetch join한다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;

    // 목록 순서가 곧 조합 순서다. seq는 1부터 저장하고, 목록 조회 시 N+1을 막기 위해 배치로 가져온다.
    // - @ManyToMany: 같은 단어가 여러 용어에, 한 용어에 여러 번 들어갈 수 있다.
    // - @OrderColumn(seq): 리스트 인덱스를 seq 컬럼에 저장해 순서를 보존한다. (term_id, seq)가 PK라 중복 단어도 가능하다.
    // - @BatchSize: 여러 용어의 단어 목록을 IN 쿼리 한 번으로 가져온다. 컬렉션을 페이징 쿼리에서 fetch join하면
    //   Hibernate가 메모리에서 페이징하므로 fetch join 대신 이 방식을 쓴다.
    /** 구성 단어 (조합 순서대로, 1~10개). 기본은 지연 로딩이다. */
    @ManyToMany
    @JoinTable(
            name = "std_term_word",
            joinColumns = @JoinColumn(name = "term_id"),
            inverseJoinColumns = @JoinColumn(name = "word_id"))
    @OrderColumn(name = "seq")
    @ListIndexBase(1)
    @BatchSize(size = 100)
    private List<Word> words = new ArrayList<>();

    /** 설명 (선택). */
    @Column(length = 1000)
    private String description;

    /** JPA 전용 기본 생성자. */
    protected Term() {
    }

    /**
     * 새 용어를 만든다. 용어명과 영문약어는 단어 목록으로부터 계산된다.
     *
     * @param words       구성 단어 (조합 순서대로)
     * @param domain      도메인
     * @param description 설명 (null 가능)
     */
    public Term(List<Word> words, Domain domain, String description) {
        changeWords(words);
        this.domain = domain;
        this.description = description;
    }

    /** 단어명을 이어 붙인다 (고객+번호 → 고객번호). 중복 검사 전에도 같은 규칙을 적용해야 한다. */
    public static String composeName(List<Word> words) {
        return words.stream().map(Word::getName).collect(joining());
    }

    /** 영문약어를 `_`로 잇는다 (CUST+NO → CUST_NO). 중복·길이 검사 전에도 같은 규칙을 적용해야 한다. */
    public static String composeAbbreviation(List<Word> words) {
        return words.stream().map(Word::getAbbreviation).collect(joining(ABBREVIATION_DELIMITER));
    }

    /**
     * 구성 단어, 도메인, 설명을 교체한다. 단어가 바뀌면 용어명/영문약어도 다시 계산된다.
     * 이 용어에 매핑된 컬럼은 다음 조회부터 새 영문약어 기준으로 검출된다.
     */
    public void update(List<Word> words, Domain domain, String description) {
        changeWords(words);
        this.domain = domain;
        this.description = description;
    }

    /**
     * 단어 목록을 바꾸고 이름/약어를 다시 계산한다.
     * 컬렉션 참조를 새 리스트로 바꾸지 않고 {@code clear()} 후 {@code addAll()}한다.
     * Hibernate가 관리하는 영속 컬렉션(PersistentList)을 그대로 유지해야 변경이 조인 테이블에 반영된다.
     */
    private void changeWords(List<Word> words) {
        this.words.clear();
        this.words.addAll(words);
        this.name = composeName(words);
        this.abbreviation = composeAbbreviation(words);
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public Domain getDomain() {
        return domain;
    }

    /** 외부에서 목록을 직접 수정해 이름/약어와 어긋나지 않도록 읽기 전용 뷰를 돌려준다. */
    public List<Word> getWords() {
        return Collections.unmodifiableList(words);
    }

    public String getDescription() {
        return description;
    }
}
