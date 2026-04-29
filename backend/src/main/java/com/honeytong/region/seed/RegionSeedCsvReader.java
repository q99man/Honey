package com.honeytong.region.seed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class RegionSeedCsvReader {

    private static final int FIELD_COUNT = 6;

    public List<RegionSeedRecord> read(Resource resource) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        )) {
            List<RegionSeedRecord> records = new ArrayList<>();
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (isHeader(trimmed)) {
                    continue;
                }
                List<String> fields = parseLine(line);
                if (fields.size() != FIELD_COUNT) {
                    throw new IllegalArgumentException("Invalid region seed CSV at line " + lineNo + ".");
                }
                records.add(new RegionSeedRecord(
                        fields.get(0),
                        fields.get(1),
                        fields.get(2),
                        fields.get(3),
                        fields.get(4),
                        fields.get(5)
                ));
            }
            return records;
        } catch (IOException exception) {
            throw new IllegalStateException("지역 seed CSV 파일을 읽을 수 없습니다.", exception);
        }
    }

    private boolean isHeader(String line) {
        return line.startsWith("city_code,");
    }

    private List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }
}
