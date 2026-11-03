package com.step.dictionary.pharmacy.inspection;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.pharmacy.PharmacyItems;
import com.step.dictionary.pharmacy.dispensing.Dispensing;
import com.step.dictionary.pharmacy.dispensing.DispensingItem;
import com.step.dictionary.pharmacy.dispensing.DispensingService;
import com.step.dictionary.pharmacy.inspection.dto.InspectionItemRequest;
import com.step.dictionary.pharmacy.inspection.dto.InspectionRequest;
import com.step.dictionary.pharmacy.inspection.dto.InspectionResponse;
import com.step.dictionary.pharmacy.inspection.dto.InspectionSearchCondition;
import com.step.dictionary.pharmacy.inspection.dto.InspectionSummaryResponse;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 검수 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>조제 상태(DISPENSED)인 조제만 검수할 수 있다 ({@code DISPENSING_NOT_INSPECTABLE}). 조제 하나에 검수 하나다.</li>
 *   <li>검수 품목은 조제 품목을 빠짐없이 한 번씩 다뤄야 한다 ({@code INVALID_INSPECTION_ITEMS}).</li>
 *   <li>불합격 품목은 불량 사유가 필요하다 ({@code DEFECT_REASON_REQUIRED}).</li>
 *   <li>모두 합격이면 조제가 INSPECTED(불출 가능), 하나라도 불합격이면 REJECTED(같은 처방으로 재조제)가 된다.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final DispensingService dispensingService;

    public InspectionService(InspectionRepository inspectionRepository, DispensingService dispensingService) {
        this.inspectionRepository = inspectionRepository;
        this.dispensingService = dispensingService;
    }

    /**
     * 조제를 검수한다.
     *
     * @param username 검수자 (로그인 아이디)
     * @throws BusinessException DISPENSING_NOT_FOUND (404), DISPENSING_NOT_INSPECTABLE (409),
     *                           INVALID_INSPECTION_ITEMS (400), DEFECT_REASON_REQUIRED (400)
     */
    @Transactional
    public InspectionResponse create(InspectionRequest request, String username) {
        Dispensing dispensing = dispensingService.getDispensing(request.dispensingId());
        if (!dispensing.isInspectable()) {
            throw new BusinessException(ErrorCode.DISPENSING_NOT_INSPECTABLE);
        }

        Map<Long, DispensingItem> dispensingItems = dispensing.getItems().stream()
                .collect(Collectors.toMap(DispensingItem::getId, Function.identity()));
        PharmacyItems.requireExactly(
                request.items().stream().map(InspectionItemRequest::dispensingItemId).toList(),
                dispensingItems.keySet(), ErrorCode.INVALID_INSPECTION_ITEMS);
        boolean missingReason = request.items().stream()
                .anyMatch(item -> item.result() == InspectionResult.FAIL && !StringUtils.hasText(item.defectReason()));
        if (missingReason) {
            throw new BusinessException(ErrorCode.DEFECT_REASON_REQUIRED);
        }

        Inspection inspection = new Inspection(dispensing, username, LocalDateTime.now(), request.remark());
        // 조제 품목 순서대로 저장한다.
        Map<Long, InspectionItemRequest> byItemId = request.items().stream()
                .collect(Collectors.toMap(InspectionItemRequest::dispensingItemId, Function.identity()));
        for (DispensingItem dispensingItem : dispensing.getItems()) {
            InspectionItemRequest item = byItemId.get(dispensingItem.getId());
            inspection.addItem(dispensingItem, item.result(), item.defectReason());
        }
        dispensingService.applyInspection(dispensing, inspection.isPassed());
        return InspectionResponse.from(inspectionRepository.save(inspection));
    }

    /**
     * 검수를 품목과 함께 조회한다.
     *
     * @throws BusinessException INSPECTION_NOT_FOUND (404)
     */
    public InspectionResponse findById(Long id) {
        return InspectionResponse.from(inspectionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_NOT_FOUND)));
    }

    /** 검수를 검색한다 (품목 제외, 검수 일시 최신순). */
    public PageResponse<InspectionSummaryResponse> search(InspectionSearchCondition condition, Pageable pageable) {
        return PageResponse.from(inspectionRepository.search(condition, pageable)
                .map(InspectionSummaryResponse::from));
    }
}
