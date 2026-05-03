package com.honeytong.ranking.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.ranking.service.RankingRecalculationResult;
import com.honeytong.ranking.service.RankingRecalculationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduledRankingRecalculationJobTest {

    @Mock
    private RankingRecalculationService rankingRecalculationService;

    @Test
    void recalculatePlaceRankingsUsesConfiguredSeasonCode() {
        RankingSchedulerProperties properties = new RankingSchedulerProperties(
                true,
                "0 0 4 * * *",
                "Asia/Seoul",
                "2026-04"
        );
        when(rankingRecalculationService.recalculate("2026-04"))
                .thenReturn(new RankingRecalculationResult(10L, "2026-04", 1, 3));

        new ScheduledRankingRecalculationJob(rankingRecalculationService, properties)
                .recalculatePlaceRankings();

        verify(rankingRecalculationService).recalculate("2026-04");
    }
}
