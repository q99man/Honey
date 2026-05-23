package com.honeytong.place.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
public class GeminiAiTaggingService implements AiTaggingService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiTaggingService.class);

    @Value("${app.ai.gemini.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<String> generateTags(String placeName, String description, List<String> comments) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Gemini API key is not configured. Falling back to default tag.");
            return List.of("숨겨진동네맛집");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        String prompt = String.format(
                "You are an expert foodie AI analyzer for Honeytong, a local restaurant discovery platform.\n" +
                "Please analyze the following restaurant details and comments, and extract 2 to 5 relevant Korean tag names (e.g. '혼밥하기좋은', '분위기맛집', '가성비맛집').\n" +
                "Place Name: %s\n" +
                "Description: %s\n" +
                "User Comments: %s\n\n" +
                "Return ONLY a comma-separated list of Korean tag names (do not include hash symbols '#', bullet points, or numbers). Example: 혼밥하기좋은, 분위기맛집, 디저트천국",
                placeName,
                description != null ? description : "",
                String.join(" | ", comments)
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> contentObj = Map.of("parts", List.of(part));
            Map<String, Object> requestBody = Map.of("contents", List.of(contentObj));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseTagsFromGeminiResponse(response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to generate tags using Gemini API: {}", e.getMessage(), e);
        }

        return List.of("숨겨진동네맛집");
    }

    private List<String> parseTagsFromGeminiResponse(Map responseBody) {
        try {
            List candidates = (List) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map candidate = (Map) candidates.getFirst();
                Map content = (Map) candidate.get("content");
                if (content != null) {
                    List parts = (List) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map part = (Map) parts.getFirst();
                        String text = (String) part.get("text");
                        if (text != null && !text.trim().isEmpty()) {
                            return Arrays.stream(text.split(","))
                                    .map(String::trim)
                                    .map(tag -> tag.replaceAll("[#\\s]", "")) // Remove hashes and spaces
                                    .filter(tag -> !tag.isEmpty())
                                    .collect(Collectors.toList());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Gemini API response: {}", e.getMessage(), e);
        }
        return List.of("숨겨진동네맛집");
    }
}
