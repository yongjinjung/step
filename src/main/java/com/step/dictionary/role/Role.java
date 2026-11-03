package com.step.dictionary.role;

import com.step.dictionary.global.entity.BaseTimeEntity;
import java.util.Locale;

/**
 * 역할. 메뉴별 권한({@link RoleMenu})을 묶어 사용자에게 부여한다.
 *
 * <p>테이블: {@code app_role} (V11 마이그레이션). 코드는 대문자로 저장하며 전체에서 유일하다.
 * 한 사용자는 역할을 여러 개 가질 수 있고, 권한은 역할들의 합집합이다.
 *
 * <p>전권({@link #superAdmin}) 역할은 메뉴 권한과 관계없이 모든 요청을 통과시킨다.
 * 권한 설정 실수로 아무도 시스템을 관리할 수 없게 되는 상황을 막는 안전장치이며, 기본 제공되는 ADMIN 역할이 이에 해당한다.
 *
 * <p>기본 제공 역할({@link #builtIn})은 삭제할 수 없고 코드와 전권 여부를 바꿀 수 없다. 이름·설명·사용 여부·권한은 바꿀 수 있다.
 */
public class Role extends BaseTimeEntity {

    /** 관리자 역할 코드. 최초 관리자 계정과 마지막 전권 사용자 보호에 쓴다. */
    public static final String ADMIN_CODE = "ADMIN";
    /** 일반 사용자 역할 코드. 기본 권한을 채울 때 쓴다 ({@link RoleInitializer}). */
    public static final String USER_CODE = "USER";

    /** 역할 ID (AUTO_INCREMENT). */
    private Long id;

    /** 역할 코드(대문자). JWT {@code roles} 클레임에 들어간다. 예: ADMIN */
    private String code;

    /** 역할명. 예: 관리자 */
    private String name;

    /** 설명 (선택). */
    private String description;

    /** 전권 여부 ('Y'/'N'). 메뉴 권한과 무관하게 모두 허용한다. */
    private boolean superAdmin;

    /** 사용 여부 ('Y'/'N'). 미사용 역할의 권한은 무시한다. */
    private boolean used;

    /** 기본 제공 역할 여부 ('Y'/'N'). 삭제·코드 변경·전권 변경이 막힌다. */
    private boolean builtIn;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected Role() {
    }

    /**
     * 새 역할을 만든다. 코드는 대문자로 정규화한다.
     *
     * @param code        역할 코드 (대소문자 무관, 저장 시 대문자)
     * @param name        역할명
     * @param description 설명 (null 가능)
     * @param superAdmin  전권 여부
     * @param used        사용 여부
     * @param builtIn     기본 제공 역할 여부 (초기 데이터만 true)
     */
    public Role(String code, String name, String description, boolean superAdmin, boolean used, boolean builtIn) {
        this.code = normalizeCode(code);
        this.name = name;
        this.description = description;
        this.superAdmin = superAdmin;
        this.used = used;
        this.builtIn = builtIn;
    }

    /** 역할 코드는 대문자로 저장한다. 중복 검사 전에도 같은 규칙을 적용해야 한다. */
    public static String normalizeCode(String code) {
        return code.toUpperCase(Locale.ROOT);
    }

    /**
     * 역할 정보를 바꾼다. 기본 제공 역할이면 코드와 전권 여부는 그대로 둔다(서비스에서 같은 값을 넘기도록 검사한다).
     */
    public void update(String code, String name, String description, boolean superAdmin, boolean used) {
        this.code = normalizeCode(code);
        this.name = name;
        this.description = description;
        this.superAdmin = superAdmin;
        this.used = used;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public boolean isUsed() {
        return used;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }
}
