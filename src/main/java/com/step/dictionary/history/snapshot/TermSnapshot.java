package com.step.dictionary.history.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.step.dictionary.history.HistoryTargetType;
import com.step.dictionary.standard.term.Term;
import com.step.dictionary.standard.word.Word;
import java.util.List;

/**
 * 표준 용어 스냅샷.
 *
 * @param id           용어 ID
 * @param name         용어명
 * @param abbreviation 영문약어
 * @param wordIds      구성 단어 ID (조합 순서대로)
 * @param domainId     도메인 ID
 * @param domainName   도메인명 (읽기용)
 * @param description  설명
 */
public record TermSnapshot(Long id, String name, String abbreviation, List<Long> wordIds, Long domainId,
        String domainName, String description) implements HistorySnapshot {

    /**
     * 엔티티의 현재 값으로 스냅샷을 뜬다. 구성 단어와 도메인을 읽으므로 트랜잭션 안에서 호출한다.
     * 단어 목록은 새 리스트로 복사해, 이후 {@code term.update()}가 목록을 바꿔도 스냅샷이 흔들리지 않는다.
     * ({@code List.copyOf}는 null 요소를 허용하지 않아, 저장 전 단어가 섞일 수 있는 테스트를 위해 {@code toList()}를 쓴다.)
     */
    public static TermSnapshot from(Term term) {
        return new TermSnapshot(term.getId(), term.getName(), term.getAbbreviation(),
                term.getWords().stream().map(Word::getId).toList(),
                term.getDomain().getId(), term.getDomain().getName(), term.getDescription());
    }

    @Override
    @JsonIgnore
    public HistoryTargetType targetType() {
        return HistoryTargetType.TERM;
    }
}
