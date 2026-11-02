package com.step.dictionary.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuInitializerTest {

    @Mock
    private MenuRepository menuRepository;

    @InjectMocks
    private MenuInitializer initializer;

    @Test
    @DisplayName("메뉴가 하나도 없으면 기본 메뉴(상위 5개, 하위 20개)를 만든다")
    void createWhenEmpty() {
        given(menuRepository.count()).willReturn(0L);
        given(menuRepository.save(any(Menu.class))).willAnswer(invocation -> invocation.getArgument(0));

        initializer.run(null);

        ArgumentCaptor<Menu> saved = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository, times(25)).save(saved.capture());
        Menu first = saved.getAllValues().get(0);
        assertThat(first.getName()).isEqualTo("표준 사전");
        assertThat(first.isTop()).isTrue();
        assertThat(first.getPath()).isNull();
        // 상위 메뉴 다음에 그 하위 메뉴가 이어서 저장된다.
        Menu second = saved.getAllValues().get(1);
        assertThat(second.getName()).isEqualTo("표준 단어");
        assertThat(second.getPath()).isEqualTo("/words");
        assertThat(second.getParent()).isSameAs(first);
        // 관리 메뉴(역할 관리 포함)도 만든다. 누가 볼 수 있는지는 역할 권한이 정한다.
        assertThat(saved.getAllValues()).extracting(Menu::getPath)
                .contains("/users", "/roles", "/menus", "/menu-permissions");
        // 초급 개발자 온보딩용 샘플 화면도 만든다.
        assertThat(saved.getAllValues()).extracting(Menu::getPath)
                .contains("/react", "/typescript", "/vite", "/antdesign", "/router", "/data-fetching", "/forms",
                        "/backend-guide");
    }

    @Test
    @DisplayName("메뉴가 이미 있으면 아무것도 하지 않는다 (관리자가 고친 메뉴를 건드리지 않는다)")
    void skipWhenMenusExist() {
        given(menuRepository.count()).willReturn(3L);

        initializer.run(null);

        verify(menuRepository, never()).save(any());
    }
}
