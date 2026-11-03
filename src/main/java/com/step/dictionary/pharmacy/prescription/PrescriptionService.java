package com.step.dictionary.pharmacy.prescription;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.pharmacy.prescription.dto.PrescriptionCancelRequest;
import com.step.dictionary.pharmacy.prescription.dto.PrescriptionItemRequest;
import com.step.dictionary.pharmacy.prescription.dto.PrescriptionRequest;
import com.step.dictionary.pharmacy.prescription.dto.PrescriptionResponse;
import com.step.dictionary.pharmacy.prescription.dto.PrescriptionSearchCondition;
import com.step.dictionary.pharmacy.prescription.dto.PrescriptionSummaryResponse;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 처방 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>처방전번호는 대문자로 정규화한 뒤 전체에서 유일해야 한다 ({@code DUPLICATE_PRESCRIPTION_NO}).</li>
 *   <li>품목 순번은 요청 순서대로 1부터 매긴다.</li>
 *   <li>처방은 수정하지 않는다. 조제 전(ORDERED)에만 취소할 수 있다 ({@code PRESCRIPTION_NOT_CANCELLABLE}).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    /**
     * 처방을 품목과 함께 등록한다.
     *
     * @throws BusinessException DUPLICATE_PRESCRIPTION_NO (409)
     */
    @Transactional
    public PrescriptionResponse create(PrescriptionRequest request) {
        String prescriptionNo = Prescription.normalizeNo(request.prescriptionNo());
        if (prescriptionRepository.existsByPrescriptionNo(prescriptionNo)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PRESCRIPTION_NO);
        }

        Prescription prescription = new Prescription(prescriptionNo, request.patientNo(), request.patientName(),
                request.departmentCode(), request.prescriberName(), request.prescriptionType(),
                request.prescribedAt(), request.remark());
        for (PrescriptionItemRequest item : request.items()) {
            prescription.addItem(item.drugCode(), item.drugName(), item.dose(), item.doseUnit(), item.route(),
                    item.frequency(), item.days(), item.totalQuantity(), item.usageNote());
        }
        // 리포지토리가 처방을 넣은 뒤 품목을 함께 넣는다.
        return PrescriptionResponse.from(prescriptionRepository.save(prescription));
    }

    /**
     * 처방을 품목과 함께 조회한다.
     *
     * @throws BusinessException PRESCRIPTION_NOT_FOUND (404)
     */
    public PrescriptionResponse findById(Long id) {
        return PrescriptionResponse.from(getPrescription(id));
    }

    /** 처방을 검색한다 (품목 제외, 처방 일시 최신순). */
    public PageResponse<PrescriptionSummaryResponse> search(PrescriptionSearchCondition condition,
            Pageable pageable) {
        return PageResponse.from(prescriptionRepository.search(condition, pageable)
                .map(PrescriptionSummaryResponse::from));
    }

    /**
     * 처방을 취소한다.
     *
     * @throws BusinessException PRESCRIPTION_NOT_FOUND (404), PRESCRIPTION_NOT_CANCELLABLE (409)
     */
    @Transactional
    public PrescriptionResponse cancel(Long id, PrescriptionCancelRequest request) {
        Prescription prescription = getPrescription(id);
        if (!prescription.isCancellable()) {
            throw new BusinessException(ErrorCode.PRESCRIPTION_NOT_CANCELLABLE);
        }
        prescription.cancel(request.reason(), LocalDateTime.now());
        // MyBatis는 변경 감지가 없으므로 직접 update한다. updatedAt도 함께 갱신되어 응답에 쓰인다.
        prescriptionRepository.save(prescription);
        return PrescriptionResponse.from(prescription);
    }

    /** ID로 처방을 찾고, 없으면 PRESCRIPTION_NOT_FOUND(404)를 던진다. 조제 서비스도 쓴다. */
    /**
     * 조제가 등록된 처방을 조제됨(DISPENSED)으로 바꾼다. {@code DispensingService}가 같은 트랜잭션에서 호출한다.
     */
    @Transactional
    public void markDispensed(Prescription prescription) {
        prescription.markDispensed();
        prescriptionRepository.save(prescription);
    }

    public Prescription getPrescription(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRESCRIPTION_NOT_FOUND));
    }
}
