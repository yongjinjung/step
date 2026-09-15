package com.step.dictionary.standard.term;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.global.config.JpaConfig;
import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.domain.DomainRepository;
import com.step.dictionary.standard.term.dto.TermSearchCondition;
import com.step.dictionary.standard.word.Word;
import com.step.dictionary.standard.word.WordRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaConfig.class)
class TermRepositoryTest {

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private EntityManager entityManager;

    private Word customer;
    private Word account;
    private Word number;
    private Domain numberDomain;
    private Term customerNumber;

    @BeforeEach
    void setUp() {
        customer = wordRepository.save(new Word("고객", "CUST", "Customer", null, false));
        account = wordRepository.save(new Word("계좌", "ACCT", "Account", null, false));
        number = wordRepository.save(new Word("번호", "NO", "Number", null, true));
        Word name = wordRepository.save(new Word("명", "NM", "Name", null, true));
        numberDomain = domainRepository.save(new Domain("번호V20", number, DataType.VARCHAR, 20, null, null));
        Domain nameDomain = domainRepository.save(new Domain("명V100", name, DataType.VARCHAR, 100, null, null));

        customerNumber = termRepository.save(new Term(List.of(customer, number), numberDomain, null));
        termRepository.save(new Term(List.of(account, number), numberDomain, null));
        termRepository.save(new Term(List.of(customer, name), nameDomain, null));
    }

    @Test
    @DisplayName("키워드는 용어명과 영문약어에 부분 일치하며 영문약어는 대소문자를 구분하지 않는다")
    void searchByKeyword() {
        assertThat(searchNames("cust", null, null)).containsExactly("고객명", "고객번호");
        assertThat(searchNames("계좌", null, null)).containsExactly("계좌번호");
    }

    @Test
    @DisplayName("도메인과 구성 단어로 필터링하고 용어명 오름차순으로 정렬한다")
    void searchByDomainAndWord() {
        assertThat(searchNames(null, numberDomain.getId(), null)).containsExactly("계좌번호", "고객번호");
        assertThat(searchNames(null, null, customer.getId())).containsExactly("고객명", "고객번호");
        assertThat(searchNames(null, numberDomain.getId(), customer.getId())).containsExactly("고객번호");
    }

    @Test
    @DisplayName("단어와 도메인이 용어에서 쓰이는지 확인한다")
    void existsByReference() {
        Word code = wordRepository.save(new Word("코드", "CD", "Code", null, true));

        assertThat(termRepository.existsByWordsId(customer.getId())).isTrue();
        assertThat(termRepository.existsByWordsId(code.getId())).isFalse();
        assertThat(termRepository.existsByDomainId(numberDomain.getId())).isTrue();
    }

    @Test
    @DisplayName("구성 단어를 늘리거나 줄여도 순서가 유지되고 seq는 1부터 저장된다")
    void updateWordsKeepsOrder() {
        Long id = customerNumber.getId();

        customerNumber.update(List.of(customer, account, number), numberDomain, null);
        flushAndClear();
        assertThat(wordNames(id)).containsExactly("고객", "계좌", "번호");
        assertThat(seqs(id)).containsExactly(1, 2, 3);

        termRepository.findById(id).orElseThrow().update(List.of(account, customer, number), numberDomain, null);
        flushAndClear();
        assertThat(wordNames(id)).containsExactly("계좌", "고객", "번호");

        termRepository.findById(id).orElseThrow().update(List.of(customer, number), numberDomain, null);
        flushAndClear();
        assertThat(wordNames(id)).containsExactly("고객", "번호");
        assertThat(seqs(id)).containsExactly(1, 2);
    }

    private List<String> searchNames(String keyword, Long domainId, Long wordId) {
        return termRepository
                .search(new TermSearchCondition(keyword, domainId, wordId), PageRequest.of(0, 20))
                .map(Term::getName)
                .getContent();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private List<String> wordNames(Long termId) {
        return termRepository.findById(termId).orElseThrow().getWords().stream().map(Word::getName).toList();
    }

    private List<Integer> seqs(Long termId) {
        List<?> rows = entityManager
                .createNativeQuery("SELECT seq FROM std_term_word WHERE term_id = :termId ORDER BY seq")
                .setParameter("termId", termId)
                .getResultList();
        return rows.stream().map(seq -> ((Number) seq).intValue()).toList();
    }
}
