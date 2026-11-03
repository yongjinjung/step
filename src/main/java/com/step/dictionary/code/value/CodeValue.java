package com.step.dictionary.code.value;

import com.step.dictionary.code.group.CodeGroup;
import com.step.dictionary.global.entity.BaseTimeEntity;
import java.util.Locale;

/**
 * 공통 코드값. 코드 그룹에 속하는 실제 값이다 (예: ORDER_STAT 그룹의 01 접수).
 *
 * <p>테이블: {@code com_code_value} (V8 마이그레이션). 코드값은 대문자로 저장하며 그룹 안에서 유일하다.
 * 소속 그룹은 필수이며 생성 후 바뀌지 않는다 (다른 그룹으로 옮기려면 삭제 후 등록).
 *
 * <p>필드 이름을 {@code value}/{@code name} 대신 {@code codeValue}/{@code valueName}으로 둔 이유:
 * {@code value}는 H2와 HQL의 예약어라 쿼리에서 충돌할 수 있다.
 */
public class CodeValue extends BaseTimeEntity {

    /** 코드값 ID (AUTO_INCREMENT). */
    private Long id;

    /** 소속 코드 그룹. update()로 바꿀 수 없다. */
    private CodeGroup group;

    /** 코드값(대문자). 예: 01, Y, ACTIVE */
    private String codeValue;

    /** 코드값명. 예: 접수 */
    private String valueName;

    /** 설명 (선택). */
    private String description;

    /** 그룹 내 정렬 순서 (1 이상). 유일하지 않으며 같으면 id 순으로 정렬한다. */
    private int sortOrder;

    /** 사용 여부 ('Y'/'N'). */
    private boolean used;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected CodeValue() {
    }

    /**
     * 새 코드값을 만든다. 소속 그룹 외 값은 {@link #update}와 같은 규칙으로 설정한다.
     *
     * @param group       소속 코드 그룹
     * @param codeValue   코드값 (저장 시 대문자)
     * @param valueName   코드값명
     * @param description 설명 (null 가능)
     * @param sortOrder   정렬 순서
     * @param used        사용 여부
     */
    public CodeValue(CodeGroup group, String codeValue, String valueName, String description, int sortOrder,
            boolean used) {
        this.group = group;
        update(codeValue, valueName, description, sortOrder, used);
    }

    /** 코드값은 대문자로 저장한다. 중복 검사 전에도 같은 규칙을 적용해야 한다 (코드ID와 같은 이유). */
    public static String normalizeCodeValue(String codeValue) {
        return codeValue.toUpperCase(Locale.ROOT);
    }

    /** 소속 그룹을 제외한 모든 값을 교체한다 (PUT 의미). 중복 검사는 호출 전에 서비스에서 끝내야 한다. */
    public void update(String codeValue, String valueName, String description, int sortOrder, boolean used) {
        this.codeValue = normalizeCodeValue(codeValue);
        this.valueName = valueName;
        this.description = description;
        this.sortOrder = sortOrder;
        this.used = used;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public CodeGroup getGroup() {
        return group;
    }

    public String getCodeValue() {
        return codeValue;
    }

    public String getValueName() {
        return valueName;
    }

    public String getDescription() {
        return description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isUsed() {
        return used;
    }
}
