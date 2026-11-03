package com.step.dictionary.pharmacy.drugreturn;

/** 반납 품목의 처리 방법. */
public enum ReturnDisposition {

    /** 재입고 (다시 쓸 수 있는 약). */
    RESTOCK,
    /** 폐기 (개봉·오염·만료 등). */
    DISPOSE
}
