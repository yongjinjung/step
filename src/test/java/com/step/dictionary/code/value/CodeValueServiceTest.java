package com.step.dictionary.code.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.step.dictionary.code.group.CodeGroup;
import com.step.dictionary.code.group.CodeGroupRepository;
import com.step.dictionary.code.value.dto.CodeValueRequest;
import com.step.dictionary.code.value.dto.CodeValueResponse;
import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.history.ChangeHistoryRecorder;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CodeValueServiceTest {

    @Mock
    private CodeValueRepository valueRepository;

    @Mock
    private CodeGroupRepository groupRepository;

    @Mock
    private ChangeHistoryRecorder historyRecorder;

    @InjectMocks
    private CodeValueService valueService;

    private final CodeGroup group = new CodeGroup("USE_YN", "사용여부", null, true);

    @Test
    @DisplayName("코드값을 등록하면 대문자로 정규화한 값으로 그룹 안에서 중복을 검사한다")
    void create() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(valueRepository.save(any(CodeValue.class))).willAnswer(invocation -> invocation.getArgument(0));

        CodeValueResponse response = valueService.create(1L, new CodeValueRequest("y", "사용", null, 1, true));

        assertThat(response.codeValue()).isEqualTo("Y");
        assertThat(response.codeId()).isEqualTo("USE_YN");
        verify(valueRepository).existsByGroupIdAndCodeValue(1L, "Y");
    }

    @Test
    @DisplayName("그룹 안에서 코드값이 중복되면 등록할 수 없다")
    void createDuplicate() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(valueRepository.existsByGroupIdAndCodeValue(1L, "Y")).willReturn(true);

        assertThatThrownBy(() -> valueService.create(1L, new CodeValueRequest("y", "사용", null, 1, true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_CODE_VALUE);
        verify(valueRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 코드 그룹에는 코드값을 등록할 수 없다")
    void createInUnknownGroup() {
        given(groupRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> valueService.create(1L, new CodeValueRequest("Y", "사용", null, 1, true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CODE_GROUP_NOT_FOUND);
        verifyNoInteractions(valueRepository);
    }

    @Test
    @DisplayName("경로의 그룹에 속하지 않은 코드값은 수정할 수 없다")
    void updateValueOfOtherGroup() {
        given(valueRepository.findByIdAndGroupId(5L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> valueService.update(2L, 5L, new CodeValueRequest("Y", "사용", null, 1, true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CODE_VALUE_NOT_FOUND);
    }

    @Test
    @DisplayName("수정 시 같은 그룹의 다른 코드값과 중복되면 엔티티를 바꾸기 전에 거부한다")
    void updateDuplicate() {
        CodeValue value = new CodeValue(group, "Y", "사용", null, 1, true);
        given(valueRepository.findByIdAndGroupId(5L, 1L)).willReturn(Optional.of(value));
        given(valueRepository.existsByGroupIdAndCodeValueAndIdNot(1L, "N", 5L)).willReturn(true);

        assertThatThrownBy(() -> valueService.update(1L, 5L, new CodeValueRequest("n", "미사용", null, 2, true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_CODE_VALUE);
        assertThat(value.getCodeValue()).isEqualTo("Y");
    }
}
