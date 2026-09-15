package com.step.dictionary.standard.word;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.history.ChangeHistoryRecorder;
import com.step.dictionary.standard.domain.DomainRepository;
import com.step.dictionary.standard.term.TermRepository;
import com.step.dictionary.standard.word.dto.WordRequest;
import com.step.dictionary.standard.word.dto.WordResponse;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WordServiceTest {

    @Mock
    private WordRepository wordRepository;

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private ChangeHistoryRecorder historyRecorder;

    @InjectMocks
    private WordService wordService;

    @Test
    @DisplayName("단어를 등록하면 영문약어를 대문자로 정규화해 저장한다")
    void create() {
        given(wordRepository.save(any(Word.class))).willAnswer(invocation -> invocation.getArgument(0));

        WordResponse response = wordService.create(new WordRequest("고객", "cust", "Customer", null, false));

        assertThat(response.name()).isEqualTo("고객");
        assertThat(response.abbreviation()).isEqualTo("CUST");
        verify(wordRepository).existsByAbbreviation("CUST");
    }

    @Test
    @DisplayName("단어명이 중복되면 등록할 수 없다")
    void createDuplicateName() {
        given(wordRepository.existsByName("고객")).willReturn(true);

        assertThatThrownBy(() -> wordService.create(new WordRequest("고객", "CUST", "Customer", null, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_WORD_NAME);
        verify(wordRepository, never()).save(any());
    }

    @Test
    @DisplayName("영문약어는 대소문자 구분 없이 중복을 검사한다")
    void createDuplicateAbbreviation() {
        given(wordRepository.existsByAbbreviation("CUST")).willReturn(true);

        assertThatThrownBy(() -> wordService.create(new WordRequest("고객", "cust", "Customer", null, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_WORD_ABBREVIATION);
        verify(wordRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정 시 다른 단어와 영문약어가 중복되면 수정할 수 없다")
    void updateDuplicateAbbreviation() {
        Word word = new Word("고객", "CUST", "Customer", null, false);
        given(wordRepository.findById(1L)).willReturn(Optional.of(word));
        given(wordRepository.existsByAbbreviationAndIdNot("NO", 1L)).willReturn(true);

        assertThatThrownBy(() -> wordService.update(1L, new WordRequest("고객", "no", "Customer", null, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_WORD_ABBREVIATION);
        assertThat(word.getAbbreviation()).isEqualTo("CUST");
    }

    @Test
    @DisplayName("존재하지 않는 단어는 수정할 수 없다")
    void updateNotFound() {
        given(wordRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wordService.update(1L, new WordRequest("고객", "CUST", "Customer", null, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WORD_NOT_FOUND);
    }

    @Test
    @DisplayName("도메인에서 사용 중인 단어는 분류어를 해제할 수 없다")
    void updateClassifierInUse() {
        Word word = new Word("번호", "NO", "Number", null, true);
        given(wordRepository.findById(1L)).willReturn(Optional.of(word));
        given(domainRepository.existsByClassifierWordId(1L)).willReturn(true);

        assertThatThrownBy(() -> wordService.update(1L, new WordRequest("번호", "NO", "Number", null, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CLASSIFIER_WORD_IN_USE);
        assertThat(word.isClassifier()).isTrue();
    }

    @Test
    @DisplayName("도메인에서 사용 중인 단어는 삭제할 수 없다")
    void deleteInUse() {
        given(wordRepository.findById(1L)).willReturn(Optional.of(new Word("번호", "NO", "Number", null, true)));
        given(domainRepository.existsByClassifierWordId(1L)).willReturn(true);

        assertThatThrownBy(() -> wordService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WORD_IN_USE);
        verify(wordRepository, never()).delete(any());
    }

    @Test
    @DisplayName("용어에서 사용 중인 단어는 단어명을 바꿀 수 없다")
    void renameUsedByTerm() {
        Word word = new Word("고객", "CUST", "Customer", null, false);
        given(wordRepository.findById(1L)).willReturn(Optional.of(word));
        given(termRepository.existsByWordsId(1L)).willReturn(true);

        assertThatThrownBy(() -> wordService.update(1L, new WordRequest("손님", "CUST", "Customer", null, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WORD_USED_BY_TERM);
        assertThat(word.getName()).isEqualTo("고객");
    }

    @Test
    @DisplayName("용어에서 사용 중인 단어도 단어명과 영문약어가 그대로면 수정할 수 있다")
    void updateUsedByTermWithoutRename() {
        Word word = new Word("고객", "CUST", "Customer", null, false);
        given(wordRepository.findById(1L)).willReturn(Optional.of(word));

        WordResponse response = wordService.update(1L, new WordRequest("고객", "cust", "Client", null, false));

        assertThat(response.englishName()).isEqualTo("Client");
        verify(termRepository, never()).existsByWordsId(any());
    }

    @Test
    @DisplayName("용어에서 사용 중인 단어는 삭제할 수 없다")
    void deleteUsedByTerm() {
        given(wordRepository.findById(1L)).willReturn(Optional.of(new Word("고객", "CUST", "Customer", null, false)));
        given(termRepository.existsByWordsId(1L)).willReturn(true);

        assertThatThrownBy(() -> wordService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WORD_IN_USE);
        verify(wordRepository, never()).delete(any());
    }
}
