package com.step.dictionary.standard.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.word.Word;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TermTest {

    private final Word customer = new Word("고객", "CUST", "Customer", null, false);
    private final Word account = new Word("계좌", "ACCT", "Account", null, false);
    private final Word number = new Word("번호", "NO", "Number", null, true);
    private final Domain numberDomain = new Domain("번호V20", number, DataType.VARCHAR, 20, null, null);

    @Test
    @DisplayName("용어명은 단어명을 잇고 영문약어는 단어 약어를 _로 이어 생성한다")
    void compose() {
        Term term = new Term(List.of(customer, account, number), numberDomain, null);

        assertThat(term.getName()).isEqualTo("고객계좌번호");
        assertThat(term.getAbbreviation()).isEqualTo("CUST_ACCT_NO");
        assertThat(term.getWords()).containsExactly(customer, account, number);
    }

    @Test
    @DisplayName("구성 단어를 바꾸면 용어명과 영문약어를 다시 생성한다")
    void update() {
        Term term = new Term(List.of(customer, account, number), numberDomain, null);

        term.update(List.of(account, number), numberDomain, "계좌 식별 번호");

        assertThat(term.getName()).isEqualTo("계좌번호");
        assertThat(term.getAbbreviation()).isEqualTo("ACCT_NO");
        assertThat(term.getWords()).containsExactly(account, number);
        assertThat(term.getDescription()).isEqualTo("계좌 식별 번호");
    }

    @Test
    @DisplayName("구성 단어 목록은 밖에서 변경할 수 없다")
    void wordsAreUnmodifiable() {
        Term term = new Term(List.of(customer, number), numberDomain, null);

        assertThatThrownBy(() -> term.getWords().add(account))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
