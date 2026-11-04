package com.step.dictionary.pharmacy.drugreturn;

import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.pharmacy.Quantities;
import com.step.dictionary.pharmacy.drugreturn.dto.DrugReturnSearchCondition;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** 반납 리포지토리 ({@link DrugReturnMapper} 사용). 반납과 품목을 한 묶음으로 등록·조회한다(수정 없음). */
@Repository
public class DrugReturnRepository {

    private final DrugReturnMapper mapper;

    public DrugReturnRepository(DrugReturnMapper mapper) {
        this.mapper = mapper;
    }

    /** 반납을 품목과 함께 등록한다. */
    public DrugReturn save(DrugReturn drugReturn) {
        mapper.insert(drugReturn);
        drugReturn.getItems().forEach(item -> mapper.insertItem(drugReturn.getId(), item));
        return drugReturn;
    }

    /** 반납을 품목과 함께 조회한다. */
    public Optional<DrugReturn> findById(Long id) {
        return mapper.findById(id).map(drugReturn -> {
            drugReturn.loadItems(mapper.findItemsByReturnId(id));
            return drugReturn;
        });
    }

    /**
     * 불출의 품목별 반납 수량 합계 (불출 품목 ID → 합계). 반납이 없는 품목은 맵에 없다.
     * 반납 한도 검사와 불출 상세의 반납 수량 표시에 쓴다.
     */
    public Map<Long, BigDecimal> sumQuantityByIssueItem(Long issueId) {
        return mapper.sumQuantitiesByIssueId(issueId).stream()
                // SUM 결과는 DB에 따라 소수 자릿수가 달라(Oracle은 1.000을 1로 준다) 수량 규칙(소수 3자리)으로 맞춘다.
                .collect(Collectors.toMap(DrugReturnMapper.ReturnedQuantity::getIssueItemId,
                        row -> Quantities.normalize(row.getQuantity())));
    }

    /** 조건에 맞는 반납을 반납 일시 최신순으로 페이지 조회한다 (품목 제외). */
    public Page<DrugReturn> search(DrugReturnSearchCondition condition, Pageable pageable) {
        return Pages.of(mapper.search(condition, pageable), pageable, () -> mapper.count(condition));
    }
}
