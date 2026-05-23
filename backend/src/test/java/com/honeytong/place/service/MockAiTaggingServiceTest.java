package com.honeytong.place.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MockAiTaggingServiceTest {

    private final MockAiTaggingService mockAiTaggingService = new MockAiTaggingService();

    @Test
    @DisplayName("텍스트 분석을 통해 적절한 규칙 기반 태그들이 반환되어야 한다")
    void generateTagsTest() {
        // given
        String placeName = "은하수 분식";
        String description = "여기 가성비가 진짜 훌륭하고 혼자 먹기 좋은 혼밥 식당이에요. 떡볶이도 매워요.";
        List<String> comments = List.of("맛있는 디저트 카페 느낌의 빵도 파네요.");

        // when
        List<String> tags = mockAiTaggingService.generateTags(placeName, description, comments);

        // then
        assertThat(tags).containsExactlyInAnyOrder(
                "혼밥하기좋은",
                "가성비맛집",
                "디저트천국",
                "매운맛마니아"
        );
    }

    @Test
    @DisplayName("아무 키워드도 매칭되지 않으면 폴백 태그가 반환되어야 한다")
    void fallbackTagTest() {
        // given
        String placeName = "김밥천국";
        String description = "평범한 식당";
        List<String> comments = List.of("무난합니다.");

        // when
        List<String> tags = mockAiTaggingService.generateTags(placeName, description, comments);

        // then
        assertThat(tags).containsExactly("숨겨진동네맛집");
    }
}
