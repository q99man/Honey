package com.honeytong;

import com.honeytong.auth.config.SecurityProperties;
import com.honeytong.auth.config.PhoneVerificationProperties;
import com.honeytong.auth.config.OAuthProperties;
import com.honeytong.admin.bootstrap.AdminBootstrapProperties;
import com.honeytong.map.config.MapProperties;
import com.honeytong.policy.cache.PolicyRedisProperties;
import com.honeytong.policy.seed.PolicySeedProperties;
import com.honeytong.ranking.cache.RankingRedisProperties;
import com.honeytong.ranking.scheduler.RankingSchedulerProperties;
import com.honeytong.region.seed.RegionSeedProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        SecurityProperties.class,
        PhoneVerificationProperties.class,
        OAuthProperties.class,
        MapProperties.class,
        PolicyRedisProperties.class,
        RankingRedisProperties.class,
        RankingSchedulerProperties.class,
        RegionSeedProperties.class,
        PolicySeedProperties.class,
        AdminBootstrapProperties.class
})
public class HoneytongApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoneytongApplication.class, args);
    }
}
