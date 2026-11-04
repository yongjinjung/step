package com.step.dictionary.code.group;

import com.step.dictionary.global.entity.BaseTimeEntity;
import java.util.Locale;

/**
 * 공통 코드 그룹. 코드ID 하나에 코드값({@code CodeValue}) 여러 개가 속한다 (예: ORDER_STAT 주문상태 → 01 접수, 02 결제완료).
 *
 * <p>테이블: {@code com_code_group} (V8 마이그레이션). 코드ID는 대문자로 저장하며 전체에서 유일하다.
 *
 * <p>참조 관계:
 * <ul>
 *   <li>코드값({@code CodeValue.group})이 소속 그룹으로 참조한다. 그룹을 삭제하면 코드값도 함께 삭제한다.</li>
 *   <li>컬럼 정의({@code MetaColumn.codeGroup})가 코드성 컬럼의 코드 그룹으로 참조한다. 매핑된 그룹은 삭제할 수 없다.</li>
 * </ul>
 * 참조 검사는 {@link CodeGroupService}에서 한다.
 */
public class CodeGroup extends BaseTimeEntity {

    /** 코드 그룹 ID (AUTO_INCREMENT). 컬럼 매핑은 이 값으로 연결하므로 코드ID를 바꿔도 매핑이 유지된다. */
    private Long id;

    /** 코드ID(대문자). 예: ORDER_STAT */
    private String codeId;

    /** 코드명. 예: 주문상태 */
    private String codeName;

    /** 설명 (선택). */
    private String description;

    /**
     * 사용 여부 ('Y'/'N'). 미사용 그룹은 컬럼에 새로 매핑할 수 없다(기존 매핑은 유지).
     * DB는 CHAR(1) Y/N이고 YesNoTypeHandler가 boolean으로 바꾼다.
     */
    private boolean used;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected CodeGroup() {
    }

    /**
     * 새 코드 그룹을 만든다. 코드ID는 대문자로 정규화한다.
     *
     * @param codeId      코드ID (대소문자 무관, 저장 시 대문자)
     * @param codeName    코드명
     * @param description 설명 (null 가능)
     * @param used        사용 여부
     */
    public CodeGroup(String codeId, String codeName, String description, boolean used) {
        update(codeId, codeName, description, used);
    }

    /**
     * 코드ID는 대문자로 저장한다. 중복 검사 전에도 같은 규칙을 적용해야 한다.
     * Oracle은 문자열 비교에서 대소문자를 구분하므로, 정규화하지 않으면 cust와 CUST가 다른 값으로 들어간다.
     */
    public static String normalizeCodeId(String codeId) {
        // Locale.ROOT: 터키어 로케일 등에서 'i'가 'İ'로 바뀌는 문제를 막는다.
        return codeId.toUpperCase(Locale.ROOT);
    }

    /** 모든 값을 요청 값으로 교체한다 (PUT 의미). 중복 검사는 호출 전에 서비스에서 끝내야 한다. */
    public void update(String codeId, String codeName, String description, boolean used) {
        this.codeId = normalizeCodeId(codeId);
        this.codeName = codeName;
        this.description = description;
        this.used = used;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public String getCodeId() {
        return codeId;
    }

    public String getCodeName() {
        return codeName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isUsed() {
        return used;
    }
}
