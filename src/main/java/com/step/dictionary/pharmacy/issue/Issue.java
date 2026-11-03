package com.step.dictionary.pharmacy.issue;

import com.step.dictionary.global.entity.BaseTimeEntity;
import com.step.dictionary.pharmacy.dispensing.Dispensing;
import com.step.dictionary.pharmacy.dispensing.DispensingItem;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 불출 마스터. 검수 합격한 조제를 병동·부서·외래 창구로 내보낸 기록이며 불출 품목({@link IssueItem})을 가진다.
 *
 * <p>테이블: {@code phm_issue} (V12). 조제 하나에 불출 하나다(유일 제약).
 * 불출 품목은 조제 품목을 수량 그대로 옮긴다({@link #addItemFrom}).
 */
public class Issue extends BaseTimeEntity {

    private Long id;

    private Dispensing dispensing;

    /** 불출처 코드 (대문자). 예: W71 */
    private String destinationCode;

    private String destinationName;

    private String receiverName;

    /** 불출자 아이디 (로그인 사용자). */
    private String issuedBy;

    private LocalDateTime issuedAt;

    private String remark;

    private List<IssueItem> items = new ArrayList<>();

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected Issue() {
    }

    /** 새 불출을 만든다. 품목은 {@link #addItemFrom}으로 붙인다. */
    public Issue(Dispensing dispensing, String destinationCode, String destinationName, String receiverName,
            String issuedBy, LocalDateTime issuedAt, String remark) {
        this.dispensing = dispensing;
        this.destinationCode = normalizeDestinationCode(destinationCode);
        this.destinationName = destinationName;
        this.receiverName = receiverName;
        this.issuedBy = issuedBy;
        this.issuedAt = issuedAt;
        this.remark = remark;
    }

    /** 불출처 코드는 대문자로 저장한다. 검색 조건에도 같은 규칙을 적용한다. */
    public static String normalizeDestinationCode(String destinationCode) {
        return destinationCode.trim().toUpperCase(Locale.ROOT);
    }

    /** 조제 품목을 수량 그대로 불출 품목으로 옮긴다. */
    public IssueItem addItemFrom(DispensingItem dispensingItem) {
        IssueItem item = new IssueItem(this, dispensingItem, dispensingItem.getQuantity());
        items.add(item);
        return item;
    }

    /** 조회한 품목을 채운다. {@link IssueRepository}만 호출한다. */
    void loadItems(List<IssueItem> loaded) {
        loaded.forEach(item -> item.attachTo(this));
        this.items = new ArrayList<>(loaded);
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public Dispensing getDispensing() {
        return dispensing;
    }

    public String getDestinationCode() {
        return destinationCode;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public String getRemark() {
        return remark;
    }

    /** 품목 목록 (읽기 전용). */
    public List<IssueItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
