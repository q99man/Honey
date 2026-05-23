package com.honeytong.place.event;

import com.honeytong.place.service.PlaceAudienceStatsService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PlaceDemographicsEventListener {

    private final PlaceAudienceStatsService placeAudienceStatsService;

    public PlaceDemographicsEventListener(PlaceAudienceStatsService placeAudienceStatsService) {
        this.placeAudienceStatsService = placeAudienceStatsService;
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRecalculateEvent(PlaceDemographicsRecalculateEvent event) {
        placeAudienceStatsService.recalculateStatsSync(event.getPlaceId());
    }
}
