package com.honeytong.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class ManagementHealthConfigTest {

    @Test
    void applicationYaml_exposesOnlyHealthByDefault() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));

        Map<String, String> properties = factory.getObject().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue().toString()
                ));

        assertThat(properties.get("management.endpoints.web.exposure.include"))
                .isEqualTo("${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE:health}");
        assertThat(properties.get("management.endpoint.health.probes.enabled"))
                .isEqualTo("${MANAGEMENT_HEALTH_PROBES_ENABLED:true}");
        assertThat(properties.get("management.endpoint.health.show-details"))
                .isEqualTo("${MANAGEMENT_HEALTH_SHOW_DETAILS:never}");
    }
}
