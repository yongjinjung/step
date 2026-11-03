package com.step.dictionary.pharmacy.drugreturn;

import com.step.dictionary.pharmacy.Quantities;
import com.step.dictionary.pharmacy.issue.IssueItem;
import java.math.BigDecimal;

/**
 * 반납 품목 (반납의 하위). 불출 품목 하나를 얼마나 반납하고 어떻게 처리했는지 남긴다.
 *
 * <p>테이블: {@code phm_return_item} (V12). {@link DrugReturn#addItem}으로만 만든다.
 */
public class DrugReturnItem {

    private Long id;

    private DrugReturn drugReturn;

    private IssueItem issueItem;

    /** 반납 수량. 같은 불출 품목의 반납 합계는 불출 수량을 넘을 수 없다. */
    private BigDecimal quantity;

    private ReturnDisposition disposition;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected DrugReturnItem() {
    }

    DrugReturnItem(DrugReturn drugReturn, IssueItem issueItem, BigDecimal quantity, ReturnDisposition disposition) {
        this.drugReturn = drugReturn;
        this.issueItem = issueItem;
        this.quantity = Quantities.normalize(quantity);
        this.disposition = disposition;
    }

    /** 조회 후 소속 DrugReturn를 연결한다. */
    void attachTo(DrugReturn parent) {
        this.drugReturn = parent;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public DrugReturn getDrugReturn() {
        return drugReturn;
    }

    public IssueItem getIssueItem() {
        return issueItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public ReturnDisposition getDisposition() {
        return disposition;
    }
}
