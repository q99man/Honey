package com.honeytong.place.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiTaggingService implements AiTaggingService {

    @Override
    public List<String> generateTags(String placeName, String description, List<String> comments) {
        List<String> tags = new ArrayList<>();
        String combined = (placeName + " " + (description != null ? description : "") + " " + String.join(" ", comments)).toLowerCase();

        if (combined.contains("혼밥") || combined.contains("혼자")) {
            tags.add("혼밥하기좋은");
        }
        if (combined.contains("가성비") || combined.contains("저렴") || combined.contains("혜자") || combined.contains("저렴한")) {
            tags.add("가성비맛집");
        }
        if (combined.contains("분위기") || combined.contains("데이트") || combined.contains("감성") || combined.contains("예쁜")) {
            tags.add("분위기맛집");
        }
        if (combined.contains("가족") || combined.contains("부모님") || combined.contains("아이와")) {
            tags.add("가족모임추천");
        }
        if (combined.contains("회식") || combined.contains("단체") || combined.contains("모임")) {
            tags.add("단체모임하기좋은");
        }
        if (combined.contains("디저트") || combined.contains("카페") || combined.contains("커피") || combined.contains("빵")) {
            tags.add("디저트천국");
        }
        if (combined.contains("매운") || combined.contains("매워") || combined.contains("맵") || combined.contains("마라")) {
            tags.add("매운맛마니아");
        }
        if (combined.contains("조용") || combined.contains("아늑") || combined.contains("한적")) {
            tags.add("조용하고아늑한");
        }
        if (combined.contains("웨이팅") || combined.contains("줄서") || combined.contains("인기")) {
            tags.add("웨이팅맛집");
        }
        if (combined.contains("야경") || combined.contains("전망") || combined.contains("뷰")) {
            tags.add("뷰맛집");
        }

        // Default tag if no keywords match
        if (tags.isEmpty()) {
            tags.add("숨겨진동네맛집");
        }

        return tags;
    }
}
