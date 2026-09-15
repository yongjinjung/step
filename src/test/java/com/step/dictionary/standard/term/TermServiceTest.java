package com.step.dictionary.standard.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.history.ChangeHistoryRecorder;
import com.step.dictionary.meta.column.MetaColumnRepository;
import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.domain.DomainRepository;
import com.step.dictionary.standard.term.dto.TermRequest;
import com.step.dictionary.standard.term.dto.TermResponse;
import com.step.dictionary.standard.word.Word;
import com.step.dictionary.standard.word.WordRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TermServiceTest {

    @Mock
    private TermRepository termRepository;

    @Mock
    private WordRepository wordRepository;

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private MetaColumnRepository columnRepository;

    @Mock
    private ChangeHistoryRecorder historyRecorder;

    @InjectMocks
    private TermService termService;

    private final Word customer = word(1L, "고객", "CUST", false);
    private final Word number = word(2L, "번호", "NO", true);
    private final Domain numberDomain = domain(10L, "번호V20", number);

    @Test
    @DisplayName("단어 ID 순서대로 용어명과 영문약어를 생성해 등록한다")
    void create() {
        // findAllById는 순서를 보장하지 않으므로 뒤섞어 돌려준다.
        given(wordRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(number, customer));
        given(domainRepository.findById(10L)).willReturn(Optional.of(numberDomain));
        given(termRepository.save(any(Term.class))).willAnswer(invocation -> invocation.getArgument(0));

        TermResponse response = termService.create(new TermRequest(List.of(1L, 2L), 10L, null));

        assertThat(response.name()).isEqualTo("고객번호");
        assertThat(response.abbreviation()).isEqualTo("CUST_NO");
        assertThat(response.words()).extracting(TermResponse.TermWord::name).containsExactly("고객", "번호");
        assertThat(response.domainName()).isEqualTo("번호V20");
        verify(termRepository).existsByAbbreviation("CUST_NO");
    }

    @Test
    @DisplayName("마지막 단어가 분류어가 아니면 등록할 수 없다")
    void createLastWordNotClassifier() {
        given(wordRepository.findAllById(List.of(2L, 1L))).willReturn(List.of(customer, number));
        given(domainRepository.findById(10L)).willReturn(Optional.of(numberDomain));

        assertThatThrownBy(() -> termService.create(new TermRequest(List.of(2L, 1L), 10L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TERM_LAST_WORD_NOT_CLASSIFIER);
        verify(termRepository, never()).save(any());
    }

    @Test
    @DisplayName("마지막 단어와 도메인의 분류어가 다르면 등록할 수 없다")
    void createDomainMismatch() {
        Domain nameDomain = domain(11L, "명V100", word(3L, "명", "NM", true));
        given(wordRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(customer, number));
        given(domainRepository.findById(11L)).willReturn(Optional.of(nameDomain));

        assertThatThrownBy(() -> termService.create(new TermRequest(List.of(1L, 2L), 11L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TERM_DOMAIN_MISMATCH);
        verify(termRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 단어가 있으면 등록할 수 없다")
    void createWithUnknownWord() {
        given(wordRepository.findAllById(List.of(1L, 99L))).willReturn(List.of(customer));

        assertThatThrownBy(() -> termService.create(new TermRequest(List.of(1L, 99L), 10L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WORD_NOT_FOUND);
    }

    @Test
    @DisplayName("생성된 영문약어가 64자를 넘으면 등록할 수 없다")
    void createTooLong() {
        Word longWord = word(4L, "긴단어", "A".repeat(40), false);
        Word longClassifier = word(5L, "긴분류어", "B".repeat(30), true);
        given(wordRepository.findAllById(List.of(4L, 5L))).willReturn(List.of(longWord, longClassifier));
        given(domainRepository.findById(12L)).willReturn(Optional.of(domain(12L, "긴분류어V20", longClassifier)));

        assertThatThrownBy(() -> termService.create(new TermRequest(List.of(4L, 5L), 12L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TERM_TOO_LONG);
    }

    @Test
    @DisplayName("생성된 영문약어가 중복되면 등록할 수 없다")
    void createDuplicateAbbreviation() {
        given(wordRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(customer, number));
        given(domainRepository.findById(10L)).willReturn(Optional.of(numberDomain));
        given(termRepository.existsByAbbreviation("CUST_NO")).willReturn(true);

        assertThatThrownBy(() -> termService.create(new TermRequest(List.of(1L, 2L), 10L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_TERM_ABBREVIATION);
        verify(termRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정 시 다른 용어와 이름이 중복되면 구성 단어를 바꾸지 않는다")
    void updateDuplicateName() {
        Word account = word(3L, "계좌", "ACCT", false);
        Term term = new Term(List.of(customer, number), numberDomain, null);
        given(termRepository.findById(100L)).willReturn(Optional.of(term));
        given(wordRepository.findAllById(List.of(3L, 2L))).willReturn(List.of(account, number));
        given(domainRepository.findById(10L)).willReturn(Optional.of(numberDomain));
        given(termRepository.existsByNameAndIdNot("계좌번호", 100L)).willReturn(true);

        assertThatThrownBy(() -> termService.update(100L, new TermRequest(List.of(3L, 2L), 10L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_TERM_NAME);
        assertThat(term.getName()).isEqualTo("고객번호");
        assertThat(term.getWords()).containsExactly(customer, number);
    }

    @Test
    @DisplayName("존재하지 않는 용어는 수정할 수 없다")
    void updateNotFound() {
        given(termRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> termService.update(100L, new TermRequest(List.of(1L, 2L), 10L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TERM_NOT_FOUND);
    }

    @Test
    @DisplayName("컬럼에 매핑된 용어는 삭제할 수 없다")
    void deleteInUse() {
        given(termRepository.findById(100L)).willReturn(Optional.of(new Term(List.of(customer, number), numberDomain, null)));
        given(columnRepository.existsByTermId(100L)).willReturn(true);

        assertThatThrownBy(() -> termService.delete(100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TERM_IN_USE);
        verify(termRepository, never()).delete(any());
    }

    private static Word word(long id, String name, String abbreviation, boolean classifier) {
        Word word = new Word(name, abbreviation, abbreviation, null, classifier);
        ReflectionTestUtils.setField(word, "id", id);
        return word;
    }

    private static Domain domain(long id, String name, Word classifierWord) {
        Domain domain = new Domain(name, classifierWord, DataType.VARCHAR, 20, null, null);
        ReflectionTestUtils.setField(domain, "id", id);
        return domain;
    }
}
