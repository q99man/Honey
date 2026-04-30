package com.honeytong.policy.seed;

import com.honeytong.policy.entity.PolicyValueType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class PolicySeedCsvReader {

    private static final int FIELD_COUNT = 5;

    public List<PolicySeedRecord> read(Resource resource) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        )) {
            List<PolicySeedRecord> records = new ArrayList<>();
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
                    throw new IllegalArgumentException("Invalid policy seed CSV at line " + lineNo + ".");
                }
                records.add(new PolicySeedRecord(
                        fields.get(0),
                        fields.get(1),
                        fields.get(2),
                        PolicyValueType.valueOf(fields.get(3)),
                        fields.get(4)
                ));
            }
            return records;
        } catch (IOException exception) {
            throw new IllegalStateException("Policy seed CSV file cannot be read.", exception);
        }
    }

    private boolean isHeader(String line) {
        return line.startsWith("policy_group,");
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
