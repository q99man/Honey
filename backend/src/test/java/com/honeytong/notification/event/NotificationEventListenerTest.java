package com.honeytong.notification.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.honeytong.comment.event.CommentCreatedEvent;
import com.honeytong.mission.event.MissionCompletedEvent;
import com.honeytong.notification.entity.NotificationType;
import com.honeytong.notification.service.NotificationService;
import com.honeytong.report.event.ReportProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new NotificationEventListener(notificationService);
    }

    @Test
    void handleCommentCreatedEvent_createsNotificationWhenCommenterIsDifferentFromPlaceCreator() {
        CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 10L, 2L, 3L, "맛집 이름", "정말 맛있는 맛집이네요!"
        );

        eventListener.handleCommentCreatedEvent(event);

        verify(notificationService).createNotification(
                eq(3L),
                eq(NotificationType.NEW_COMMENT),
                contains("새로운 댓글"),
                contains("[맛집 이름] 맛집에 새로운 댓글이 작성되었습니다"),
                eq(10L)
        );
    }

    @Test
    void handleCommentCreatedEvent_doesNotCreateNotificationWhenCommenterIsPlaceCreator() {
        CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 10L, 3L, 3L, "맛집 이름", "스스로 댓글 달기"
        );

        eventListener.handleCommentCreatedEvent(event);

        verify(notificationService, never()).createNotification(
                any(), any(), any(), any(), any()
        );
    }

    @Test
    void handleMissionCompletedEvent_createsKoreanNotification() {
        MissionCompletedEvent event = new MissionCompletedEvent(
                5L, 20L, "주간 리뷰 작성", 50
        );

        eventListener.handleMissionCompletedEvent(event);

        verify(notificationService).createNotification(
                eq(5L),
                eq(NotificationType.MISSION_COMPLETE),
                contains("미션을 완료했습니다"),
                contains("\"주간 리뷰 작성\" 미션을 완료하여 50 EXP 보상"),
                eq(20L)
        );
    }

    @Test
    void handleReportProcessedEvent_createsKoreanNotificationForApproval() {
        ReportProcessedEvent event = new ReportProcessedEvent(
                8L, 30L, "COMMENT", "APPROVED", "욕설 사유로 숨김 처리 완료"
        );

        eventListener.handleReportProcessedEvent(event);

        verify(notificationService).createNotification(
                eq(8L),
                eq(NotificationType.REPORT_PROCESSED),
                contains("신고 처리 결과"),
                contains("제출하신 신고(유형: COMMENT)가 승인 처리되었습니다. 처리 의견: 욕설 사유로 숨김 처리 완료"),
                eq(30L)
        );
    }

    @Test
    void handleReportProcessedEvent_createsKoreanNotificationForRejection() {
        ReportProcessedEvent event = new ReportProcessedEvent(
                8L, 30L, "PLACE", "REJECTED", null
        );

        eventListener.handleReportProcessedEvent(event);

        verify(notificationService).createNotification(
                eq(8L),
                eq(NotificationType.REPORT_PROCESSED),
                contains("신고 처리 결과"),
                contains("제출하신 신고(유형: PLACE)가 반려 처리되었습니다. 처리 의견: 규정에 따라 검토 및 처리가 완료되었습니다."),
                eq(30L)
        );
    }
}
