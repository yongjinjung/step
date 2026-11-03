package com.step.dictionary.pharmacy.prescription.dto;

import com.step.dictionary.pharmacy.prescription.PrescriptionStatus;
import com.step.dictionary.pharmacy.prescription.PrescriptionType;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 처방 검색 조건. 쿼리 파라미터가 record 생성자로 바인딩된다. 모든 조건은 선택이다.
 *
 * @param keyword          처방전번호·환자번호·환자명 앞부분 일치 (대소문자 구분 없음, 인덱스를 타도록 중간 글자는 찾지 않는다)
 * @param status           처방 상태
 * @param prescriptionType 처방 구분
 * @param departmentCode   진료과 코드 (대소문자 구분 없이 일치)
 * @param from             처방일 시작 (yyyy-MM-dd, 포함)
 * @param to               처방일 종료 (yyyy-MM-dd, 포함)
 */
public record PrescriptionSearchCondition(
        String keyword,
        PrescriptionStatus status,
        PrescriptionType prescriptionType,
        String departmentCode,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {
}
