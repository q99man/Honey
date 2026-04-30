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
                });
    }
}
