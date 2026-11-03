package com.step.dictionary.pharmacy.drugreturn;

import com.step.dictionary.global.entity.BaseTimeEntity;
import com.step.dictionary.pharmacy.issue.Issue;
import com.step.dictionary.pharmacy.issue.IssueItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 반납 마스터. 불출된 약을 되돌려 받은 기록이며 반납 품목({@link DrugReturnItem})을 가진다.
 *
 * <p>테이블: {@code phm_return} (V12). 불출 하나에 여러 번 반납할 수 있고, 불출 품목 일부만 반납해도 된다.
 * 클래스 이름이 {@code Return}이 아닌 이유: {@code return}은 자바 예약어라 패키지·변수 이름으로 쓸 수 없어
 * 헷갈리지 않게 {@code DrugReturn}으로 통일했다.
 */
public class DrugReturn extends BaseTimeEntity {

    private Long id;

    private Issue issue;

    /** 반납 사유. 예: 퇴원, 처방 중단 */
    private String reason;

    /** 반납자 아이디 (로그인 사용자). */
    private String returnedBy;

    private LocalDateTime returnedAt;

    private String remark;

    private List<DrugReturnItem> items = new ArrayList<>();

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected DrugReturn() {
    }

    public DrugReturn(Issue issue, String reason, String returnedBy, LocalDateTime returnedAt, String remark) {
        this.issue = issue;
        this.reason = reason;
        this.returnedBy = returnedBy;
        this.returnedAt = returnedAt;
        this.remark = remark;
    }

    /** 품목을 붙인다. 수량 한도는 서비스가 먼저 확인한다. */
    public DrugReturnItem addItem(IssueItem issueItem, BigDecimal quantity, ReturnDisposition disposition) {
        DrugReturnItem item = new DrugReturnItem(this, issueItem, quantity, disposition);
        items.add(item);
        return item;
    }

    /** 조회한 품목을 채운다. {@link DrugReturnRepository}만 호출한다. */
    void loadItems(List<DrugReturnItem> loaded) {
        loaded.forEach(item -> item.attachTo(this));
        this.items = new ArrayList<>(loaded);
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public Issue getIssue() {
        return issue;
    }

    public String getReason() {
        return reason;
    }

    public String getReturnedBy() {
        return returnedBy;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public String getRemark() {
        return remark;
    }

    /** 품목 목록 (읽기 전용). */
    public List<DrugReturnItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
