package com.step.dictionary.standard.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.global.config.MyBatisConfig;
import com.step.dictionary.standard.domain.dto.DomainSearchCondition;
import com.step.dictionary.standard.word.Word;
import com.step.dictionary.standard.word.WordRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({MyBatisConfig.class, DomainRepository.class, WordRepository.class})
class DomainRepositoryTest {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private WordRepository wordRepository;

    private Word number;

    @BeforeEach
    void setUp() {
        number = wordRepository.save(new Word("번호", "NO", "Number", null, true));
        Word name = wordRepository.save(new Word("명", "NM", "Name", null, true));
        domainRepository.save(new Domain("번호V20", number, DataType.VARCHAR, 20, null, null));
        domainRepository.save(new Domain("번호N10", number, DataType.DECIMAL, 10, null, null));
        domainRepository.save(new Domain("명V100", name, DataType.VARCHAR, 100, null, null));
    }

    @Test
    @DisplayName("키워드는 도메인명에 대소문자 구분 없이 부분 일치한다")
    void searchByKeyword() {
        assertThat(searchNames("v20", null, null)).containsExactly("번호V20");
    }

    @Test
    @DisplayName("분류어 단어와 데이터 타입으로 필터링하고 도메인명 오름차순으로 정렬한다")
    void searchByClassifierWordAndDataType() {
        assertThat(searchNames(null, number.getId(), null)).containsExactly("번호N10", "번호V20");
        assertThat(searchNames(null, null, DataType.VARCHAR)).containsExactly("명V100", "번호V20");
        assertThat(searchNames(null, number.getId(), DataType.VARCHAR)).containsExactly("번호V20");
    }

    @Test
    @DisplayName("단어가 도메인의 분류어로 쓰이는지 확인한다")
    void existsByClassifierWordId() {
        Word customer = wordRepository.save(new Word("고객", "CUST", "Customer", null, false));

        assertThat(domainRepository.existsByClassifierWordId(number.getId())).isTrue();
        assertThat(domainRepository.existsByClassifierWordId(customer.getId())).isFalse();
    }

    private List<String> searchNames(String keyword, Long classifierWordId, DataType dataType) {
        return domainRepository
                .search(new DomainSearchCondition(keyword, classifierWordId, dataType), PageRequest.of(0, 20))
                .map(Domain::getName)
                .getContent();
    }
}
