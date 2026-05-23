package com.honeytong.notification.event;

import com.honeytong.comment.event.CommentCreatedEvent;
import com.honeytong.mission.event.MissionCompletedEvent;
import com.honeytong.notification.entity.NotificationType;
import com.honeytong.notification.service.NotificationService;
import com.honeytong.report.event.ReportProcessedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        // 맛집 등록자가 자신의 맛집에 댓글을 남긴 경우는 알림을 보내지 않음
        if (event.commenterId().equals(event.placeCreatorId())) {
            return;
        }

        String title = "새로운 댓글이 등록되었습니다.";
        String content = String.format("[%s] 맛집에 새로운 댓글이 작성되었습니다: \"%s\"", 
                event.placeName(), truncateContent(event.commentContent(), 30));

        notificationService.createNotification(
                event.placeCreatorId(),
                NotificationType.NEW_COMMENT,
                title,
                content,
                event.placeId()
        );
    }

    @EventListener
    public void handleMissionCompletedEvent(MissionCompletedEvent event) {
        String title = "미션을 완료했습니다!";
        String content = String.format("\"%s\" 미션을 완료하여 %d EXP 보상 획득이 가능합니다.", 
                event.missionTitle(), event.rewardExp());

        notificationService.createNotification(
                event.userId(),
                NotificationType.MISSION_COMPLETE,
                title,
                content,
                event.missionId()
        );
    }

    @EventListener
    public void handleReportProcessedEvent(ReportProcessedEvent event) {
        String statusKo = "APPROVED".equalsIgnoreCase(event.reportStatus()) ? "승인" : "반려";
        String title = "신고 처리 결과 안내";
        
        String memo = event.reviewNote() != null ? event.reviewNote() : "규정에 따라 검토 및 처리가 완료되었습니다.";
        String content = String.format("제출하신 신고(유형: %s)가 %s 처리되었습니다. 처리 의견: %s", 
                event.targetType(), statusKo, memo);

        notificationService.createNotification(
                event.reporterId(),
                NotificationType.REPORT_PROCESSED,
                title,
                content,
                event.reportId()
        );
    }

    private String truncateContent(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
