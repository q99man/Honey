package com.honeytong;

import com.honeytong.auth.config.SecurityProperties;
import com.honeytong.auth.config.PhoneVerificationProperties;
import com.honeytong.map.config.MapProperties;
import com.honeytong.region.seed.RegionSeedProperties;
import com.honeytong.user.config.GrowthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        SecurityProperties.class,
        PhoneVerificationProperties.class,
        GrowthProperties.class,
        MapProperties.class,
        RegionSeedProperties.class
})
public class HoneytongApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoneytongApplication.class, args);
    }
}
