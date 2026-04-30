package com.honeytong.policy.seed;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.policies.seed", name = "enabled", havingValue = "true")
public class PolicySeedImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicySeedImportRunner.class);

    private final PolicySeedProperties properties;
    private final ResourceLoader resourceLoader;
    private final PolicySeedCsvReader csvReader;
    private final PolicySeedImportService importService;

    public PolicySeedImportRunner(
            PolicySeedProperties properties,
            ResourceLoader resourceLoader,
            PolicySeedCsvReader csvReader,
            PolicySeedImportService importService
    ) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.csvReader = csvReader;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Resource resource = resourceLoader.getResource(properties.location());
        if (!resource.exists()) {
            log.warn("Policy seed import skipped. Resource not found: {}", properties.location());
            return;
        }

        List<PolicySeedRecord> records = csvReader.read(resource);
        PolicySeedImportResult result = importService.importRecords(records);
        log.info(
                "Policy seed import completed. rows={}, inserted={}, skipped={}",
                result.rowCount(),
                result.insertedCount(),
                result.skippedCount()
        );
    }
}
