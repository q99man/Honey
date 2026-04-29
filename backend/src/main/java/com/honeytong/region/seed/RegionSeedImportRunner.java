package com.honeytong.region.seed;

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
@ConditionalOnProperty(prefix = "app.regions.seed", name = "enabled", havingValue = "true")
public class RegionSeedImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RegionSeedImportRunner.class);

    private final RegionSeedProperties properties;
    private final ResourceLoader resourceLoader;
    private final RegionSeedCsvReader csvReader;
    private final RegionSeedImportService importService;

    public RegionSeedImportRunner(
            RegionSeedProperties properties,
            ResourceLoader resourceLoader,
            RegionSeedCsvReader csvReader,
            RegionSeedImportService importService
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
            log.warn("Region seed import skipped. Resource not found: {}", properties.location());
            return;
        }

        List<RegionSeedRecord> records = csvReader.read(resource);
        RegionSeedImportResult result = importService.importRecords(records);
        log.info(
                "Region seed import completed. rows={}, cities={}, districts={}, dongs={}",
                result.rowCount(),
                result.cityCount(),
                result.districtCount(),
                result.dongCount()
        );
    }
}
