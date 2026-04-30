package com.honeytong.admin.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private final AdminBootstrapProperties properties;
    private final AdminBootstrapService adminBootstrapService;

    public AdminBootstrapRunner(
            AdminBootstrapProperties properties,
            AdminBootstrapService adminBootstrapService
    ) {
        this.properties = properties;
        this.adminBootstrapService = adminBootstrapService;
    }

    @Override
    public void run(String... args) {
        if (properties.enabled()) {
            adminBootstrapService.bootstrap();
        }
    }
}
