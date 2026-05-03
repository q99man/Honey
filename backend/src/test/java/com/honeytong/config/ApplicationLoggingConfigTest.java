package com.honeytong.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class ApplicationLoggingConfigTest {

    @Test
    void applicationYaml_definesSafeLoggingBaseline() throws IOException {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));

        Map<String, String> properties = factory.getObject().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue().toString()
                ));

        assertThat(properties.get("logging.level.root")).isEqualTo("${LOG_LEVEL_ROOT:INFO}");
        assertThat(properties.get("logging.level.com.honeytong")).isEqualTo("${LOG_LEVEL_APP:INFO}");
        assertThat(properties.get("logging.level.org.springframework.security")).isEqualTo("${LOG_LEVEL_SECURITY:WARN}");
        assertThat(properties.get("logging.level.org.hibernate.SQL")).isEqualTo("${LOG_LEVEL_SQL:WARN}");
        assertThat(properties.get("logging.level.org.hibernate.orm.jdbc.bind")).isEqualTo("${LOG_LEVEL_SQL_BIND:OFF}");
        assertThat(properties.get("logging.file.name")).isEqualTo("${LOG_FILE:logs/honeytong-backend.log}");
        assertThat(properties.get("logging.logback.rollingpolicy.max-file-size")).isEqualTo("${LOG_FILE_MAX_SIZE:10MB}");
        assertThat(properties.get("logging.logback.rollingpolicy.max-history")).isEqualTo("${LOG_FILE_MAX_HISTORY:14}");
    }
}
