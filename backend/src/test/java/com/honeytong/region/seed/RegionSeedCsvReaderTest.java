package com.honeytong.region.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

class RegionSeedCsvReaderTest {

    private final RegionSeedCsvReader reader = new RegionSeedCsvReader();

    @Test
    void read_parsesUtf8CsvAndSkipsHeader() {
        String csv = """
                city_code,city_name_ko,district_code,district_name_ko,dong_code,dong_name_ko
                11,서울특별시,11440,마포구,1144066000,서교동
                """;

        var records = reader.read(new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(records).hasSize(1);
        assertThat(records.getFirst().cityNameKo()).isEqualTo("서울특별시");
        assertThat(records.getFirst().dongCode()).isEqualTo("1144066000");
    }

    @Test
    void read_parsesGeneratedRegionSeedResource() {
        var records = reader.read(new ClassPathResource("region/region-seed.csv"));

        assertThat(records).hasSizeGreaterThan(3_000);
        assertThat(records)
                .anySatisfy(record -> {
                    assertThat(record.dongCode()).isEqualTo("1144066000");
                    assertThat(record.dongNameKo()).isEqualTo("서교동");
                })
                .anySatisfy(record -> {
                    assertThat(record.dongCode()).isEqualTo("4113565500");
                    assertThat(record.dongNameKo()).isEqualTo("삼평동");
                });
    }
}
