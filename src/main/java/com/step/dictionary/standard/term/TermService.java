package com.step.dictionary.standard.term;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.history.ChangeHistoryRecorder;
import com.step.dictionary.history.snapshot.TermSnapshot;
import com.step.dictionary.meta.column.MetaColumnRepository;
import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.domain.DomainRepository;
import com.step.dictionary.standard.term.dto.TermRequest;
import com.step.dictionary.standard.term.dto.TermResponse;
import com.step.dictionary.standard.term.dto.TermSearchCondition;
import com.step.dictionary.standard.word.Word;
import com.step.dictionary.standard.word.WordRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 표준 용어 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>입력은 구성 단어 ID 목록(조합 순서, 1~10개, 같은 단어 반복 가능)과 도메인 ID다.</li>
 *   <li>용어명(단어명 연결)과 영문약어(단어 약어를 '_'로 연결)는 자동 생성하며 각각 유일하다.</li>
 *   <li>용어명은 200자, 영문약어는 64자를 넘을 수 없다 ({@code TERM_TOO_LONG}).</li>
 *   <li>마지막 단어는 분류어여야 하고({@code TERM_LAST_WORD_NOT_CLASSIFIER}),
 *       도메인의 분류어와 같아야 한다({@code TERM_DOMAIN_MISMATCH}).</li>
 *   <li>컬럼에 매핑된 용어는 삭제할 수 없다 ({@code TERM_IN_USE}).</li>
 *   <li>등록·수정·삭제마다 변경 이력을 남긴다 (7단계, 같은 트랜잭션).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class TermService {

    private final TermRepository termRepository;
    /** 구성 단어 조회용. */
    private final WordRepository wordRepository;
    /** 도메인 조회용. */
    private final DomainRepository domainRepository;
    /** 컬럼 매핑 여부 확인용 (삭제 차단). */
    private final MetaColumnRepository columnRepository;
    /** 변경 이력 기록. */
    private final ChangeHistoryRecorder historyRecorder;

    public TermService(TermRepository termRepository, WordRepository wordRepository,
            DomainRepository domainRepository, MetaColumnRepository columnRepository,
            ChangeHistoryRecorder historyRecorder) {
        this.termRepository = termRepository;
        this.wordRepository = wordRepository;
        this.domainRepository = domainRepository;
        this.columnRepository = columnRepository;
        this.historyRecorder = historyRecorder;
    }

    /**
     * 용어를 등록한다.
     * 검사 순서: 대상 존재(404) → 조합 규칙(400) → 길이(400) → 중복(409).
     *
     * @throws BusinessException WORD_NOT_FOUND, DOMAIN_NOT_FOUND (404), TERM_LAST_WORD_NOT_CLASSIFIER,
     *                           TERM_DOMAIN_MISMATCH, TERM_TOO_LONG (400),
     *                           DUPLICATE_TERM_NAME, DUPLICATE_TERM_ABBREVIATION (409)
     */
    @Transactional
    public TermResponse create(TermRequest request) {
        List<Word> words = getWords(request.wordIds());
        Domain domain = getDomain(request.domainId());
        validateComposition(words, domain);

        // 엔티티와 같은 규칙으로 이름/약어를 미리 계산해 길이와 중복을 검사한다.
        String name = Term.composeName(words);
        String abbreviation = Term.composeAbbreviation(words);
        validateLength(name, abbreviation);
        if (termRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_TERM_NAME);
        }
        if (termRepository.existsByAbbreviation(abbreviation)) {
            throw new BusinessException(ErrorCode.DUPLICATE_TERM_ABBREVIATION);
        }

        // save() 시 std_term insert 후 std_term_word에 seq 1부터 단어 행이 insert된다.
        Term term = termRepository.save(new Term(words, domain, request.description()));
        historyRecorder.recordCreate(TermSnapshot.from(term));
        return TermResponse.from(term);
    }

    /**
     * 용어를 단건 조회한다. 구성 단어와 도메인은 트랜잭션 안에서 지연 로딩된다.
     *
     * @throws BusinessException TERM_NOT_FOUND (404)
     */
    public TermResponse findById(Long id) {
        return TermResponse.from(getTerm(id));
    }

    /** 조건으로 용어를 검색한다. 용어명 오름차순. */
    public PageResponse<TermResponse> search(TermSearchCondition condition, Pageable pageable) {
        return PageResponse.from(termRepository.search(condition, pageable).map(TermResponse::from));
    }

    /**
     * 용어를 수정한다 (전체 교체). 구성 단어가 바뀌면 용어명/영문약어를 다시 생성한다.
     *
     * @throws BusinessException TERM_NOT_FOUND, WORD_NOT_FOUND, DOMAIN_NOT_FOUND (404),
     *                           TERM_LAST_WORD_NOT_CLASSIFIER, TERM_DOMAIN_MISMATCH, TERM_TOO_LONG (400),
     *                           DUPLICATE_TERM_NAME, DUPLICATE_TERM_ABBREVIATION (409)
     */
    @Transactional
    public TermResponse update(Long id, TermRequest request) {
        Term term = getTerm(id);
        // 변경 이력용으로 수정 전 값을 떠 둔다. term.update()가 단어 목록을 비우기 전이어야 한다.
        TermSnapshot before = TermSnapshot.from(term);
        List<Word> words = getWords(request.wordIds());
        Domain domain = getDomain(request.domainId());
        validateComposition(words, domain);

        // 엔티티를 변경하기 전에 검사해야 한다. 변경 후 조회하면 auto flush로 unique 제약 위반이 먼저 터진다.
        String name = Term.composeName(words);
        String abbreviation = Term.composeAbbreviation(words);
        validateLength(name, abbreviation);
        if (termRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_TERM_NAME);
        }
        if (termRepository.existsByAbbreviationAndIdNot(abbreviation, id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_TERM_ABBREVIATION);
        }

        term.update(words, domain, request.description());
        // 즉시 update(조인 테이블 포함)를 실행해 갱신된 updatedAt으로 응답한다.
        termRepository.flush();
        historyRecorder.recordUpdate(before, TermSnapshot.from(term));
        return TermResponse.from(term);
    }

    /**
     * 용어를 삭제한다. 구성 단어 행(std_term_word)은 JPA가 함께 지운다.
     *
     * @throws BusinessException TERM_NOT_FOUND (404), TERM_IN_USE (409)
     */
    @Transactional
    public void delete(Long id) {
        Term term = getTerm(id);
        if (columnRepository.existsByTermId(id)) {
            throw new BusinessException(ErrorCode.TERM_IN_USE);
        }
        // 삭제 전 값으로 이력을 남긴다.
        historyRecorder.recordDelete(TermSnapshot.from(term));
        termRepository.delete(term);
    }

    /** 요청 순서대로 단어를 돌려준다. 같은 단어가 여러 번 나와도 된다. */
    private List<Word> getWords(List<Long> wordIds) {
        // IN 쿼리 한 번으로 조회한다. 결과는 요청 순서와 무관하고 중복 ID는 한 번만 오므로 Map으로 만든다.
        Map<Long, Word> found = wordRepository.findAllById(wordIds).stream()
                .collect(Collectors.toMap(Word::getId, Function.identity(), (first, second) -> first));
        // 요청 ID 순서를 따라 다시 나열한다. 하나라도 없으면 404.
        return wordIds.stream()
                .map(wordId -> {
                    Word word = found.get(wordId);
                    if (word == null) {
                        throw new BusinessException(ErrorCode.WORD_NOT_FOUND);
                    }
                    return word;
                })
                .toList();
    }

    /** 마지막 단어는 분류어이고, 도메인의 분류어와 같아야 한다. */
    private void validateComposition(List<Word> words, Domain domain) {
        // 요청 DTO의 @NotEmpty로 목록이 비어 있지 않음이 보장된다.
        Word lastWord = words.getLast();
        if (!lastWord.isClassifier()) {
            throw new BusinessException(ErrorCode.TERM_LAST_WORD_NOT_CLASSIFIER);
        }
        // ID로 비교한다. 도메인의 분류어 단어는 지연 로딩 프록시일 수 있지만 getId()는 추가 조회를 하지 않는다.
        if (!Objects.equals(domain.getClassifierWord().getId(), lastWord.getId())) {
            throw new BusinessException(ErrorCode.TERM_DOMAIN_MISMATCH);
        }
    }

    /** 생성된 이름/약어가 컬럼 길이를 넘는지 검사한다. DB 오류(500) 대신 400으로 응답하기 위해서다. */
    private void validateLength(String name, String abbreviation) {
        if (name.length() > Term.MAX_NAME_LENGTH || abbreviation.length() > Term.MAX_ABBREVIATION_LENGTH) {
            throw new BusinessException(ErrorCode.TERM_TOO_LONG);
        }
    }

    /** ID로 도메인을 찾고, 없으면 DOMAIN_NOT_FOUND(404)를 던진다. */
    private Domain getDomain(Long domainId) {
        return domainRepository.findById(domainId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOMAIN_NOT_FOUND));
    }

    /** ID로 용어를 찾고, 없으면 TERM_NOT_FOUND(404)를 던진다. */
    private Term getTerm(Long id) {
        return termRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERM_NOT_FOUND));
    }
}
