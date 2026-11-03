package com.step.dictionary.pharmacy.issue;

import com.step.dictionary.pharmacy.dispensing.DispensingItem;
import java.math.BigDecimal;

/**
 * 불출 품목 (불출의 하위). 조제 품목 하나를 얼마나 내보냈는지 남긴다. 반납은 이 품목을 기준으로 한다.
 *
 * <p>테이블: {@code phm_issue_item} (V12). {@link Issue#addItemFrom}으로만 만든다.
 */
public class IssueItem {

    private Long id;

    private Issue issue;

    private DispensingItem dispensingItem;

    /** 불출 수량 (= 조제 수량). 반납 수량 합계의 상한이다. */
    private BigDecimal quantity;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected IssueItem() {
    }

    IssueItem(Issue issue, DispensingItem dispensingItem, BigDecimal quantity) {
        this.issue = issue;
        this.dispensingItem = dispensingItem;
        this.quantity = quantity;
    }

    /** 조회 후 소속 Issue를 연결한다. */
    void attachTo(Issue parent) {
        this.issue = parent;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public Issue getIssue() {
        return issue;
    }

    public DispensingItem getDispensingItem() {
        return dispensingItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
