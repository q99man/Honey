package com.honeytong.ranking.scheduler;

import com.honeytong.ranking.service.RankingRecalculationResult;
import com.honeytong.ranking.service.RankingRecalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ranking.scheduler", name = "enabled", havingValue = "true")
public class ScheduledRankingRecalculationJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRankingRecalculationJob.class);

    private final RankingRecalculationService rankingRecalculationService;
    private final RankingSchedulerProperties properties;

    public ScheduledRankingRecalculationJob(
            RankingRecalculationService rankingRecalculationService,
            RankingSchedulerProperties properties
    ) {
        this.rankingRecalculationService = rankingRecalculationService;
        this.properties = properties;
    }

    @Scheduled(
            cron = "#{@rankingSchedulerProperties.cron}",
            zone = "#{@rankingSchedulerProperties.zone}"
    )
    public void recalculatePlaceRankings() {
        RankingRecalculationResult result = rankingRecalculationService.recalculate(properties.seasonCode());
        log.info(
                "Scheduled ranking recalculation completed. seasonCode={}, placeCount={}, scoreCount={}",
                result.seasonCode(),
                result.placeCount(),
                result.scoreCount()
        );
    }
}
