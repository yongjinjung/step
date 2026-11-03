package com.step.dictionary.standard.word;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.global.config.MyBatisConfig;
import com.step.dictionary.standard.word.dto.WordSearchCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({MyBatisConfig.class, WordRepository.class})
class WordRepositoryTest {

    @Autowired
    private WordRepository wordRepository;

    @BeforeEach
    void setUp() {
        wordRepository.save(new Word("고객", "CUST", "Customer", null, false));
        wordRepository.save(new Word("번호", "NO", "Number", null, true));
        wordRepository.save(new Word("일자", "DT", "Date", null, true));
    }

    @Test
    @DisplayName("키워드는 단어명, 영문약어, 영문명에 부분 일치하며 영문은 대소문자를 구분하지 않는다")
    void searchByKeyword() {
        assertThat(search("고객", null).getContent()).extracting(Word::getName).containsExactly("고객");
        assertThat(search("cus", null).getContent()).extracting(Word::getName).containsExactly("고객");
        assertThat(search("numb", null).getContent()).extracting(Word::getName).containsExactly("번호");
    }

    @Test
    @DisplayName("분류어 여부로 필터링하고 단어명 오름차순으로 정렬한다")
    void searchByClassifier() {
        Page<Word> result = search(null, true);

        assertThat(result.getContent()).extracting(Word::getName).containsExactly("번호", "일자");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("페이징 시 전체 건수는 조건에 맞는 전체 기준이다")
    void searchWithPaging() {
        Page<Word> result = wordRepository.search(new WordSearchCondition(null, null), PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    private Page<Word> search(String keyword, Boolean classifier) {
        return wordRepository.search(new WordSearchCondition(keyword, classifier), PageRequest.of(0, 20));
    }
}
