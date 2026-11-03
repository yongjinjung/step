package com.step.dictionary.pharmacy.drugreturn;

import com.step.dictionary.pharmacy.drugreturn.dto.DrugReturnSearchCondition;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/** 반납 SQL 매퍼. 반납 행은 불출·조제·처방과 함께, 품목은 불출 품목·조제 품목·처방 품목과 함께 읽는다. */
@Mapper
public interface DrugReturnMapper {

    Optional<DrugReturn> findById(Long id);

    /** 반납의 품목 (ID 순 = 불출 품목 순). */
    List<DrugReturnItem> findItemsByReturnId(Long returnId);

    /** 불출 품목별 반납 수량 합계. 반납이 없는 품목은 결과에 없다. */
    List<ReturnedQuantity> sumQuantitiesByIssueId(Long issueId);

    /** 조건 검색 (반납 일시 최신순, 같으면 ID 내림차순). 품목은 읽지 않는다. */
    List<DrugReturn> search(@Param("c") DrugReturnSearchCondition condition, @Param("p") Pageable pageable);

    long count(@Param("c") DrugReturnSearchCondition condition);

    void insert(DrugReturn drugReturn);

    void insertItem(@Param("returnId") Long returnId, @Param("item") DrugReturnItem item);

    /** {@link #sumQuantitiesByIssueId} 결과 한 줄. */
    class ReturnedQuantity {

        private Long issueItemId;
        private BigDecimal quantity;

        public Long getIssueItemId() {
            return issueItemId;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }
    }
}
