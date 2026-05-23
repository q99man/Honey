package com.honeytong.place.event;

import static org.mockito.Mockito.verify;

import com.honeytong.place.service.PlaceAudienceStatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceDemographicsEventListenerTest {

    @Mock
    private PlaceAudienceStatsService placeAudienceStatsService;

    @InjectMocks
    private PlaceDemographicsEventListener listener;

    @Test
    void handleRecalculateEvent_callsRecalculateStatsSync() {
        // Arrange
        Long placeId = 123L;
        PlaceDemographicsRecalculateEvent event = new PlaceDemographicsRecalculateEvent(placeId);

        // Act
        listener.handleRecalculateEvent(event);

        // Assert
        verify(placeAudienceStatsService).recalculateStatsSync(placeId);
    }
}
