package com.step.dictionary.history;

/** 변경 종류. 종류에 따라 변경 전/후 스냅샷의 존재 여부가 정해진다. */
public enum ChangeType {

    /** 등록. 변경 전 스냅샷이 없다. */
    CREATE,
    /** 수정. 전/후 스냅샷이 모두 있고 서로 다르다. */
    UPDATE,
    /** 삭제. 변경 후 스냅샷이 없다. */
    DELETE
}
