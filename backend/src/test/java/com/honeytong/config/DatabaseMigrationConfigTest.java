package com.honeytong.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class DatabaseMigrationConfigTest {

    @Test
    void localProfile_keepsFlywayDisabledByDefaultForConvenientDevelopment() {
        Map<String, String> properties = loadYaml("application.yml");

        assertThat(properties.get("spring.flyway.enabled")).isEqualTo("${FLYWAY_ENABLED:false}");
        assertThat(properties.get("spring.flyway.locations")).isEqualTo("${FLYWAY_LOCATIONS:classpath:db/migration}");
        assertThat(properties.get("spring.flyway.baseline-on-migrate"))
                .isEqualTo("${FLYWAY_BASELINE_ON_MIGRATE:false}");
        assertThat(properties.get("spring.flyway.validate-on-migrate"))
                .isEqualTo("${FLYWAY_VALIDATE_ON_MIGRATE:true}");
    }

    @Test
    void productionProfile_enablesFlywayBeforeHibernateSchemaValidation() {
        Map<String, String> properties = loadYaml("application-prod.yml");

        assertThat(properties.get("spring.flyway.enabled")).isEqualTo("${FLYWAY_ENABLED:true}");
        assertThat(properties.get("spring.flyway.locations")).isEqualTo("${FLYWAY_LOCATIONS:classpath:db/migration}");
        assertThat(properties.get("spring.flyway.validate-on-migrate"))
                .isEqualTo("${FLYWAY_VALIDATE_ON_MIGRATE:true}");
        assertThat(properties.get("spring.jpa.hibernate.ddl-auto")).isEqualTo("${JPA_DDL_AUTO:validate}");
    }

    @Test
    void baselineMigration_containsCurrentCoreSchemaTables() throws IOException {
        ClassPathResource migration = new ClassPathResource("db/migration/V1__baseline_schema.sql");

        assertThat(migration.exists()).isTrue();

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE users");
        assertThat(sql).contains("CREATE TABLE places");
        assertThat(sql).contains("CREATE TABLE system_policies");
        assertThat(sql).contains("CREATE TABLE admin_action_logs");
        assertThat(sql).contains("CREATE TABLE user_action_logs");
        assertThat(sql).contains("ranking_excluded BOOLEAN NOT NULL DEFAULT FALSE");
    }

    @Test
    void rankingIndexMigration_addsReadPathIndexes() throws IOException {
        ClassPathResource migration = new ClassPathResource(
                "db/migration/V2__add_ranking_query_indexes.sql"
        );

        assertThat(migration.exists()).isTrue();

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("idx_place_season_scores_region_rank");
        assertThat(sql).contains("season_id");
        assertThat(sql).contains("region_type");
        assertThat(sql).contains("region_ref_id");
        assertThat(sql).contains("rank_no");
        assertThat(sql).contains("idx_place_ranking_history_place_season_region_rank");
        assertThat(sql).contains("idx_place_ranking_history_season_region_rank");
    }

    @Test
    void spatialIndexMigration_usesLongitudeLatitudeAxisOrder() throws IOException {
        ClassPathResource migration = new ClassPathResource(
                "db/migration/V10__add_place_location_spatial_index.sql"
        );

        assertThat(migration.exists()).isTrue();

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("axis-order=long-lat");
        assertThat(sql).contains("STORED NOT NULL");
        assertThat(sql).contains("idx_places_location");
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
