package com.honeytong.policy.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

class PolicySeedCsvReaderTest {

    private final PolicySeedCsvReader reader = new PolicySeedCsvReader();

    @Test
    void read_parsesUtf8CsvAndSkipsHeader() {
        String csv = """
                policy_group,policy_key,policy_value,value_type,description
                region,change_cooldown_day,7,INTEGER,지역 변경 쿨다운 일수
                """;

        var records = reader.read(new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(records).hasSize(1);
        assertThat(records.getFirst().policyGroup()).isEqualTo("region");
        assertThat(records.getFirst().policyKey()).isEqualTo("change_cooldown_day");
        assertThat(records.getFirst().description()).isEqualTo("지역 변경 쿨다운 일수");
    }

    @Test
    void read_parsesDefaultPolicySeedResource() {
        var records = reader.read(new ClassPathResource("policy/policy-defaults.csv"));

        assertThat(records)
                .anySatisfy(record -> {
                    assertThat(record.policyGroup()).isEqualTo("region");
                    assertThat(record.policyKey()).isEqualTo("change_cooldown_day");
                    assertThat(record.policyValue()).isEqualTo("7");
                })
                .anySatisfy(record -> {
                    assertThat(record.policyGroup()).isEqualTo("visit");
                    assertThat(record.policyKey()).isEqualTo("radius_meter");
                    assertThat(record.policyValue()).isEqualTo("70");
                })
                .anySatisfy(record -> {
                    assertThat(record.policyGroup()).isEqualTo("comment");
                    assertThat(record.policyKey()).isEqualTo("max_length");
                    assertThat(record.policyValue()).isEqualTo("300");
                })
                .anySatisfy(record -> {
                    assertThat(record.policyGroup()).isEqualTo("community");
                    assertThat(record.policyKey()).isEqualTo("post_title_max_length");
                    assertThat(record.policyValue()).isEqualTo("120");
                })
                .anySatisfy(record -> {
                    assertThat(record.policyGroup()).isEqualTo("community");
                    assertThat(record.policyKey()).isEqualTo("post_content_max_length");
                    assertThat(record.policyValue()).isEqualTo("2000");
                })
                .anySatisfy(record -> {
                    assertThat(record.policyGroup()).isEqualTo("place");
                    assertThat(record.policyKey()).isEqualTo("recommended_menu_max_length");
                    assertThat(record.policyValue()).isEqualTo("255");
                })
                .anySatisfy(record -> {
                    assertThat(record.policyGroup()).isEqualTo("place");
                    assertThat(record.policyKey()).isEqualTo("short_recommendation_max_length");
                    assertThat(record.policyValue()).isEqualTo("255");
                })
                .anySatisfy(record -> {
                    assertThat(record.policyGroup()).isEqualTo("place");
                    assertThat(record.policyKey()).isEqualTo("feature_text_max_length");
                    assertThat(record.policyValue()).isEqualTo("500");
                })
                .anySatisfy(record -> {
                    assertThat(record.policyGroup()).isEqualTo("report");
                    assertThat(record.policyKey()).isEqualTo("reason_text_max_length");
                    assertThat(record.policyValue()).isEqualTo("255");
                })
                .anySatisfy(record -> {
                    assertThat(record.policyGroup()).isEqualTo("report");
                    assertThat(record.policyKey()).isEqualTo("review_note_max_length");
                    assertThat(record.policyValue()).isEqualTo("255");
                })
                .anySatisfy(record -> {
                    assertThat(record.policyGroup()).isEqualTo("report");
                    assertThat(record.policyKey()).isEqualTo("follow_up_reason_max_length");
                    assertThat(record.policyValue()).isEqualTo("255");
                })
                .anySatisfy(record -> {
                    assertThat(record.policyGroup()).isEqualTo("report");
                    assertThat(record.policyKey()).isEqualTo("follow_up_memo_max_length");
                    assertThat(record.policyValue()).isEqualTo("255");
                });
    }
}
