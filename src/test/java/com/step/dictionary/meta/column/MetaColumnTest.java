package com.step.dictionary.meta.column;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.meta.table.MetaTable;
import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.term.Term;
import com.step.dictionary.standard.word.Word;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MetaColumnTest {

    private final MetaTable table = new MetaTable("TB_CUSTOMER", "고객", null);
    private final Word customer = new Word("고객", "CUST", "Customer", null, false);
    private final Word number = new Word("번호", "NO", "Number", null, true);
    private final Word amount = new Word("금액", "AMT", "Amount", null, true);
    /** 고객번호 / CUST_NO / VARCHAR(20) */
    private final Term customerNumber = new Term(
            List.of(customer, number), new Domain("번호V20", number, DataType.VARCHAR, 20, null, null), null);
    /** 고객금액 / CUST_AMT / DECIMAL(15,0) */
    private final Term customerAmount = new Term(
            List.of(customer, amount), new Domain("금액N15", amount, DataType.DECIMAL, 15, null, null), null);

    @Test
    @DisplayName("물리명은 대문자로 저장하고, 용어 영문약어와 도메인 규칙이 모두 같으면 표준이다")
    void standard() {
        MetaColumn column = column("cust_no", customerNumber, DataType.VARCHAR, 20, null);

        assertThat(column.getPhysicalName()).isEqualTo("CUST_NO");
        assertThat(column.inspect()).isEmpty();
    }

    @Test
    @DisplayName("용어가 매핑되지 않으면 NO_TERM만 검출한다")
    void noTerm() {
        MetaColumn column = column("ETC_TXT", null, DataType.VARCHAR, 100, null);

        assertThat(column.inspect()).containsExactly(ColumnIssue.NO_TERM);
    }

    @Test
    @DisplayName("물리명이 용어 영문약어와 다르면 NAME_MISMATCH를 검출한다")
    void nameMismatch() {
        MetaColumn column = column("CUSTOMER_NO", customerNumber, DataType.VARCHAR, 20, null);

        assertThat(column.inspect()).containsExactly(ColumnIssue.NAME_MISMATCH);
    }

    @Test
    @DisplayName("타입과 길이가 도메인과 다르면 각각 검출한다")
    void typeAndLengthMismatch() {
        MetaColumn column = column("CUST_NO", customerNumber, DataType.CHAR, 10, null);

        assertThat(column.inspect()).containsExactly(ColumnIssue.TYPE_MISMATCH, ColumnIssue.LENGTH_MISMATCH);
    }

    @Test
    @DisplayName("DECIMAL 소수점을 생략하면 0으로 보고 도메인과 비교한다")
    void decimalScale() {
        assertThat(column("CUST_AMT", customerAmount, DataType.DECIMAL, 15, null).inspect()).isEmpty();
        assertThat(column("CUST_AMT", customerAmount, DataType.DECIMAL, 15, 2).inspect())
                .containsExactly(ColumnIssue.LENGTH_MISMATCH);
    }

    @Test
    @DisplayName("매핑된 용어의 구성이 바뀌면 다시 점검할 때 결과에 반영된다")
    void reflectsTermChange() {
        MetaColumn column = column("CUST_NO", customerNumber, DataType.VARCHAR, 20, null);

        customerNumber.update(List.of(amount, number), customerNumber.getDomain(), null);

        assertThat(column.inspect()).containsExactly(ColumnIssue.NAME_MISMATCH);
    }

    private MetaColumn column(String physicalName, Term term, DataType dataType, Integer length, Integer scale) {
        return new MetaColumn(table, physicalName, "논리명", term, dataType, length, scale, true, false, 1);
    }
}
