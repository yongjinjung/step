package com.step.dictionary.code.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.step.dictionary.code.group.dto.CodeGroupRequest;
import com.step.dictionary.code.group.dto.CodeGroupResponse;
import com.step.dictionary.code.value.CodeValue;
import com.step.dictionary.code.value.CodeValueRepository;
import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.history.ChangeHistoryRecorder;
import com.step.dictionary.meta.column.MetaColumnRepository;
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
class CodeGroupServiceTest {

    @Mock
    private CodeGroupRepository groupRepository;

    @Mock
    private CodeValueRepository valueRepository;

    @Mock
    private MetaColumnRepository columnRepository;

    @Mock
    private ChangeHistoryRecorder historyRecorder;

    @InjectMocks
    private CodeGroupService groupService;

    @Test
    @DisplayName("코드 그룹을 등록하면 코드ID를 대문자로 정규화해 저장하고 이력을 남긴다")
    void create() {
        given(groupRepository.save(any(CodeGroup.class))).willAnswer(invocation -> invocation.getArgument(0));

        CodeGroupResponse response = groupService.create(new CodeGroupRequest("order_stat", "주문상태", null, true));

        assertThat(response.codeId()).isEqualTo("ORDER_STAT");
        verify(groupRepository).existsByCodeId("ORDER_STAT");
        verify(historyRecorder).recordCreate(any());
    }

    @Test
    @DisplayName("코드ID는 대소문자 구분 없이 중복을 검사한다")
    void createDuplicate() {
        given(groupRepository.existsByCodeId("ORDER_STAT")).willReturn(true);

        assertThatThrownBy(() -> groupService.create(new CodeGroupRequest("order_stat", "주문상태", null, true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_CODE_ID);
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정 시 다른 그룹과 코드ID가 중복되면 엔티티를 바꾸기 전에 거부한다")
    void updateDuplicate() {
        CodeGroup group = new CodeGroup("ORDER_STAT", "주문상태", null, true);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupRepository.existsByCodeIdAndIdNot("PAY_TYPE", 1L)).willReturn(true);

        assertThatThrownBy(() -> groupService.update(1L, new CodeGroupRequest("pay_type", "결제수단", null, true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_CODE_ID);
        assertThat(group.getCodeId()).isEqualTo("ORDER_STAT");
    }

    @Test
    @DisplayName("컬럼에 매핑된 코드 그룹은 삭제할 수 없다")
    void deleteInUse() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(new CodeGroup("ORDER_STAT", "주문상태", null, true)));
        given(columnRepository.existsByCodeGroupId(1L)).willReturn(true);

        assertThatThrownBy(() -> groupService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CODE_GROUP_IN_USE);
        verifyNoInteractions(valueRepository, historyRecorder);
        verify(groupRepository, never()).delete(any());
    }

    @Test
    @DisplayName("코드 그룹을 삭제하면 코드값마다 삭제 이력을 남기고 코드값을 그룹보다 먼저 삭제한다")
    void deleteWithValues() {
        CodeGroup group = new CodeGroup("ORDER_STAT", "주문상태", null, true);
        List<CodeValue> values = List.of(
                new CodeValue(group, "01", "접수", null, 1, true),
                new CodeValue(group, "02", "결제완료", null, 2, true));
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(valueRepository.findAllByGroupIdOrderBySortOrderAscIdAsc(1L)).willReturn(values);

        groupService.delete(1L);

        verify(historyRecorder, times(3)).recordDelete(any());
        InOrder order = inOrder(valueRepository, groupRepository);
        order.verify(valueRepository).deleteAll(values);
        order.verify(groupRepository).delete(group);
    }
}
