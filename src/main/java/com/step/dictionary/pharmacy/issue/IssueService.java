package com.step.dictionary.pharmacy.issue;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.pharmacy.dispensing.Dispensing;
import com.step.dictionary.pharmacy.dispensing.DispensingItem;
import com.step.dictionary.pharmacy.dispensing.DispensingService;
import com.step.dictionary.pharmacy.drugreturn.DrugReturnRepository;
import com.step.dictionary.pharmacy.issue.dto.IssueRequest;
import com.step.dictionary.pharmacy.issue.dto.IssueResponse;
import com.step.dictionary.pharmacy.issue.dto.IssueSearchCondition;
import com.step.dictionary.pharmacy.issue.dto.IssueSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 불출 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>검수 합격(INSPECTED)한 조제만 불출할 수 있다 ({@code DISPENSING_NOT_ISSUABLE}). 조제 하나에 불출 하나다.</li>
 *   <li>불출 품목은 조제 품목을 수량 그대로 옮긴다 (요청으로 받지 않는다).</li>
 *   <li>불출하면 조제는 ISSUED가 된다.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class IssueService {

    private final IssueRepository issueRepository;
    private final DispensingService dispensingService;
    /** 불출 상세에 품목별 반납 수량을 보여주기 위해 쓴다. */
    private final DrugReturnRepository returnRepository;

    public IssueService(IssueRepository issueRepository, DispensingService dispensingService,
            DrugReturnRepository returnRepository) {
        this.issueRepository = issueRepository;
        this.dispensingService = dispensingService;
        this.returnRepository = returnRepository;
    }

    /**
     * 조제를 불출한다.
     *
     * @param username 불출자 (로그인 아이디)
     * @throws BusinessException DISPENSING_NOT_FOUND (404), DISPENSING_NOT_ISSUABLE (409)
     */
    @Transactional
    public IssueResponse create(IssueRequest request, String username) {
        Dispensing dispensing = dispensingService.getDispensing(request.dispensingId());
        if (!dispensing.isIssuable()) {
            throw new BusinessException(ErrorCode.DISPENSING_NOT_ISSUABLE);
        }

        Issue issue = new Issue(dispensing, request.destinationCode(), request.destinationName(),
                request.receiverName(), username, LocalDateTime.now(), request.remark());
        for (DispensingItem dispensingItem : dispensing.getItems()) {
            issue.addItemFrom(dispensingItem);
        }
        dispensingService.markIssued(dispensing);
        // 방금 만든 불출이라 반납이 없다.
        return IssueResponse.from(issueRepository.save(issue), Map.of());
    }

    /**
     * 불출을 품목·반납 수량과 함께 조회한다.
     *
     * @throws BusinessException ISSUE_NOT_FOUND (404)
     */
    public IssueResponse findById(Long id) {
        Issue issue = getIssue(id);
        return IssueResponse.from(issue, returnedQuantities(id));
    }

    /** 불출을 검색한다 (품목 제외, 불출 일시 최신순). */
    public PageResponse<IssueSummaryResponse> search(IssueSearchCondition condition, Pageable pageable) {
        return PageResponse.from(issueRepository.search(condition, pageable).map(IssueSummaryResponse::from));
    }

    /** ID로 불출을 찾고, 없으면 ISSUE_NOT_FOUND(404)를 던진다. 반납 서비스도 쓴다. */
    public Issue getIssue(Long id) {
        return issueRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ISSUE_NOT_FOUND));
    }

    /** 불출 품목 ID → 지금까지 반납한 수량 합계. 반납이 없는 품목은 빠진다. 반납 서비스도 쓴다. */
    public Map<Long, BigDecimal> returnedQuantities(Long issueId) {
        return returnRepository.sumQuantityByIssueItem(issueId);
    }
}
