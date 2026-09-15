package com.step.dictionary.excel.dto;

import com.step.dictionary.auth.CurrentUser;
import java.util.List;

/**
 * 업로드 성공 응답.
 *
 * @param mode     처리 방식
 * @param rowCount 처리한 데이터 행 수
 * @param ids      만들어진 대상 ID. APPLIED면 단어/도메인/용어 ID(테이블 정의는 테이블 ID), REQUESTED면 신청 ID
 */
public record ExcelUploadResponse(Mode mode, int rowCount, List<Long> ids) {

    /** 처리 방식. */
    public enum Mode {
        /** 데이터에 바로 반영했다 (ADMIN의 표준 데이터 업로드, 모든 사용자의 테이블 정의 업로드). */
        APPLIED,
        /** 행마다 등록 신청을 만들었다 (USER의 표준 데이터 업로드). 관리자가 승인해야 반영된다. */
        REQUESTED
    }

    /** 바로 반영한 결과. */
    public static ExcelUploadResponse applied(int rowCount, List<Long> ids) {
        return new ExcelUploadResponse(Mode.APPLIED, rowCount, ids);
    }

    /** 표준 데이터 업로드 결과. 관리자면 반영, 아니면 신청이다. */
    public static ExcelUploadResponse forStandardData(CurrentUser user, int rowCount, List<Long> ids) {
        return new ExcelUploadResponse(user.admin() ? Mode.APPLIED : Mode.REQUESTED, rowCount, ids);
    }
}
