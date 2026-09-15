package com.step.dictionary.meta.table;

import com.step.dictionary.global.entity.BaseTimeEntity;
import com.step.dictionary.meta.PhysicalNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 테이블 정의. 실제 테이블을 등록하고 그 컬럼이 표준을 지키는지 점검하는 단위다.
 *
 * <p>테이블: {@code meta_table} (V4 마이그레이션). 물리명은 대문자로 저장하며 전체에서 유일하다.
 * 컬럼({@code MetaColumn})이 이 테이블을 참조하지만 양방향 컬렉션은 두지 않는다.
 * 테이블 삭제 시 컬럼 일괄 삭제는 {@link MetaTableService#delete}가 처리한다.
 */
@Entity
@Table(name = "meta_table")
public class MetaTable extends BaseTimeEntity {

    /** 테이블 정의 ID (AUTO_INCREMENT). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 물리명(대문자). 예: TB_CUSTOMER */
    @Column(nullable = false, length = PhysicalNames.MAX_LENGTH)
    private String physicalName;

    /** 논리명(한글명). 예: 고객 */
    @Column(nullable = false, length = 200)
    private String logicalName;

    /** 설명 (선택). */
    @Column(length = 1000)
    private String description;

    /** JPA 전용 기본 생성자. */
    protected MetaTable() {
    }

    /**
     * 새 테이블 정의를 만든다. 물리명은 대문자로 정규화한다.
     *
     * @param physicalName 물리명 (형식 검증은 요청 DTO에서 끝난 값)
     * @param logicalName  논리명
     * @param description  설명 (null 가능)
     */
    public MetaTable(String physicalName, String logicalName, String description) {
        this.physicalName = PhysicalNames.normalize(physicalName);
        this.logicalName = logicalName;
        this.description = description;
    }

    /** 모든 값을 교체한다 (PUT 의미). 물리명 중복 검사는 호출 전에 서비스에서 끝내야 한다. */
    public void update(String physicalName, String logicalName, String description) {
        this.physicalName = PhysicalNames.normalize(physicalName);
        this.logicalName = logicalName;
        this.description = description;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public String getPhysicalName() {
        return physicalName;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public String getDescription() {
        return description;
    }
}
