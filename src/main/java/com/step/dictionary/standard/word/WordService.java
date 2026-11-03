package com.step.dictionary.standard.word;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.history.ChangeHistoryRecorder;
import com.step.dictionary.history.snapshot.WordSnapshot;
import com.step.dictionary.standard.domain.DomainRepository;
import com.step.dictionary.standard.term.TermRepository;
import com.step.dictionary.standard.word.dto.WordRequest;
import com.step.dictionary.standard.word.dto.WordResponse;
import com.step.dictionary.standard.word.dto.WordSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 표준 단어 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>단어명과 영문약어는 각각 유일하다. 영문약어는 대문자로 정규화한 뒤 비교한다.</li>
 *   <li>도메인이 분류어로 쓰는 단어는 분류어 여부를 해제할 수 없다 ({@code CLASSIFIER_WORD_IN_USE}).</li>
 *   <li>용어에서 쓰는 단어는 단어명·영문약어를 바꿀 수 없다 ({@code WORD_USED_BY_TERM}). 영문명·설명은 바꿀 수 있다.</li>
 *   <li>도메인이나 용어가 참조하는 단어는 삭제할 수 없다 ({@code WORD_IN_USE}).</li>
 *   <li>등록·수정·삭제마다 변경 이력을 남긴다 (7단계, 같은 트랜잭션).</li>
 * </ul>
 *
 * <p>클래스 기본은 읽기 전용 트랜잭션이고, 변경 메서드만 {@code @Transactional}로 쓰기 트랜잭션을 연다.
 * 직접 API와 신청 승인({@code StandardDataApplier}) 모두 이 서비스를 거친다.
 */
@Service
@Transactional(readOnly = true)
public class WordService {

    private final WordRepository wordRepository;
    /** 분류어로 참조 중인지 확인하는 데 쓴다. */
    private final DomainRepository domainRepository;
    /** 용어 구성 단어로 참조 중인지 확인하는 데 쓴다. */
    private final TermRepository termRepository;
    /** 변경 이력 기록. */
    private final ChangeHistoryRecorder historyRecorder;

    public WordService(WordRepository wordRepository, DomainRepository domainRepository,
            TermRepository termRepository, ChangeHistoryRecorder historyRecorder) {
        this.wordRepository = wordRepository;
        this.domainRepository = domainRepository;
        this.termRepository = termRepository;
        this.historyRecorder = historyRecorder;
    }

    /**
     * 단어를 등록한다.
     *
     * @throws BusinessException DUPLICATE_WORD_NAME, DUPLICATE_WORD_ABBREVIATION (409)
     */
    @Transactional
    public WordResponse create(WordRequest request) {
        // 정규화한 값으로 중복을 검사해야 'cust'와 'CUST'를 같은 약어로 판단한다.
        String abbreviation = Word.normalizeAbbreviation(request.abbreviation());
        if (wordRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_WORD_NAME);
        }
        if (wordRepository.existsByAbbreviation(abbreviation)) {
            throw new BusinessException(ErrorCode.DUPLICATE_WORD_ABBREVIATION);
        }

        // save()가 insert하면서 생성된 id와 생성·수정 일시를 채운다.
        Word word = wordRepository.save(new Word(
                request.name(), abbreviation, request.englishName(), request.description(), request.classifier()));
        historyRecorder.recordCreate(WordSnapshot.from(word));
        return WordResponse.from(word);
    }

    /**
     * 단어를 단건 조회한다.
     *
     * @throws BusinessException WORD_NOT_FOUND (404)
     */
    public WordResponse findById(Long id) {
        return WordResponse.from(getWord(id));
    }

    /** 조건으로 단어를 검색한다. 단어명 오름차순. */
    public PageResponse<WordResponse> search(WordSearchCondition condition, Pageable pageable) {
        return PageResponse.from(wordRepository.search(condition, pageable).map(WordResponse::from));
    }

    /**
     * 단어를 수정한다 (전체 교체).
     *
     * @throws BusinessException WORD_NOT_FOUND (404), DUPLICATE_WORD_NAME, DUPLICATE_WORD_ABBREVIATION,
     *                           CLASSIFIER_WORD_IN_USE, WORD_USED_BY_TERM (409)
     */
    @Transactional
    public WordResponse update(Long id, WordRequest request) {
        Word word = getWord(id);
        // 변경 이력용으로 수정 전 값을 떠 둔다. 엔티티가 바뀌기 전이어야 한다.
        WordSnapshot before = WordSnapshot.from(word);
        // 엔티티를 변경하기 전에 검사한다. 검사에 쓰는 현재 값(이름·약어·분류어)이 바뀌기 전이어야 한다.
        String abbreviation = Word.normalizeAbbreviation(request.abbreviation());
        if (wordRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_WORD_NAME);
        }
        if (wordRepository.existsByAbbreviationAndIdNot(abbreviation, id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_WORD_ABBREVIATION);
        }
        // 분류어 Y → N으로 바꾸는 경우만 검사한다. 도메인이 분류어로 쓰고 있으면 도메인 규칙이 깨진다.
        if (word.isClassifier() && !request.classifier() && domainRepository.existsByClassifierWordId(id)) {
            throw new BusinessException(ErrorCode.CLASSIFIER_WORD_IN_USE);
        }
        // 용어명과 영문약어는 단어에서 생성되므로 용어에서 쓰는 단어의 이름/약어를 바꾸면 용어와 어긋난다.
        boolean renamed = !word.getName().equals(request.name()) || !word.getAbbreviation().equals(abbreviation);
        if (renamed && termRepository.existsByWordsId(id)) {
            throw new BusinessException(ErrorCode.WORD_USED_BY_TERM);
        }

        word.update(request.name(), abbreviation, request.englishName(), request.description(), request.classifier());
        // MyBatis는 변경 감지가 없으므로 직접 update한다. updatedAt도 함께 갱신되어 응답에 쓰인다.
        wordRepository.save(word);
        historyRecorder.recordUpdate(before, WordSnapshot.from(word));
        return WordResponse.from(word);
    }

    /**
     * 단어를 삭제한다.
     * DB FK 위반(500)에 맡기지 않고 참조 여부를 먼저 확인해 409로 응답한다.
     *
     * @throws BusinessException WORD_NOT_FOUND (404), WORD_IN_USE (409)
     */
    @Transactional
    public void delete(Long id) {
        Word word = getWord(id);
        if (domainRepository.existsByClassifierWordId(id) || termRepository.existsByWordsId(id)) {
            throw new BusinessException(ErrorCode.WORD_IN_USE);
        }
        // 삭제되면 값을 읽을 수 없으므로 지우기 전에 기록한다. 같은 트랜잭션이라 삭제가 실패하면 이력도 롤백된다.
        historyRecorder.recordDelete(WordSnapshot.from(word));
        wordRepository.delete(word);
    }

    /** ID로 단어를 찾고, 없으면 WORD_NOT_FOUND(404)를 던진다. */
    private Word getWord(Long id) {
        return wordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));
    }
}
