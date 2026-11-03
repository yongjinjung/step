package com.step.dictionary.standard.domain;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.history.ChangeHistoryRecorder;
import com.step.dictionary.history.snapshot.DomainSnapshot;
import com.step.dictionary.standard.domain.dto.DomainRequest;
import com.step.dictionary.standard.domain.dto.DomainResponse;
import com.step.dictionary.standard.domain.dto.DomainSearchCondition;
import com.step.dictionary.standard.term.TermRepository;
import com.step.dictionary.standard.word.Word;
import com.step.dictionary.standard.word.WordRepository;
import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 표준 도메인 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>도메인명은 유일하다.</li>
 *   <li>분류어가 아닌 단어에는 도메인을 연결할 수 없다 ({@code NOT_CLASSIFIER_WORD}).</li>
 *   <li>길이/소수점은 {@link DataType} 규칙을 따른다 ({@code INVALID_DOMAIN_SPEC}).</li>
 *   <li>용어에서 쓰는 도메인은 분류어를 바꿀 수 없고({@code DOMAIN_CLASSIFIER_IN_USE}), 삭제할 수 없다({@code DOMAIN_IN_USE}).
 *       타입·길이 변경은 허용하며 용어/컬럼 조회 결과에 바로 반영된다.</li>
 *   <li>등록·수정·삭제마다 변경 이력을 남긴다 (7단계, 같은 트랜잭션).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class DomainService {

    private final DomainRepository domainRepository;
    /** 분류어 단어 조회용. */
    private final WordRepository wordRepository;
    /** 용어가 이 도메인을 참조하는지 확인하는 데 쓴다. */
    private final TermRepository termRepository;
    /** 변경 이력 기록. */
    private final ChangeHistoryRecorder historyRecorder;

    public DomainService(DomainRepository domainRepository, WordRepository wordRepository,
            TermRepository termRepository, ChangeHistoryRecorder historyRecorder) {
        this.domainRepository = domainRepository;
        this.wordRepository = wordRepository;
        this.termRepository = termRepository;
        this.historyRecorder = historyRecorder;
    }

    /**
     * 도메인을 등록한다.
     *
     * @throws BusinessException INVALID_DOMAIN_SPEC, NOT_CLASSIFIER_WORD (400), WORD_NOT_FOUND (404),
     *                           DUPLICATE_DOMAIN_NAME (409)
     */
    @Transactional
    public DomainResponse create(DomainRequest request) {
        // DB 조회 없이 할 수 있는 입력 검증을 먼저 한다.
        validateSpec(request);
        if (domainRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_DOMAIN_NAME);
        }
        Word classifierWord = getClassifierWord(request.classifierWordId());

        Domain domain = domainRepository.save(new Domain(
                request.name(), classifierWord, request.dataType(),
                request.dataLength(), request.dataScale(), request.description()));
        historyRecorder.recordCreate(DomainSnapshot.from(domain));
        return DomainResponse.from(domain);
    }

    /**
     * 도메인을 단건 조회한다. 응답에 분류어 단어명이 들어간다.
     *
     * @throws BusinessException DOMAIN_NOT_FOUND (404)
     */
    public DomainResponse findById(Long id) {
        return DomainResponse.from(getDomain(id));
    }

    /** 조건으로 도메인을 검색한다. 도메인명 오름차순. */
    public PageResponse<DomainResponse> search(DomainSearchCondition condition, Pageable pageable) {
        return PageResponse.from(domainRepository.search(condition, pageable).map(DomainResponse::from));
    }

    /**
     * 도메인을 수정한다 (전체 교체).
     *
     * @throws BusinessException INVALID_DOMAIN_SPEC, NOT_CLASSIFIER_WORD (400), DOMAIN_NOT_FOUND, WORD_NOT_FOUND (404),
     *                           DUPLICATE_DOMAIN_NAME, DOMAIN_CLASSIFIER_IN_USE (409)
     */
    @Transactional
    public DomainResponse update(Long id, DomainRequest request) {
        Domain domain = getDomain(id);
        // 변경 이력용으로 수정 전 값을 떠 둔다.
        DomainSnapshot before = DomainSnapshot.from(domain);
        // 엔티티를 변경하기 전에 검사해 중복이면 저장하지 않고 409로 끝낸다.
        validateSpec(request);
        if (domainRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_DOMAIN_NAME);
        }
        Word classifierWord = getClassifierWord(request.classifierWordId());
        // 용어의 마지막 단어는 도메인의 분류어와 같아야 하므로, 용어에서 쓰는 도메인은 분류어를 바꿀 수 없다.
        // getId()는 프록시를 초기화하지 않으므로 기존 분류어 단어를 추가 조회하지 않는다.
        boolean classifierChanged = !Objects.equals(domain.getClassifierWord().getId(), classifierWord.getId());
        if (classifierChanged && termRepository.existsByDomainId(id)) {
            throw new BusinessException(ErrorCode.DOMAIN_CLASSIFIER_IN_USE);
        }

        domain.update(request.name(), classifierWord, request.dataType(),
                request.dataLength(), request.dataScale(), request.description());
        // MyBatis는 변경 감지가 없으므로 직접 update한다. updatedAt도 함께 갱신되어 응답에 쓰인다.
        domainRepository.save(domain);
        historyRecorder.recordUpdate(before, DomainSnapshot.from(domain));
        return DomainResponse.from(domain);
    }

    /**
     * 도메인을 삭제한다. 용어가 참조하면 FK 위반 대신 409로 막는다.
     *
     * @throws BusinessException DOMAIN_NOT_FOUND (404), DOMAIN_IN_USE (409)
     */
    @Transactional
    public void delete(Long id) {
        Domain domain = getDomain(id);
        if (termRepository.existsByDomainId(id)) {
            throw new BusinessException(ErrorCode.DOMAIN_IN_USE);
        }
        // 삭제 전 값으로 이력을 남긴다.
        historyRecorder.recordDelete(DomainSnapshot.from(domain));
        domainRepository.delete(domain);
    }

    /** 타입별 길이/소수점 규칙을 검사한다. */
    private void validateSpec(DomainRequest request) {
        if (!request.dataType().isValidSpec(request.dataLength(), request.dataScale())) {
            throw new BusinessException(ErrorCode.INVALID_DOMAIN_SPEC);
        }
    }

    /** 단어를 찾고 분류어인지 확인한다. 없으면 404, 분류어가 아니면 400. */
    private Word getClassifierWord(Long wordId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));
        if (!word.isClassifier()) {
            throw new BusinessException(ErrorCode.NOT_CLASSIFIER_WORD);
        }
        return word;
    }

    /** ID로 도메인을 찾고, 없으면 DOMAIN_NOT_FOUND(404)를 던진다. */
    private Domain getDomain(Long id) {
        return domainRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOMAIN_NOT_FOUND));
    }
}
