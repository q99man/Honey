package com.honeytong.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class ProductionProfileConfigTest {

    @Test
    void productionProfile_requiresExternalSecretsAndDisablesSchemaAutoUpdate() {
        Map<String, String> properties = loadYaml("application-prod.yml");

        assertThat(properties.get("spring.datasource.url")).isEqualTo("${DB_URL}");
        assertThat(properties.get("spring.datasource.username")).isEqualTo("${DB_USERNAME}");
        assertThat(properties.get("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
        assertThat(properties.get("app.security.jwt.secret")).isEqualTo("${JWT_SECRET}");
        assertThat(properties.get("spring.jpa.hibernate.ddl-auto")).isEqualTo("${JPA_DDL_AUTO:validate}");
    }

    @Test
    void localDevelopmentProfile_keepsExistingSchemaAutoUpdateDefault() {
        Map<String, String> properties = loadYaml("application.yml");

        assertThat(properties.get("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
    }

    @Test
    void localDevelopmentProfileSeedsMissingPoliciesByDefault() {
        Map<String, String> properties = loadYaml("application.yml");

        assertThat(properties.get("app.policies.seed.enabled")).isEqualTo("${POLICY_SEED_ENABLED:true}");
    }

    @Test
    void productionProfile_keepsOperationalBootstrapAndSeedImportsDisabledByDefault() {
        Map<String, String> properties = loadYaml("application-prod.yml");

        assertThat(properties.get("app.regions.seed.enabled")).isEqualTo("${REGION_SEED_ENABLED:false}");
        assertThat(properties.get("app.policies.seed.enabled")).isEqualTo("${POLICY_SEED_ENABLED:false}");
        assertThat(properties.get("app.admin.bootstrap.enabled")).isEqualTo("${ADMIN_BOOTSTRAP_ENABLED:false}");
    }

    @Test
    void productionProfile_defaultsPhoneVerificationSenderToSolapi() {
        Map<String, String> properties = loadYaml("application-prod.yml");

        assertThat(properties.get("app.security.phone-verification.sender.provider"))
                .isEqualTo("${PHONE_VERIFICATION_SENDER_PROVIDER:solapi}");
    }

    @Test
    void productionProfile_exposesOnlyHealthAndHidesDetailsByDefault() {
        Map<String, String> properties = loadYaml("application-prod.yml");

        assertThat(properties.get("management.endpoints.web.exposure.include"))
                .isEqualTo("${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE:health}");
        assertThat(properties.get("management.endpoint.health.probes.enabled"))
                .isEqualTo("${MANAGEMENT_HEALTH_PROBES_ENABLED:true}");
        assertThat(properties.get("management.endpoint.health.show-details"))
                .isEqualTo("${MANAGEMENT_HEALTH_SHOW_DETAILS:never}");
    }

    private Map<String, String> loadYaml(String resourceName) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(resourceName));

        return factory.getObject().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue().toString()
                ));
    }
}
