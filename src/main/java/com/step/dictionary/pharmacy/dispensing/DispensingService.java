package com.step.dictionary.pharmacy.dispensing;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.pharmacy.PharmacyItems;
import com.step.dictionary.pharmacy.dispensing.dto.DispensingItemRequest;
import com.step.dictionary.pharmacy.dispensing.dto.DispensingRequest;
import com.step.dictionary.pharmacy.dispensing.dto.DispensingResponse;
import com.step.dictionary.pharmacy.dispensing.dto.DispensingSearchCondition;
import com.step.dictionary.pharmacy.dispensing.dto.DispensingSummaryResponse;
import com.step.dictionary.pharmacy.prescription.Prescription;
import com.step.dictionary.pharmacy.prescription.PrescriptionItem;
import com.step.dictionary.pharmacy.prescription.PrescriptionService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 조제 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>취소된 처방은 조제할 수 없다 ({@code PRESCRIPTION_CANCELLED}).</li>
 *   <li>처방에 검수 불합격(REJECTED)이 아닌 조제가 이미 있으면 다시 조제할 수 없다 ({@code DISPENSING_ALREADY_EXISTS}).
 *       불합격이면 같은 처방으로 다시 조제한다.</li>
 *   <li>조제 품목은 처방 품목을 빠짐없이 한 번씩 다뤄야 한다 ({@code INVALID_DISPENSING_ITEMS}).</li>
 *   <li>조제하면 처방은 DISPENSED가 되어 더는 취소할 수 없다.</li>
 *   <li>조제자는 로그인 사용자, 조제 일시는 등록 시각이다.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class DispensingService {

    private final DispensingRepository dispensingRepository;
    private final PrescriptionService prescriptionService;

    public DispensingService(DispensingRepository dispensingRepository, PrescriptionService prescriptionService) {
        this.dispensingRepository = dispensingRepository;
        this.prescriptionService = prescriptionService;
    }

    /**
     * 처방을 조제한다.
     *
     * @param username 조제자 (로그인 아이디)
     * @throws BusinessException PRESCRIPTION_NOT_FOUND (404), PRESCRIPTION_CANCELLED (409),
     *                           DISPENSING_ALREADY_EXISTS (409), INVALID_DISPENSING_ITEMS (400)
     */
    @Transactional
    public DispensingResponse create(DispensingRequest request, String username) {
        Prescription prescription = prescriptionService.getPrescription(request.prescriptionId());
        if (prescription.isCancelled()) {
            throw new BusinessException(ErrorCode.PRESCRIPTION_CANCELLED);
        }
        if (dispensingRepository.existsByPrescriptionIdAndStatusNot(prescription.getId(),
                DispensingStatus.REJECTED)) {
            throw new BusinessException(ErrorCode.DISPENSING_ALREADY_EXISTS);
        }

        Map<Long, PrescriptionItem> prescriptionItems = prescription.getItems().stream()
                .collect(Collectors.toMap(PrescriptionItem::getId, Function.identity()));
        PharmacyItems.requireExactly(
                request.items().stream().map(DispensingItemRequest::prescriptionItemId).toList(),
                prescriptionItems.keySet(), ErrorCode.INVALID_DISPENSING_ITEMS);

        Dispensing dispensing = new Dispensing(prescription, username, LocalDateTime.now(), request.remark());
        // 처방 품목 순서대로 저장해 조제 품목 ID 순 = 처방 순번 순이 되게 한다.
        Map<Long, DispensingItemRequest> byItemId = request.items().stream()
                .collect(Collectors.toMap(DispensingItemRequest::prescriptionItemId, Function.identity()));
        for (PrescriptionItem prescriptionItem : prescription.getItems()) {
            DispensingItemRequest item = byItemId.get(prescriptionItem.getId());
            dispensing.addItem(prescriptionItem, item.quantity(), item.lotNo(), item.expiryDate());
        }
        prescriptionService.markDispensed(prescription);
        return DispensingResponse.from(dispensingRepository.save(dispensing));
    }

    /**
     * 조제를 품목과 함께 조회한다.
     *
     * @throws BusinessException DISPENSING_NOT_FOUND (404)
     */
    public DispensingResponse findById(Long id) {
        return DispensingResponse.from(getDispensing(id));
    }

    /** 조제를 검색한다 (품목 제외, 조제 일시 최신순). */
    public PageResponse<DispensingSummaryResponse> search(DispensingSearchCondition condition, Pageable pageable) {
        return PageResponse.from(dispensingRepository.search(condition, pageable)
                .map(DispensingSummaryResponse::from));
    }

    /** ID로 조제를 찾고, 없으면 DISPENSING_NOT_FOUND(404)를 던진다. 검수·불출 서비스도 쓴다. */
    /** 검수 결과를 조제 상태에 반영한다 (합격 INSPECTED, 불합격 REJECTED). {@code InspectionService}가 호출한다. */
    @Transactional
    public void applyInspection(Dispensing dispensing, boolean passed) {
        dispensing.applyInspection(passed);
        dispensingRepository.save(dispensing);
    }

    /** 불출된 조제를 ISSUED로 바꾼다. {@code IssueService}가 호출한다. */
    @Transactional
    public void markIssued(Dispensing dispensing) {
        dispensing.markIssued();
        dispensingRepository.save(dispensing);
    }

    public Dispensing getDispensing(Long id) {
        return dispensingRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DISPENSING_NOT_FOUND));
    }
}
