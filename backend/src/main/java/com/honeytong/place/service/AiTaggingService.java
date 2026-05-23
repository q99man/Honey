package com.honeytong.place.service;

import java.util.List;

public interface AiTaggingService {
    List<String> generateTags(String placeName, String description, List<String> comments);
}
