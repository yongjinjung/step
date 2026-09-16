package com.step.dictionary.code.group;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.code.group.dto.CodeGroupSearchCondition;
import com.step.dictionary.global.config.JpaConfig;
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
class CodeGroupRepositoryTest {

    @Autowired
    private CodeGroupRepository groupRepository;

    @BeforeEach
    void setUp() {
        groupRepository.save(new CodeGroup("PAY_TYPE", "결제수단", null, true));
        groupRepository.save(new CodeGroup("ORDER_STAT", "주문상태", null, true));
        groupRepository.save(new CodeGroup("OLD_TYPE", "옛 구분", null, false));
    }

    @Test
    @DisplayName("키워드는 코드ID(대소문자 무시)와 코드명에 부분 일치한다")
    void searchByKeyword() {
        assertThat(search("order", null)).extracting(CodeGroup::getCodeId).containsExactly("ORDER_STAT");
        assertThat(search("결제", null)).extracting(CodeGroup::getCodeId).containsExactly("PAY_TYPE");
        assertThat(search("type", null)).extracting(CodeGroup::getCodeId).containsExactly("OLD_TYPE", "PAY_TYPE");
    }

    @Test
    @DisplayName("사용 여부로 거르고 코드ID 오름차순으로 정렬한다")
    void searchByUsed() {
        assertThat(search(null, true)).extracting(CodeGroup::getCodeId).containsExactly("ORDER_STAT", "PAY_TYPE");
        assertThat(search(null, false)).extracting(CodeGroup::getCodeId).containsExactly("OLD_TYPE");
    }

    private List<CodeGroup> search(String keyword, Boolean used) {
        return groupRepository.search(new CodeGroupSearchCondition(keyword, used), PageRequest.of(0, 20)).getContent();
    }
}
