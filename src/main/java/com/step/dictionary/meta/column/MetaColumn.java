package com.step.dictionary.meta.column;

import com.step.dictionary.global.entity.BaseTimeEntity;
import com.step.dictionary.meta.PhysicalNames;
import com.step.dictionary.meta.table.MetaTable;
import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.term.Term;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.YesNoConverter;

/**
 * 컬럼 정의. 표준 용어에 매핑하고, 매핑된 용어와 도메인을 기준으로 표준 준수 여부를 점검한다.
 *
 * <p>테이블: {@code meta_column} (V4 마이그레이션). 물리명은 대문자로 저장하며 테이블 안에서 유일하다.
 *
 * <p>참조 관계:
 * <ul>
 *   <li>소속 테이블({@link MetaTable})은 필수이며 생성 후 바뀌지 않는다.</li>
 *   <li>표준 용어({@link Term})는 선택이다. 없으면 {@link ColumnIssue#NO_TERM}으로 검출된다.</li>
 * </ul>
 */
@Entity
@Table(name = "meta_column")
public class MetaColumn extends BaseTimeEntity {

    /** 컬럼 정의 ID (AUTO_INCREMENT). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 테이블. update()로 바꿀 수 없다 (다른 테이블로 옮기려면 삭제 후 등록). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private MetaTable table;

    /** 물리명(대문자). 용어 영문약어와 같아야 표준이다. 예: CUST_NO */
    @Column(nullable = false, length = PhysicalNames.MAX_LENGTH)
    private String physicalName;

    /** 논리명(한글명). 예: 고객번호. 검출 대상이 아니다. */
    @Column(nullable = false, length = 200)
    private String logicalName;

    /** 매핑된 표준 용어 (선택). 지연 로딩이며 조회 쿼리에서 용어와 도메인을 함께 가져온다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;

    // MySQL 방언은 STRING enum을 ENUM 타입으로 보므로 VARCHAR 컬럼과 validate가 어긋나지 않게 지정한다.
    /** 데이터 타입. 용어 도메인의 타입과 같아야 표준이다. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private DataType dataType;

    /** 길이. 길이를 쓰지 않는 타입이면 null. */
    private Integer dataLength;

    /** 소수점 자리수. DECIMAL은 생략 시 0, 나머지 타입은 null. */
    private Integer dataScale;

    /** NULL 허용 여부 ('Y'/'N'). 검출 대상이 아니다. */
    @Convert(converter = YesNoConverter.class)
    @Column(name = "nullable_yn", nullable = false, length = 1)
    private boolean nullable;

    /** 기본키 여부 ('Y'/'N'). 기본키면 nullable은 false여야 한다 (서비스에서 검증). */
    @Convert(converter = YesNoConverter.class)
    @Column(name = "primary_key_yn", nullable = false, length = 1)
    private boolean primaryKey;

    /** 테이블 내 컬럼 순서 (1 이상). 유일하지 않으며 같으면 id 순으로 정렬한다. */
    @Column(nullable = false)
    private int seq;

    /** JPA 전용 기본 생성자. */
    protected MetaColumn() {
    }

    /**
     * 새 컬럼 정의를 만든다. 소속 테이블 외 값은 {@link #update}와 같은 규칙으로 설정한다.
     * 타입/길이 조합과 기본키 NULL 규칙은 호출 전에 서비스에서 검증돼 있어야 한다.
     *
     * @param table        소속 테이블
     * @param physicalName 물리명 (대문자로 정규화)
     * @param logicalName  논리명
     * @param term         매핑 용어 (null 가능)
     * @param dataType     데이터 타입
     * @param dataLength   길이 (null 가능)
     * @param dataScale    소수점 자리수 (null 가능, DECIMAL이면 0으로 정규화)
     * @param nullable     NULL 허용 여부
     * @param primaryKey   기본키 여부
     * @param seq          컬럼 순서
     */
    public MetaColumn(MetaTable table, String physicalName, String logicalName, Term term, DataType dataType,
            Integer dataLength, Integer dataScale, boolean nullable, boolean primaryKey, int seq) {
        this.table = table;
        update(physicalName, logicalName, term, dataType, dataLength, dataScale, nullable, primaryKey, seq);
    }

    /** 소속 테이블을 제외한 모든 값을 교체한다 (PUT 의미). 물리명 중복 검사는 호출 전에 서비스에서 끝내야 한다. */
    public void update(String physicalName, String logicalName, Term term, DataType dataType,
            Integer dataLength, Integer dataScale, boolean nullable, boolean primaryKey, int seq) {
        this.physicalName = PhysicalNames.normalize(physicalName);
        this.logicalName = logicalName;
        this.term = term;
        this.dataType = dataType;
        this.dataLength = dataLength;
        // 도메인과 같은 규칙으로 정규화해야 DECIMAL(15)와 DECIMAL(15,0)을 같다고 본다.
        this.dataScale = dataType.normalizeScale(dataScale);
        this.nullable = nullable;
        this.primaryKey = primaryKey;
        this.seq = seq;
    }

    /**
     * 비표준 사유를 돌려준다. 비어 있으면 표준 컬럼이다.
     * {@link MetaColumnRepositoryCustomImpl}의 검색 조건과 같은 규칙이어야 한다.
     *
     * <p>매핑된 용어와 그 도메인을 읽으므로 트랜잭션 안에서 호출하거나 둘을 미리 fetch해 둬야 한다.
     *
     * @return 사유 목록. 용어가 없으면 {@code [NO_TERM]}만, 있으면 해당하는 사유를 순서대로 담는다
     */
    public List<ColumnIssue> inspect() {
        // 용어가 없으면 비교 기준이 없으므로 다른 항목은 검사하지 않는다.
        if (term == null) {
            return List.of(ColumnIssue.NO_TERM);
        }
        List<ColumnIssue> issues = new ArrayList<>();
        Domain domain = term.getDomain();
        // 물리명: 둘 다 대문자로 저장돼 있으므로 그대로 비교한다.
        if (!physicalName.equals(term.getAbbreviation())) {
            issues.add(ColumnIssue.NAME_MISMATCH);
        }
        // 타입: enum이므로 참조 비교(!=)로 충분하다.
        if (dataType != domain.getDataType()) {
            issues.add(ColumnIssue.TYPE_MISMATCH);
        }
        // 길이/소수점: Objects.equals는 null끼리를 같다고 본다. 검색 쿼리의 NULL 안전 비교와 같은 의미다.
        if (!Objects.equals(dataLength, domain.getDataLength()) || !Objects.equals(dataScale, domain.getDataScale())) {
            issues.add(ColumnIssue.LENGTH_MISMATCH);
        }
        return issues;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public MetaTable getTable() {
        return table;
    }

    public String getPhysicalName() {
        return physicalName;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public Term getTerm() {
        return term;
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

    public boolean isNullable() {
        return nullable;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public int getSeq() {
        return seq;
    }
}
