package com.honeytong.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class RedisConfigTest {

    @Test
    void localProfile_definesRedisConnectionButKeepsUsageDisabledByDefault() {
        Map<String, String> properties = loadYaml("application.yml");

        assertThat(properties.get("spring.cache.type")).isEqualTo("${CACHE_TYPE:none}");
        assertThat(properties.get("spring.data.redis.host")).isEqualTo("${REDIS_HOST:localhost}");
        assertThat(properties.get("spring.data.redis.port")).isEqualTo("${REDIS_PORT:6379}");
        assertThat(properties.get("spring.data.redis.password")).isEqualTo("${REDIS_PASSWORD:}");
        assertThat(properties.get("spring.data.redis.database")).isEqualTo("${REDIS_DATABASE:0}");
        assertThat(properties.get("spring.data.redis.timeout")).isEqualTo("${REDIS_TIMEOUT:2s}");
        assertThat(properties.get("spring.data.redis.repositories.enabled")).isEqualTo("false");
        assertThat(properties.get("management.health.redis.enabled")).isEqualTo("${REDIS_HEALTH_ENABLED:false}");
        assertThat(properties.get("app.redis.enabled")).isEqualTo("${APP_REDIS_ENABLED:false}");
        assertThat(properties.get("app.redis.policy-cache-ttl")).isEqualTo("${POLICY_CACHE_TTL:10m}");
    }

    @Test
    void productionProfile_requiresRedisHostButKeepsRedisBackedFeaturesDisabledByDefault() {
        Map<String, String> properties = loadYaml("application-prod.yml");

        assertThat(properties.get("spring.cache.type")).isEqualTo("${CACHE_TYPE:none}");
        assertThat(properties.get("spring.data.redis.host")).isEqualTo("${REDIS_HOST}");
        assertThat(properties.get("spring.data.redis.port")).isEqualTo("${REDIS_PORT:6379}");
        assertThat(properties.get("spring.data.redis.password")).isEqualTo("${REDIS_PASSWORD:}");
        assertThat(properties.get("spring.data.redis.database")).isEqualTo("${REDIS_DATABASE:0}");
        assertThat(properties.get("spring.data.redis.timeout")).isEqualTo("${REDIS_TIMEOUT:2s}");
        assertThat(properties.get("spring.data.redis.repositories.enabled")).isEqualTo("false");
        assertThat(properties.get("management.health.redis.enabled")).isEqualTo("${REDIS_HEALTH_ENABLED:false}");
        assertThat(properties.get("app.redis.enabled")).isEqualTo("${APP_REDIS_ENABLED:false}");
        assertThat(properties.get("app.redis.policy-cache-ttl")).isEqualTo("${POLICY_CACHE_TTL:10m}");
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
