package com.step.dictionary.meta.column;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.history.ChangeHistoryRecorder;
import com.step.dictionary.meta.column.dto.MetaColumnRequest;
import com.step.dictionary.meta.column.dto.MetaColumnResponse;
import com.step.dictionary.meta.table.MetaTable;
import com.step.dictionary.meta.table.MetaTableRepository;
import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.term.Term;
import com.step.dictionary.standard.term.TermRepository;
import com.step.dictionary.standard.word.Word;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetaColumnServiceTest {

    @Mock
    private MetaColumnRepository columnRepository;

    @Mock
    private MetaTableRepository tableRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private ChangeHistoryRecorder historyRecorder;

    @InjectMocks
    private MetaColumnService columnService;

    private final MetaTable table = new MetaTable("TB_CUSTOMER", "고객", null);
    private final Word number = new Word("번호", "NO", "Number", null, true);
    private final Term customerNumber = new Term(
            List.of(new Word("고객", "CUST", "Customer", null, false), number),
            new Domain("번호V20", number, DataType.VARCHAR, 20, null, null), null);

    @Test
    @DisplayName("용어를 매핑해 컬럼을 등록하면 물리명을 대문자로 저장하고 검출 결과를 돌려준다")
    void create() {
        given(tableRepository.findById(1L)).willReturn(Optional.of(table));
        given(termRepository.findById(10L)).willReturn(Optional.of(customerNumber));
        given(columnRepository.save(any(MetaColumn.class))).willAnswer(invocation -> invocation.getArgument(0));

        MetaColumnResponse response = columnService.create(1L, request("cust_no", 10L, DataType.VARCHAR, 20, false, true));

        assertThat(response.physicalName()).isEqualTo("CUST_NO");
        assertThat(response.standard()).isTrue();
        assertThat(response.expected().physicalName()).isEqualTo("CUST_NO");
        verify(columnRepository).existsByTableIdAndPhysicalName(1L, "CUST_NO");
    }

    @Test
    @DisplayName("용어 없이 등록하면 NO_TERM으로 검출한다")
    void createWithoutTerm() {
        given(tableRepository.findById(1L)).willReturn(Optional.of(table));
        given(columnRepository.save(any(MetaColumn.class))).willAnswer(invocation -> invocation.getArgument(0));

        MetaColumnResponse response = columnService.create(1L, request("ETC_TXT", null, DataType.VARCHAR, 100, true, false));

        assertThat(response.standard()).isFalse();
        assertThat(response.issues()).containsExactly(ColumnIssue.NO_TERM);
        assertThat(response.expected()).isNull();
        verifyNoInteractions(termRepository);
    }

    @Test
    @DisplayName("기본키 컬럼이 NULL을 허용하면 DB를 조회하기 전에 거부한다")
    void createPrimaryKeyNullable() {
        assertThatThrownBy(() -> columnService.create(1L, request("CUST_NO", 10L, DataType.VARCHAR, 20, true, true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PRIMARY_KEY_NULLABLE);
        verifyNoInteractions(columnRepository, tableRepository, termRepository);
    }

    @Test
    @DisplayName("데이터 타입에 맞지 않는 길이면 거부한다")
    void createInvalidSpec() {
        assertThatThrownBy(() -> columnService.create(1L, request("REG_DT", null, DataType.DATE, 8, true, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_COLUMN_SPEC);
    }

    @Test
    @DisplayName("같은 테이블에 물리명이 중복되면 등록할 수 없다")
    void createDuplicateName() {
        given(tableRepository.findById(1L)).willReturn(Optional.of(table));
        given(columnRepository.existsByTableIdAndPhysicalName(1L, "CUST_NO")).willReturn(true);

        assertThatThrownBy(() -> columnService.create(1L, request("cust_no", 10L, DataType.VARCHAR, 20, false, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_COLUMN_NAME);
        verify(columnRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 용어는 매핑할 수 없다")
    void createWithUnknownTerm() {
        given(tableRepository.findById(1L)).willReturn(Optional.of(table));
        given(termRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> columnService.create(1L, request("CUST_NO", 99L, DataType.VARCHAR, 20, false, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TERM_NOT_FOUND);
    }

    @Test
    @DisplayName("경로의 테이블에 속하지 않은 컬럼은 찾을 수 없다")
    void updateColumnOfOtherTable() {
        given(columnRepository.findByIdAndTableId(5L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> columnService.update(2L, 5L, request("CUST_NO", null, DataType.VARCHAR, 20, false, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COLUMN_NOT_FOUND);
    }

    @Test
    @DisplayName("수정 시 같은 테이블의 다른 컬럼과 물리명이 중복되면 수정할 수 없다")
    void updateDuplicateName() {
        MetaColumn column = new MetaColumn(table, "CUST_NO", "고객번호", null, DataType.VARCHAR, 20, null, false, false, 1);
        given(columnRepository.findByIdAndTableId(5L, 1L)).willReturn(Optional.of(column));
        given(columnRepository.existsByTableIdAndPhysicalNameAndIdNot(1L, "CUST_NM", 5L)).willReturn(true);

        assertThatThrownBy(() -> columnService.update(1L, 5L, request("cust_nm", null, DataType.VARCHAR, 20, false, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_COLUMN_NAME);
        assertThat(column.getPhysicalName()).isEqualTo("CUST_NO");
    }

    private static MetaColumnRequest request(String physicalName, Long termId, DataType dataType, Integer length,
            boolean nullable, boolean primaryKey) {
        return new MetaColumnRequest(physicalName, "논리명", termId, dataType, length, null, nullable, primaryKey, 1);
    }
}
