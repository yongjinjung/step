package com.step.dictionary.pharmacy.drugreturn;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.pharmacy.PharmacyItems;
import com.step.dictionary.pharmacy.Quantities;
import com.step.dictionary.pharmacy.drugreturn.dto.DrugReturnItemRequest;
import com.step.dictionary.pharmacy.drugreturn.dto.DrugReturnRequest;
import com.step.dictionary.pharmacy.drugreturn.dto.DrugReturnResponse;
import com.step.dictionary.pharmacy.drugreturn.dto.DrugReturnSearchCondition;
import com.step.dictionary.pharmacy.drugreturn.dto.DrugReturnSummaryResponse;
import com.step.dictionary.pharmacy.issue.Issue;
import com.step.dictionary.pharmacy.issue.IssueItem;
import com.step.dictionary.pharmacy.issue.IssueService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 반납 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>불출 하나에 여러 번 반납할 수 있고, 불출 품목 중 일부만 반납해도 된다.</li>
 *   <li>반납 품목은 그 불출의 품목이어야 하며 같은 품목을 한 요청에 두 번 넣을 수 없다 ({@code INVALID_RETURN_ITEMS}).</li>
 *   <li>품목별 반납 수량 합계(이전 반납 + 이번 반납)는 불출 수량을 넘을 수 없다 ({@code RETURN_QUANTITY_EXCEEDED}).</li>
 * </ul>
 * 동시에 같은 품목을 반납하면 두 요청이 모두 한도 검사를 통과할 수 있다. 화면에서 한 사람이 처리하는 업무라
 * 잠금은 두지 않았다(필요하면 불출 행을 비관적 잠금으로 읽는다).
 */
@Service
@Transactional(readOnly = true)
public class DrugReturnService {

    private final DrugReturnRepository returnRepository;
    private final IssueService issueService;

    public DrugReturnService(DrugReturnRepository returnRepository, IssueService issueService) {
        this.returnRepository = returnRepository;
        this.issueService = issueService;
    }

    /**
     * 불출된 약을 반납한다.
     *
     * @param username 반납자 (로그인 아이디)
     * @throws BusinessException ISSUE_NOT_FOUND (404), INVALID_RETURN_ITEMS (400), RETURN_QUANTITY_EXCEEDED (409)
     */
    @Transactional
    public DrugReturnResponse create(DrugReturnRequest request, String username) {
        Issue issue = issueService.getIssue(request.issueId());
        Map<Long, IssueItem> issueItems = issue.getItems().stream()
                .collect(Collectors.toMap(IssueItem::getId, Function.identity()));
        PharmacyItems.requireSubset(
                request.items().stream().map(DrugReturnItemRequest::issueItemId).toList(),
                issueItems.keySet(), ErrorCode.INVALID_RETURN_ITEMS);

        Map<Long, BigDecimal> returned = issueService.returnedQuantities(issue.getId());
        for (DrugReturnItemRequest item : request.items()) {
            BigDecimal total = returned.getOrDefault(item.issueItemId(), Quantities.ZERO).add(item.quantity());
            if (total.compareTo(issueItems.get(item.issueItemId()).getQuantity()) > 0) {
                throw new BusinessException(ErrorCode.RETURN_QUANTITY_EXCEEDED);
            }
        }

        DrugReturn drugReturn = new DrugReturn(issue, request.reason(), username, LocalDateTime.now(),
                request.remark());
        // 불출 품목 순서대로 저장한다 (요청에 있는 품목만).
        Map<Long, DrugReturnItemRequest> byItemId = request.items().stream()
                .collect(Collectors.toMap(DrugReturnItemRequest::issueItemId, Function.identity()));
        for (IssueItem issueItem : issue.getItems()) {
            DrugReturnItemRequest item = byItemId.get(issueItem.getId());
            if (item != null) {
                drugReturn.addItem(issueItem, item.quantity(), item.disposition());
            }
        }
        return DrugReturnResponse.from(returnRepository.save(drugReturn));
    }

    /**
     * 반납을 품목과 함께 조회한다.
     *
     * @throws BusinessException RETURN_NOT_FOUND (404)
     */
    public DrugReturnResponse findById(Long id) {
        return DrugReturnResponse.from(returnRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_NOT_FOUND)));
    }

    /** 반납을 검색한다 (품목 제외, 반납 일시 최신순). */
    public PageResponse<DrugReturnSummaryResponse> search(DrugReturnSearchCondition condition, Pageable pageable) {
        return PageResponse.from(returnRepository.search(condition, pageable).map(DrugReturnSummaryResponse::from));
    }
}
