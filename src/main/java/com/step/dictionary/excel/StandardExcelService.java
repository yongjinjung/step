package com.step.dictionary.excel;

import static java.util.Map.entry;
import static java.util.stream.Collectors.joining;

import com.step.dictionary.auth.CurrentUser;
import com.step.dictionary.excel.dto.ExcelUploadResponse;
import com.step.dictionary.excel.support.ExcelErrors;
import com.step.dictionary.excel.support.ExcelReader;
import com.step.dictionary.excel.support.ExcelRow;
import com.step.dictionary.excel.support.ExcelWriter;
import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.request.ChangeRequestService;
import com.step.dictionary.request.RequestType;
import com.step.dictionary.request.TargetType;
import com.step.dictionary.request.dto.ChangeRequestForm;
import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.domain.DomainRepository;
import com.step.dictionary.standard.domain.DomainService;
import com.step.dictionary.standard.domain.dto.DomainRequest;
import com.step.dictionary.standard.domain.dto.DomainSearchCondition;
import com.step.dictionary.standard.term.Term;
import com.step.dictionary.standard.term.TermRepository;
import com.step.dictionary.standard.term.TermService;
import com.step.dictionary.standard.term.dto.TermRequest;
import com.step.dictionary.standard.term.dto.TermSearchCondition;
import com.step.dictionary.standard.word.Word;
import com.step.dictionary.standard.word.WordRepository;
import com.step.dictionary.standard.word.WordService;
import com.step.dictionary.standard.word.dto.WordRequest;
import com.step.dictionary.standard.word.dto.WordSearchCondition;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 표준 단어/도메인/용어 엑셀 다운로드와 업로드 (8단계).
 *
 * <p>업로드 규칙:
 * <ul>
 *   <li>등록만 한다. 이미 있는 이름이면 기존 서비스의 중복 오류가 난다.</li>
 *   <li>행마다 API 요청 본문과 같은 DTO를 만들어 같은 Bean Validation을 적용하고, 기존 서비스로 반영한다.
 *       그래서 업무 규칙과 변경 이력은 직접 API와 똑같다.</li>
 *   <li>참조 대상은 이름으로 지정한다 (도메인의 분류어 단어명, 용어의 구성 단어명과 도메인명). 이미 등록돼 있어야 한다.</li>
 *   <li>전부 반영하거나 전혀 반영하지 않는다. 모든 행을 끝까지 처리해 오류를 모은 뒤 하나라도 있으면 전체를 롤백한다.</li>
 *   <li>ADMIN은 바로 반영하고, USER는 행마다 등록 신청을 만든다. 신청은 형식 검증만 하므로 이름 중복 같은 문제는 승인할 때 드러난다.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class StandardExcelService {

    // ---- 열 번호 (ExcelSheet 헤더 순서와 같아야 한다) ----
    private static final int WORD_NAME = 0;
    private static final int WORD_ABBREVIATION = 1;
    private static final int WORD_ENGLISH_NAME = 2;
    private static final int WORD_CLASSIFIER = 3;
    private static final int WORD_DESCRIPTION = 4;

    private static final int DOMAIN_NAME = 0;
    private static final int DOMAIN_CLASSIFIER_WORD = 1;
    private static final int DOMAIN_DATA_TYPE = 2;
    private static final int DOMAIN_DATA_LENGTH = 3;
    private static final int DOMAIN_DATA_SCALE = 4;
    private static final int DOMAIN_DESCRIPTION = 5;

    private static final int TERM_WORDS = 0;
    private static final int TERM_DOMAIN = 1;
    private static final int TERM_DESCRIPTION = 2;

    // ---- 요청 DTO 필드 → 열 번호 (검증 오류를 어느 열에 표시할지) ----
    private static final Map<String, Integer> WORD_FIELDS = Map.of(
            "name", WORD_NAME, "abbreviation", WORD_ABBREVIATION, "englishName", WORD_ENGLISH_NAME,
            "classifier", WORD_CLASSIFIER, "description", WORD_DESCRIPTION);
    private static final Map<String, Integer> DOMAIN_FIELDS = Map.ofEntries(
            entry("name", DOMAIN_NAME), entry("classifierWordId", DOMAIN_CLASSIFIER_WORD),
            entry("dataType", DOMAIN_DATA_TYPE), entry("dataLength", DOMAIN_DATA_LENGTH),
            entry("dataScale", DOMAIN_DATA_SCALE), entry("description", DOMAIN_DESCRIPTION));
    private static final Map<String, Integer> TERM_FIELDS = Map.of(
            "wordIds", TERM_WORDS, "domainId", TERM_DOMAIN, "description", TERM_DESCRIPTION);

    /** 신청 사유 최대 길이 (ChangeRequestForm.reason). */
    private static final int MAX_REASON_LENGTH = 1000;

    private final WordRepository wordRepository;
    private final DomainRepository domainRepository;
    private final TermRepository termRepository;
    private final WordService wordService;
    private final DomainService domainService;
    private final TermService termService;
    /** USER 업로드를 신청으로 만들 때 쓴다. */
    private final ChangeRequestService requestService;
    /** 행별 요청 DTO 검증. 컨트롤러의 {@code @Valid}와 같은 검증기다. */
    private final Validator validator;

    public StandardExcelService(WordRepository wordRepository, DomainRepository domainRepository,
            TermRepository termRepository, WordService wordService, DomainService domainService,
            TermService termService, ChangeRequestService requestService, Validator validator) {
        this.wordRepository = wordRepository;
        this.domainRepository = domainRepository;
        this.termRepository = termRepository;
        this.wordService = wordService;
        this.domainService = domainService;
        this.termService = termService;
        this.requestService = requestService;
        this.validator = validator;
    }

    // ================= 다운로드 =================

    /** 단어 검색 결과를 엑셀로 만든다. 정렬은 목록 API와 같다(단어명 순). */
    public byte[] downloadWords(WordSearchCondition condition) {
        Page<Word> page = wordRepository.search(condition, ExcelWriter.downloadPage());
        ExcelWriter.requireWithinLimit(page);
        return ExcelWriter.write(ExcelSheet.WORD, page.getContent().stream()
                .map(word -> Arrays.<Object>asList(word.getName(), word.getAbbreviation(), word.getEnglishName(),
                        yesNo(word.isClassifier()), word.getDescription()))
                .toList());
    }

    /** 도메인 검색 결과를 엑셀로 만든다. 분류어는 단어명으로 쓴다. */
    public byte[] downloadDomains(DomainSearchCondition condition) {
        Page<Domain> page = domainRepository.search(condition, ExcelWriter.downloadPage());
        ExcelWriter.requireWithinLimit(page);
        return ExcelWriter.write(ExcelSheet.DOMAIN, page.getContent().stream()
                .map(domain -> Arrays.<Object>asList(domain.getName(), domain.getClassifierWord().getName(),
                        domain.getDataType().name(), domain.getDataLength(), domain.getDataScale(),
                        domain.getDescription()))
                .toList());
    }

    /** 용어 검색 결과를 엑셀로 만든다. 구성 단어는 단어명을 쉼표로 잇는다 (단어는 @BatchSize로 한꺼번에 조회). */
    public byte[] downloadTerms(TermSearchCondition condition) {
        Page<Term> page = termRepository.search(condition, ExcelWriter.downloadPage());
        ExcelWriter.requireWithinLimit(page);
        return ExcelWriter.write(ExcelSheet.TERM, page.getContent().stream()
                .map(term -> Arrays.<Object>asList(
                        term.getWords().stream().map(Word::getName).collect(joining(", ")),
                        term.getDomain().getName(), term.getDescription(), term.getName(), term.getAbbreviation()))
                .toList());
    }

    // ================= 업로드 =================

    /**
     * 단어를 업로드한다.
     *
     * @throws com.step.dictionary.excel.support.ExcelException 파일/헤더/행 수 오류, 또는 오류 행이 있을 때 (400, 전체 롤백)
     */
    @Transactional
    public ExcelUploadResponse uploadWords(MultipartFile file, CurrentUser user) {
        ExcelErrors errors = new ExcelErrors();
        List<ExcelRow> rows = ExcelReader.read(file, ExcelSheet.WORD, errors);
        List<Long> ids = new ArrayList<>();
        for (ExcelRow row : rows) {
            WordRequest request = new WordRequest(row.text(WORD_NAME), row.text(WORD_ABBREVIATION),
                    row.text(WORD_ENGLISH_NAME), row.text(WORD_DESCRIPTION), row.yesNo(WORD_CLASSIFIER));
            if (row.validate(validator, request, WORD_FIELDS)) {
                apply(row, user, file, TargetType.WORD, request, () -> wordService.create(request).id())
                        .ifPresent(ids::add);
            }
        }
        errors.throwIfAny();
        return ExcelUploadResponse.forStandardData(user, rows.size(), ids);
    }

    /**
     * 도메인을 업로드한다. 분류어 단어는 이미 등록돼 있어야 한다.
     *
     * @throws com.step.dictionary.excel.support.ExcelException 파일/헤더/행 수 오류, 또는 오류 행이 있을 때 (400, 전체 롤백)
     */
    @Transactional
    public ExcelUploadResponse uploadDomains(MultipartFile file, CurrentUser user) {
        ExcelErrors errors = new ExcelErrors();
        List<ExcelRow> rows = ExcelReader.read(file, ExcelSheet.DOMAIN, errors);
        List<Long> ids = new ArrayList<>();
        for (ExcelRow row : rows) {
            DomainRequest request = new DomainRequest(row.text(DOMAIN_NAME), findWordId(row, DOMAIN_CLASSIFIER_WORD),
                    row.enumValue(DOMAIN_DATA_TYPE, DataType.class), row.integer(DOMAIN_DATA_LENGTH),
                    row.integer(DOMAIN_DATA_SCALE), row.text(DOMAIN_DESCRIPTION));
            if (row.validate(validator, request, DOMAIN_FIELDS)) {
                apply(row, user, file, TargetType.DOMAIN, request, () -> domainService.create(request).id())
                        .ifPresent(ids::add);
            }
        }
        errors.throwIfAny();
        return ExcelUploadResponse.forStandardData(user, rows.size(), ids);
    }

    /**
     * 용어를 업로드한다. 구성 단어와 도메인은 이미 등록돼 있어야 한다. 용어명/영문약어(참고) 열은 무시한다.
     *
     * @throws com.step.dictionary.excel.support.ExcelException 파일/헤더/행 수 오류, 또는 오류 행이 있을 때 (400, 전체 롤백)
     */
    @Transactional
    public ExcelUploadResponse uploadTerms(MultipartFile file, CurrentUser user) {
        ExcelErrors errors = new ExcelErrors();
        List<ExcelRow> rows = ExcelReader.read(file, ExcelSheet.TERM, errors);
        List<Long> ids = new ArrayList<>();
        for (ExcelRow row : rows) {
            TermRequest request = new TermRequest(findWordIds(row), findDomainId(row), row.text(TERM_DESCRIPTION));
            if (row.validate(validator, request, TERM_FIELDS)) {
                apply(row, user, file, TargetType.TERM, request, () -> termService.create(request).id())
                        .ifPresent(ids::add);
            }
        }
        errors.throwIfAny();
        return ExcelUploadResponse.forStandardData(user, rows.size(), ids);
    }

    /**
     * 행 하나를 반영한다. ADMIN은 기존 서비스로 등록, USER는 등록 신청을 만든다.
     *
     * <p>업무 규칙 예외는 잡아서 행 오류로 남기고 다음 행을 계속 처리한다. 예외가 기존 서비스의 트랜잭션 경계를 지나며
     * 트랜잭션이 롤백 전용으로 표시되지만, 오류가 있으면 결국 {@link ExcelErrors#throwIfAny()}로 예외를 던져 롤백하므로
     * 커밋을 시도하는 일은 없다.
     *
     * @return 만들어진 대상 ID 또는 신청 ID. 실패하면 빈 값
     */
    private Optional<Long> apply(ExcelRow row, CurrentUser user, MultipartFile file, TargetType targetType,
            Object payload, Supplier<Long> directCreate) {
        try {
            if (user.admin()) {
                return Optional.of(directCreate.get());
            }
            ChangeRequestForm<Object> form = new ChangeRequestForm<>(
                    RequestType.CREATE, null, payload, requestReason(file, row));
            return Optional.of(requestService.create(targetType, form, user.username()).id());
        } catch (BusinessException e) {
            row.error(e.getErrorCode().getMessage());
            return Optional.empty();
        }
    }

    /** 단어명 열로 단어 ID를 찾는다. 비어 있으면 null(필수 검증이 처리), 없는 단어면 오류를 남기고 null. */
    private Long findWordId(ExcelRow row, int column) {
        String name = row.text(column);
        if (name == null) {
            return null;
        }
        return wordRepository.findByName(name).map(Word::getId).orElseGet(() -> {
            row.error(column, "단어 '" + name + "'을(를) 찾을 수 없습니다.");
            return null;
        });
    }

    /**
     * 쉼표로 구분한 단어명 목록을 조합 순서대로 단어 ID 목록으로 바꾼다.
     * 비어 있으면 null(필수 검증이 처리). 없는 단어는 모아서 한 번에 오류로 남긴다.
     */
    private List<Long> findWordIds(ExcelRow row) {
        String text = row.text(TERM_WORDS);
        if (text == null) {
            return null;
        }
        List<Long> ids = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .forEach(name -> wordRepository.findByName(name)
                        .ifPresentOrElse(word -> ids.add(word.getId()), () -> missing.add(name)));
        if (!missing.isEmpty()) {
            row.error(TERM_WORDS, "단어를 찾을 수 없습니다: " + String.join(", ", missing));
        }
        return ids;
    }

    /** 도메인명 열로 도메인 ID를 찾는다. 비어 있으면 null, 없는 도메인이면 오류를 남기고 null. */
    private Long findDomainId(ExcelRow row) {
        String name = row.text(TERM_DOMAIN);
        if (name == null) {
            return null;
        }
        return domainRepository.findByName(name).map(Domain::getId).orElseGet(() -> {
            row.error(TERM_DOMAIN, "도메인 '" + name + "'을(를) 찾을 수 없습니다.");
            return null;
        });
    }

    /** USER 업로드로 만든 신청의 사유. 어느 파일의 몇 행에서 왔는지 남긴다. */
    private static String requestReason(MultipartFile file, ExcelRow row) {
        String fileName = file.getOriginalFilename();
        String reason = "엑셀 업로드" + (StringUtils.hasText(fileName) ? " " + fileName : "") + " " + row.getRowNumber() + "행";
        return reason.length() > MAX_REASON_LENGTH ? reason.substring(0, MAX_REASON_LENGTH) : reason;
    }

    private static String yesNo(boolean value) {
        return value ? "Y" : "N";
    }
}
