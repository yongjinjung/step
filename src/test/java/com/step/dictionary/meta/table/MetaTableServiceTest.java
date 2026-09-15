package com.step.dictionary.meta.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.history.ChangeHistoryRecorder;
import com.step.dictionary.history.snapshot.ColumnSnapshot;
import com.step.dictionary.history.snapshot.TableSnapshot;
import com.step.dictionary.meta.column.MetaColumn;
import com.step.dictionary.meta.column.MetaColumnRepository;
import com.step.dictionary.meta.table.dto.MetaTableRequest;
import com.step.dictionary.meta.table.dto.MetaTableResponse;
import com.step.dictionary.standard.domain.DataType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetaTableServiceTest {

    @Mock
    private MetaTableRepository tableRepository;

    @Mock
    private MetaColumnRepository columnRepository;

    @Mock
    private ChangeHistoryRecorder historyRecorder;

    @InjectMocks
    private MetaTableService tableService;

    @Test
    @DisplayName("테이블을 등록하면 물리명을 대문자로 정규화해 중복을 검사하고 저장한다")
    void create() {
        given(tableRepository.save(any(MetaTable.class))).willAnswer(invocation -> invocation.getArgument(0));

        MetaTableResponse response = tableService.create(new MetaTableRequest("tb_customer", "고객", null));

        assertThat(response.physicalName()).isEqualTo("TB_CUSTOMER");
        verify(tableRepository).existsByPhysicalName("TB_CUSTOMER");
    }

    @Test
    @DisplayName("물리명이 중복되면 등록할 수 없다")
    void createDuplicate() {
        given(tableRepository.existsByPhysicalName("TB_CUSTOMER")).willReturn(true);

        assertThatThrownBy(() -> tableService.create(new MetaTableRequest("tb_customer", "고객", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_TABLE_NAME);
        verify(tableRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정 시 다른 테이블과 물리명이 중복되면 수정할 수 없다")
    void updateDuplicate() {
        MetaTable table = new MetaTable("TB_CUSTOMER", "고객", null);
        given(tableRepository.findById(1L)).willReturn(Optional.of(table));
        given(tableRepository.existsByPhysicalNameAndIdNot("TB_ORDER", 1L)).willReturn(true);

        assertThatThrownBy(() -> tableService.update(1L, new MetaTableRequest("tb_order", "주문", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_TABLE_NAME);
        assertThat(table.getPhysicalName()).isEqualTo("TB_CUSTOMER");
    }

    @Test
    @DisplayName("테이블을 삭제하면 컬럼마다 삭제 이력을 남기고 컬럼을 먼저 삭제한 뒤 테이블을 삭제한다")
    void deleteWithColumns() {
        MetaTable table = new MetaTable("TB_CUSTOMER", "고객", null);
        List<MetaColumn> columns = List.of(
                new MetaColumn(table, "CUST_NO", "고객번호", null, DataType.VARCHAR, 20, null, false, true, 1));
        given(tableRepository.findById(1L)).willReturn(Optional.of(table));
        given(columnRepository.findAllByTableIdOrderBySeqAscIdAsc(1L)).willReturn(columns);

        tableService.delete(1L);

        InOrder inOrder = inOrder(historyRecorder, columnRepository, tableRepository);
        inOrder.verify(historyRecorder).recordDelete(any(ColumnSnapshot.class));
        inOrder.verify(columnRepository).deleteAll(columns);
        inOrder.verify(historyRecorder).recordDelete(any(TableSnapshot.class));
        inOrder.verify(tableRepository).delete(table);
    }

    @Test
    @DisplayName("존재하지 않는 테이블은 삭제할 수 없다")
    void deleteNotFound() {
        given(tableRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tableService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TABLE_NOT_FOUND);
        verify(columnRepository, never()).deleteAll(any());
    }
}
