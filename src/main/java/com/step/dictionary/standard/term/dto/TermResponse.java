package com.step.dictionary.standard.term.dto;

import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.term.Term;
import com.step.dictionary.standard.word.Word;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 용어 응답. 도메인의 타입/길이를 펼쳐 담아 용어만 봐도 컬럼 스펙을 알 수 있게 한다.
 * 도메인 값은 저장된 복사본이 아니라 조회 시점의 도메인 값이므로 도메인 변경이 바로 반영된다.
 *
 * @param id           용어 ID
 * @param name         용어명 (예: 고객번호)
 * @param abbreviation 영문약어 (예: CUST_NO)
 * @param words        구성 단어 (조합 순서대로)
 * @param domainId     도메인 ID
 * @param domainName   도메인명
 * @param dataType     도메인 데이터 타입
 * @param dataLength   도메인 길이 (null 가능)
 * @param dataScale    도메인 소수점 자리수 (null 가능)
 * @param description  설명 (null 가능)
 * @param createdAt    생성 일시
 * @param updatedAt    최종 수정 일시
 */
public record TermResponse(
        Long id,
        String name,
        String abbreviation,
        List<TermWord> words,
        Long domainId,
        String domainName,
        DataType dataType,
        Integer dataLength,
        Integer dataScale,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 구성 단어 요약.
     *
     * @param id           단어 ID
     * @param name         단어명
     * @param abbreviation 단어 영문약어
     */
    public record TermWord(Long id, String name, String abbreviation) {

        static TermWord from(Word word) {
            return new TermWord(word.getId(), word.getName(), word.getAbbreviation());
        }
    }

    /**
     * 엔티티를 응답으로 변환한다.
     * 지연 로딩인 구성 단어와 도메인을 읽으므로 트랜잭션 안에서 호출해야 한다 (open-in-view가 꺼져 있다).
     * 목록 조회에서는 도메인은 fetch join, 단어는 @BatchSize로 한꺼번에 가져온다.
     */
    public static TermResponse from(Term term) {
        Domain domain = term.getDomain();
        return new TermResponse(
                term.getId(),
                term.getName(),
                term.getAbbreviation(),
                term.getWords().stream().map(TermWord::from).toList(),
                domain.getId(),
                domain.getName(),
                domain.getDataType(),
                domain.getDataLength(),
                domain.getDataScale(),
                term.getDescription(),
                term.getCreatedAt(),
                term.getUpdatedAt());
    }
}
