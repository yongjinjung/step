package com.step.dictionary.meta.column.dto;

import com.step.dictionary.code.group.CodeGroup;
import com.step.dictionary.meta.column.ColumnIssue;
import com.step.dictionary.meta.column.MetaColumn;
import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.term.Term;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 컬럼 정의 응답. 컬럼 값과 함께 표준 검출 결과(expected, standard, issues)를 담는다.
 * 검출 결과는 저장된 값이 아니라 응답을 만들 때 {@link MetaColumn#inspect()}로 계산한다.
 *
 * @param id                컬럼 정의 ID
 * @param tableId           소속 테이블 ID
 * @param tablePhysicalName 소속 테이블 물리명 (비표준 검색 결과에서 어느 테이블인지 보여주기 위함)
 * @param seq               컬럼 순서
 * @param physicalName      물리명 (대문자)
 * @param logicalName       논리명
 * @param dataType          데이터 타입
 * @param dataLength        길이 (null 가능)
 * @param dataScale         소수점 자리수 (null 가능)
 * @param nullable          NULL 허용 여부
 * @param primaryKey        기본키 여부
 * @param termId            매핑 용어 ID (없으면 null)
 * @param termName          매핑 용어명 (없으면 null)
 * @param codeGroupId       매핑 공통 코드 그룹 ID (없으면 null)
 * @param codeId            매핑 코드 그룹의 코드ID (없으면 null)
 * @param codeName          매핑 코드 그룹의 코드명 (없으면 null)
 * @param expected          매핑된 용어/도메인 기준의 표준 값. 용어가 없으면 null
 * @param standard          비표준 사유(issues)가 없으면 true
 * @param issues            비표준 사유 목록. 표준이면 빈 배열
 * @param createdAt         생성 일시
 * @param updatedAt         최종 수정 일시
 */
public record MetaColumnResponse(
        Long id,
        Long tableId,
        String tablePhysicalName,
        int seq,
        String physicalName,
        String logicalName,
        DataType dataType,
        Integer dataLength,
        Integer dataScale,
        boolean nullable,
        boolean primaryKey,
        Long termId,
        String termName,
        Long codeGroupId,
        String codeId,
        String codeName,
        StandardSpec expected,
        boolean standard,
        List<ColumnIssue> issues,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 표준 기준 값. 컬럼이 표준이 되려면 가져야 할 값이며, 화면에서 실제 값과 나란히 비교하는 데 쓴다.
     *
     * @param physicalName 용어 영문약어
     * @param dataType     도메인 데이터 타입
     * @param dataLength   도메인 길이
     * @param dataScale    도메인 소수점 자리수
     */
    public record StandardSpec(String physicalName, DataType dataType, Integer dataLength, Integer dataScale) {

        static StandardSpec from(Term term) {
            Domain domain = term.getDomain();
            return new StandardSpec(
                    term.getAbbreviation(), domain.getDataType(), domain.getDataLength(), domain.getDataScale());
        }
    }

    /**
     * 엔티티를 응답으로 변환한다.
     * 테이블, 용어, 도메인, 코드 그룹을 읽으므로 트랜잭션 안에서 호출하거나 미리 fetch해 둬야 한다 (open-in-view가 꺼져 있다).
     */
    public static MetaColumnResponse from(MetaColumn column) {
        Term term = column.getTerm();
        CodeGroup codeGroup = column.getCodeGroup();
        // standard와 issues가 같은 결과를 쓰도록 한 번만 계산한다.
        List<ColumnIssue> issues = column.inspect();
        return new MetaColumnResponse(
                column.getId(),
                column.getTable().getId(),
                column.getTable().getPhysicalName(),
                column.getSeq(),
                column.getPhysicalName(),
                column.getLogicalName(),
                column.getDataType(),
                column.getDataLength(),
                column.getDataScale(),
                column.isNullable(),
                column.isPrimaryKey(),
                term == null ? null : term.getId(),
                term == null ? null : term.getName(),
                codeGroup == null ? null : codeGroup.getId(),
                codeGroup == null ? null : codeGroup.getCodeId(),
                codeGroup == null ? null : codeGroup.getCodeName(),
                term == null ? null : StandardSpec.from(term),
                issues.isEmpty(),
                issues,
                column.getCreatedAt(),
                column.getUpdatedAt());
    }
}
