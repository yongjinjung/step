package com.step.dictionary.pharmacy.dispensing;

import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.pharmacy.dispensing.dto.DispensingSearchCondition;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 조제 리포지토리 ({@link DispensingMapper} 사용). 조제와 품목을 한 묶음으로 저장·조회한다.
 * 조회한 조제에는 처방(품목 제외)이, 품목에는 처방 품목이 채워져 있다.
 */
@Repository
public class DispensingRepository {

    private final DispensingMapper mapper;

    public DispensingRepository(DispensingMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 품목과 함께 등록, 있으면 상태를 저장한다. */
    public Dispensing save(Dispensing dispensing) {
        if (dispensing.getId() == null) {
            mapper.insert(dispensing);
            dispensing.getItems().forEach(item -> mapper.insertItem(dispensing.getId(), item));
        } else {
            mapper.updateStatus(dispensing);
        }
        return dispensing;
    }

    /** 조제를 품목과 함께 조회한다. */
    public Optional<Dispensing> findById(Long id) {
        return mapper.findById(id).map(dispensing -> {
            dispensing.loadItems(mapper.findItemsByDispensingId(id));
            return dispensing;
        });
    }

    /** 처방에 지정한 상태가 아닌 조제가 있는지. 검수 불합격이 아닌 조제가 있으면 다시 조제할 수 없다. */
    public boolean existsByPrescriptionIdAndStatusNot(Long prescriptionId, DispensingStatus status) {
        return mapper.countByPrescriptionIdAndStatusNot(prescriptionId, status) > 0;
    }

    /** 조건에 맞는 조제를 조제 일시 최신순으로 페이지 조회한다 (품목 제외). */
    public Page<Dispensing> search(DispensingSearchCondition condition, Pageable pageable) {
        return Pages.of(mapper.search(condition, pageable), pageable, () -> mapper.count(condition));
    }
}
