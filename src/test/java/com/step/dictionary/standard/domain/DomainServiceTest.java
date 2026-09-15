package com.step.dictionary.standard.domain;

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
import com.step.dictionary.standard.domain.dto.DomainRequest;
import com.step.dictionary.standard.domain.dto.DomainResponse;
import com.step.dictionary.standard.term.TermRepository;
import com.step.dictionary.standard.word.Word;
import com.step.dictionary.standard.word.WordRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DomainServiceTest {

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private WordRepository wordRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private ChangeHistoryRecorder historyRecorder;

    @InjectMocks
    private DomainService domainService;

    @Test
    @DisplayName("분류어 단어로 도메인을 등록한다")
    void create() {
        given(wordRepository.findById(1L)).willReturn(Optional.of(new Word("금액", "AMT", "Amount", null, true)));
        given(domainRepository.save(any(Domain.class))).willAnswer(invocation -> invocation.getArgument(0));

        DomainResponse response = domainService.create(
                new DomainRequest("금액N15", 1L, DataType.DECIMAL, 15, null, null));

        assertThat(response.name()).isEqualTo("금액N15");
        assertThat(response.classifierWordName()).isEqualTo("금액");
        assertThat(response.dataScale()).isZero();
    }

    @Test
    @DisplayName("분류어가 아닌 단어로는 도메인을 등록할 수 없다")
    void createWithNonClassifierWord() {
        given(wordRepository.findById(1L)).willReturn(Optional.of(new Word("고객", "CUST", "Customer", null, false)));

        assertThatThrownBy(() -> domainService.create(new DomainRequest("고객V20", 1L, DataType.VARCHAR, 20, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_CLASSIFIER_WORD);
        verify(domainRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 단어로는 도메인을 등록할 수 없다")
    void createWithUnknownWord() {
        given(wordRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> domainService.create(new DomainRequest("번호V20", 1L, DataType.VARCHAR, 20, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WORD_NOT_FOUND);
    }

    @Test
    @DisplayName("데이터 타입에 맞지 않는 길이면 DB를 조회하기 전에 거부한다")
    void createInvalidSpec() {
        assertThatThrownBy(() -> domainService.create(new DomainRequest("번호V", 1L, DataType.VARCHAR, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_DOMAIN_SPEC);
        verifyNoInteractions(domainRepository, wordRepository);
    }

    @Test
    @DisplayName("도메인명이 중복되면 등록할 수 없다")
    void createDuplicateName() {
        given(domainRepository.existsByName("번호V20")).willReturn(true);

        assertThatThrownBy(() -> domainService.create(new DomainRequest("번호V20", 1L, DataType.VARCHAR, 20, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_DOMAIN_NAME);
        verify(domainRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정 시 다른 도메인과 이름이 중복되면 수정할 수 없다")
    void updateDuplicateName() {
        Domain domain = new Domain("번호V20", new Word("번호", "NO", "Number", null, true), DataType.VARCHAR, 20, null, null);
        given(domainRepository.findById(1L)).willReturn(Optional.of(domain));
        given(domainRepository.existsByNameAndIdNot("번호V30", 1L)).willReturn(true);

        assertThatThrownBy(() -> domainService.update(1L, new DomainRequest("번호V30", 1L, DataType.VARCHAR, 30, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_DOMAIN_NAME);
        assertThat(domain.getName()).isEqualTo("번호V20");
    }

    @Test
    @DisplayName("존재하지 않는 도메인은 수정할 수 없다")
    void updateNotFound() {
        given(domainRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> domainService.update(1L, new DomainRequest("번호V20", 1L, DataType.VARCHAR, 20, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DOMAIN_NOT_FOUND);
    }

    @Test
    @DisplayName("용어에서 사용 중인 도메인은 분류어를 바꿀 수 없다")
    void updateClassifierInUse() {
        Word number = classifierWord(1L, "번호", "NO");
        Domain domain = new Domain("번호V20", number, DataType.VARCHAR, 20, null, null);
        given(domainRepository.findById(10L)).willReturn(Optional.of(domain));
        given(wordRepository.findById(2L)).willReturn(Optional.of(classifierWord(2L, "코드", "CD")));
        given(termRepository.existsByDomainId(10L)).willReturn(true);

        assertThatThrownBy(() -> domainService.update(10L, new DomainRequest("번호V20", 2L, DataType.VARCHAR, 20, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DOMAIN_CLASSIFIER_IN_USE);
        assertThat(domain.getClassifierWord()).isSameAs(number);
    }

    @Test
    @DisplayName("용어에서 사용 중인 도메인은 삭제할 수 없다")
    void deleteInUse() {
        Domain domain = new Domain("번호V20", classifierWord(1L, "번호", "NO"), DataType.VARCHAR, 20, null, null);
        given(domainRepository.findById(10L)).willReturn(Optional.of(domain));
        given(termRepository.existsByDomainId(10L)).willReturn(true);

        assertThatThrownBy(() -> domainService.delete(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DOMAIN_IN_USE);
        verify(domainRepository, never()).delete(any());
    }

    private static Word classifierWord(long id, String name, String abbreviation) {
        Word word = new Word(name, abbreviation, abbreviation, null, true);
        ReflectionTestUtils.setField(word, "id", id);
        return word;
    }
}
