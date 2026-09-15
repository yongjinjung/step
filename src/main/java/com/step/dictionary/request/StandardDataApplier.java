package com.step.dictionary.request;

import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.domain.DomainRepository;
import com.step.dictionary.standard.domain.DomainService;
import com.step.dictionary.standard.domain.dto.DomainRequest;
import com.step.dictionary.standard.term.Term;
import com.step.dictionary.standard.term.TermRepository;
import com.step.dictionary.standard.term.TermService;
import com.step.dictionary.standard.term.dto.TermRequest;
import com.step.dictionary.standard.word.Word;
import com.step.dictionary.standard.word.WordRepository;
import com.step.dictionary.standard.word.WordService;
import com.step.dictionary.standard.word.dto.WordRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 신청 대상(단어/도메인/용어)을 다루는 창구.
 *
 * <p>{@link ChangeRequestService}가 대상 종류별 분기를 몰라도 되도록 여기에 모았다.
 * <ul>
 *   <li>대상의 현재 수정 일시 조회 (신청 시 기록, 승인 시 충돌 검사)</li>
 *   <li>승인된 신청 반영: 신청 내용 JSON을 원래 요청 DTO로 되돌려 기존 서비스({@link WordService} 등)를 호출한다.
 *       직접 등록/수정/삭제 API와 같은 코드를 타므로 중복·참조·조합 규칙이 모두 똑같이 적용된다.</li>
 *   <li>신청 내용 JSON 직렬화/역직렬화</li>
 * </ul>
 * 새 신청 대상이 생기면 {@link TargetType}과 이 클래스의 switch만 늘리면 된다
 * (switch에 default가 없어 빠뜨리면 컴파일 오류가 난다).
 */
@Component
public class StandardDataApplier {

    private final WordRepository wordRepository;
    private final DomainRepository domainRepository;
    private final TermRepository termRepository;
    private final WordService wordService;
    private final DomainService domainService;
    private final TermService termService;
    /** 스프링 부트가 등록한 Jackson 3 매퍼. 컨트롤러 요청/응답과 같은 설정으로 신청 내용을 다룬다. */
    private final JsonMapper jsonMapper;

    public StandardDataApplier(WordRepository wordRepository, DomainRepository domainRepository,
            TermRepository termRepository, WordService wordService, DomainService domainService,
            TermService termService, JsonMapper jsonMapper) {
        this.wordRepository = wordRepository;
        this.domainRepository = domainRepository;
        this.termRepository = termRepository;
        this.wordService = wordService;
        this.domainService = domainService;
        this.termService = termService;
        this.jsonMapper = jsonMapper;
    }

    /**
     * 대상의 현재 수정 일시를 조회한다.
     *
     * @return 대상이 없으면 빈 값
     */
    public Optional<LocalDateTime> findUpdatedAt(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case WORD -> wordRepository.findById(targetId).map(Word::getUpdatedAt);
            case DOMAIN -> domainRepository.findById(targetId).map(Domain::getUpdatedAt);
            case TERM -> termRepository.findById(targetId).map(Term::getUpdatedAt);
        };
    }

    /**
     * 신청을 대상에 반영한다. 호출한 쪽의 트랜잭션에 참여하므로, 여기서 업무 규칙 예외가 나면 승인 전체가 롤백된다.
     *
     * @param targetType  대상 종류
     * @param requestType 신청 종류
     * @param targetId    대상 ID (등록은 null)
     * @param payload     신청 내용 JSON (삭제는 null)
     * @return 반영된 대상 ID. 등록이면 새로 만들어진 ID
     * @throws com.step.dictionary.global.error.BusinessException 기존 서비스의 업무 규칙 위반
     */
    public Long apply(TargetType targetType, RequestType requestType, Long targetId, String payload) {
        return switch (targetType) {
            case WORD -> applyWord(requestType, targetId, payload);
            case DOMAIN -> applyDomain(requestType, targetId, payload);
            case TERM -> applyTerm(requestType, targetId, payload);
        };
    }

    /** 신청 내용 객체(요청 DTO)를 JSON 문자열로 바꾼다. null이면 null. */
    public String serialize(Object payload) {
        return payload == null ? null : jsonMapper.writeValueAsString(payload);
    }

    /** 저장된 신청 내용 JSON을 응답용 트리로 바꾼다. 응답에서 문자열이 아닌 JSON 객체로 보이게 하기 위해서다. */
    public JsonNode toJsonNode(String payload) {
        return payload == null ? null : jsonMapper.readTree(payload);
    }

    private Long applyWord(RequestType requestType, Long targetId, String payload) {
        return switch (requestType) {
            case CREATE -> wordService.create(read(payload, WordRequest.class)).id();
            case UPDATE -> wordService.update(targetId, read(payload, WordRequest.class)).id();
            case DELETE -> {
                wordService.delete(targetId);
                yield targetId;
            }
        };
    }

    private Long applyDomain(RequestType requestType, Long targetId, String payload) {
        return switch (requestType) {
            case CREATE -> domainService.create(read(payload, DomainRequest.class)).id();
            case UPDATE -> domainService.update(targetId, read(payload, DomainRequest.class)).id();
            case DELETE -> {
                domainService.delete(targetId);
                yield targetId;
            }
        };
    }

    private Long applyTerm(RequestType requestType, Long targetId, String payload) {
        return switch (requestType) {
            case CREATE -> termService.create(read(payload, TermRequest.class)).id();
            case UPDATE -> termService.update(targetId, read(payload, TermRequest.class)).id();
            case DELETE -> {
                termService.delete(targetId);
                yield targetId;
            }
        };
    }

    /** JSON을 요청 DTO로 되돌린다. 신청 시점에 이미 검증된 값이라 여기서 다시 Bean Validation을 하지 않는다. */
    private <T> T read(String payload, Class<T> type) {
        return jsonMapper.readValue(payload, type);
    }
}
