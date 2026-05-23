package com.honeytong.place.event;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.comment.event.CommentCreatedEvent;
import com.honeytong.place.entity.Place;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.place.service.PlaceAiTagService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceAiTagEventListenerTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceAiTagService placeAiTagService;

    @Mock
    private Place place;

    @InjectMocks
    private PlaceAiTagEventListener eventListener;

    @Test
    @DisplayName("PlaceCreatedEvent가 발생하면 AI 태그 생성이 트리거되어야 한다")
    void handlePlaceCreatedTest() {
        // given
        Long placeId = 1L;
        PlaceCreatedEvent event = new PlaceCreatedEvent(placeId);
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));

        // when
        eventListener.handlePlaceCreated(event);

        // then
        verify(placeRepository).findById(placeId);
        verify(placeAiTagService).generateAndSaveTags(place);
    }

    @Test
    @DisplayName("CommentCreatedEvent가 발생하면 AI 태그 재생성이 트리거되어야 한다")
    void handleCommentCreatedTest() {
        // given
        Long placeId = 1L;
        CommentCreatedEvent event = new CommentCreatedEvent(
                10L, placeId, 2L, 3L, "식당", "댓글 내용"
        );
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));

        // when
        eventListener.handleCommentCreated(event);

        // then
        verify(placeRepository).findById(placeId);
        verify(placeAiTagService).generateAndSaveTags(place);
    }
}
