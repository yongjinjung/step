package com.step.dictionary.pharmacy.prescription;

import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.pharmacy.prescription.dto.PrescriptionSearchCondition;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 처방 리포지토리 ({@link PrescriptionMapper} 사용). 처방과 품목을 한 묶음으로 저장·조회한다.
 *
 * <ul>
 *   <li>등록: 처방 행을 넣고 생성된 ID로 품목을 순서대로 넣는다.</li>
 *   <li>수정: 처방 내용과 품목은 바뀌지 않으므로 상태·취소 정보만 저장한다.</li>
 *   <li>단건 조회는 품목까지, 목록 조회는 처방 행만 읽는다.</li>
 * </ul>
 */
@Repository
public class PrescriptionRepository {

    private final PrescriptionMapper mapper;

    public PrescriptionRepository(PrescriptionMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 품목과 함께 등록, 있으면 상태(취소 정보 포함)를 저장한다. */
    public Prescription save(Prescription prescription) {
        if (prescription.getId() == null) {
            mapper.insert(prescription);
            prescription.getItems().forEach(item -> mapper.insertItem(prescription.getId(), item));
        } else {
            mapper.updateStatus(prescription);
        }
        return prescription;
    }

    /** 처방을 품목과 함께 조회한다. */
    public Optional<Prescription> findById(Long id) {
        return mapper.findById(id).map(prescription -> {
            prescription.loadItems(mapper.findItemsByPrescriptionId(id));
            return prescription;
        });
    }

    /** 처방전번호 중복 확인. 대문자로 정규화한 값을 넘긴다. */
    public boolean existsByPrescriptionNo(String prescriptionNo) {
        return mapper.countByPrescriptionNo(prescriptionNo) > 0;
    }

    /** 조건에 맞는 처방을 처방 일시 최신순으로 페이지 조회한다 (품목 제외). */
    public Page<Prescription> search(PrescriptionSearchCondition condition, Pageable pageable) {
        return Pages.of(mapper.search(condition, pageable), pageable, () -> mapper.count(condition));
    }
}
