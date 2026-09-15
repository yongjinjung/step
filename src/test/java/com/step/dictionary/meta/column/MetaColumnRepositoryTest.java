package com.step.dictionary.meta.column;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.global.config.JpaConfig;
import com.step.dictionary.meta.column.dto.NonStandardColumnCondition;
import com.step.dictionary.meta.table.MetaTable;
import com.step.dictionary.meta.table.MetaTableRepository;
import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.Domain;
import com.step.dictionary.standard.domain.DomainRepository;
import com.step.dictionary.standard.term.Term;
import com.step.dictionary.standard.term.TermRepository;
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
class MetaColumnRepositoryTest {

    @Autowired
    private MetaColumnRepository columnRepository;

    @Autowired
    private MetaTableRepository tableRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private EntityManager entityManager;

    private MetaTable customerTable;
    private MetaTable orderTable;
    private Term customerNumber;

    /**
     * TB_CUSTOMER: CUST_NO(표준), CUST_AMT(표준), ETC_TXT(NO_TERM), CUSTOMER_NO(NAME_MISMATCH)
     * TB_ORDER:    CUST_NO(TYPE_MISMATCH), CUST_AMT(LENGTH_MISMATCH)
     */
    @BeforeEach
    void setUp() {
        Word customer = wordRepository.save(new Word("고객", "CUST", "Customer", null, false));
        Word number = wordRepository.save(new Word("번호", "NO", "Number", null, true));
        Word amount = wordRepository.save(new Word("금액", "AMT", "Amount", null, true));
        Domain numberDomain = domainRepository.save(new Domain("번호V20", number, DataType.VARCHAR, 20, null, null));
        Domain amountDomain = domainRepository.save(new Domain("금액N15", amount, DataType.DECIMAL, 15, null, null));
        customerNumber = termRepository.save(new Term(List.of(customer, number), numberDomain, null));
        Term customerAmount = termRepository.save(new Term(List.of(customer, amount), amountDomain, null));

        customerTable = tableRepository.save(new MetaTable("TB_CUSTOMER", "고객", null));
        orderTable = tableRepository.save(new MetaTable("TB_ORDER", "주문", null));

        saveColumn(customerTable, "CUST_NO", customerNumber, DataType.VARCHAR, 20, null, 1);
        saveColumn(customerTable, "CUST_AMT", customerAmount, DataType.DECIMAL, 15, null, 2);
        saveColumn(customerTable, "ETC_TXT", null, DataType.VARCHAR, 100, null, 3);
        saveColumn(customerTable, "CUSTOMER_NO", customerNumber, DataType.VARCHAR, 20, null, 4);
        saveColumn(orderTable, "CUST_NO", customerNumber, DataType.CHAR, 20, null, 1);
        saveColumn(orderTable, "CUST_AMT", customerAmount, DataType.DECIMAL, 15, 2, 2);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("비표준 컬럼을 테이블 물리명, 컬럼 순서로 검색한다")
    void searchNonStandard() {
        assertThat(searchNames(null, null)).containsExactly(
                "TB_CUSTOMER.ETC_TXT", "TB_CUSTOMER.CUSTOMER_NO", "TB_ORDER.CUST_NO", "TB_ORDER.CUST_AMT");
    }

    @Test
    @DisplayName("사유별로 검색하면 그 사유에 해당하는 컬럼만 나온다")
    void searchByIssue() {
        assertThat(searchNames(null, ColumnIssue.NO_TERM)).containsExactly("TB_CUSTOMER.ETC_TXT");
        assertThat(searchNames(null, ColumnIssue.NAME_MISMATCH)).containsExactly("TB_CUSTOMER.CUSTOMER_NO");
        assertThat(searchNames(null, ColumnIssue.TYPE_MISMATCH)).containsExactly("TB_ORDER.CUST_NO");
        assertThat(searchNames(null, ColumnIssue.LENGTH_MISMATCH)).containsExactly("TB_ORDER.CUST_AMT");
        assertThat(searchNames(orderTable.getId(), null)).containsExactly("TB_ORDER.CUST_NO", "TB_ORDER.CUST_AMT");
    }

    @Test
    @DisplayName("DB 검색 조건과 엔티티 점검 결과가 사유별로 일치한다")
    void searchAgreesWithInspect() {
        List<MetaColumn> all = columnRepository.findAll();

        for (ColumnIssue issue : ColumnIssue.values()) {
            List<String> inspected = all.stream()
                    .filter(column -> column.inspect().contains(issue))
                    .map(MetaColumnRepositoryTest::name)
                    .toList();
            assertThat(searchNames(null, issue)).as(issue.name()).containsExactlyInAnyOrderElementsOf(inspected);
        }
        List<String> nonStandard = all.stream()
                .filter(column -> !column.inspect().isEmpty())
                .map(MetaColumnRepositoryTest::name)
                .toList();
        assertThat(searchNames(null, null)).containsExactlyInAnyOrderElementsOf(nonStandard);
    }

    @Test
    @DisplayName("테이블의 컬럼을 순서대로 조회한다")
    void findAllByTable() {
        assertThat(columnRepository.findAllByTableIdOrderBySeqAscIdAsc(customerTable.getId()))
                .extracting(MetaColumn::getPhysicalName)
                .containsExactly("CUST_NO", "CUST_AMT", "ETC_TXT", "CUSTOMER_NO");
    }

    @Test
    @DisplayName("용어를 매핑한 컬럼이 하나라도 남아 있으면 용어 참조 중으로 본다")
    void existsByTermId() {
        columnRepository.deleteAll(columnRepository.findAllByTableIdOrderBySeqAscIdAsc(customerTable.getId()));
        assertThat(columnRepository.existsByTermId(customerNumber.getId())).isTrue();

        columnRepository.deleteAll(columnRepository.findAllByTableIdOrderBySeqAscIdAsc(orderTable.getId()));
        assertThat(columnRepository.existsByTermId(customerNumber.getId())).isFalse();
    }

    private void saveColumn(MetaTable table, String physicalName, Term term, DataType dataType,
            Integer length, Integer scale, int seq) {
        columnRepository.save(new MetaColumn(table, physicalName, physicalName, term, dataType, length, scale,
                true, false, seq));
    }

    private List<String> searchNames(Long tableId, ColumnIssue issue) {
        return columnRepository
                .searchNonStandard(new NonStandardColumnCondition(tableId, issue), PageRequest.of(0, 20))
                .map(MetaColumnRepositoryTest::name)
                .getContent();
    }

    private static String name(MetaColumn column) {
        return column.getTable().getPhysicalName() + "." + column.getPhysicalName();
    }
}
