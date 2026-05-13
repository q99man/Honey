package com.honeytong.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class UserActionLogService {

    public static final String ACTION_PLACE_CREATE = "PLACE_CREATE";
    public static final String ACTION_RECOMMENDATION_CREATE = "RECOMMENDATION_CREATE";
    public static final String ACTION_RECOMMENDATION_CANCEL = "RECOMMENDATION_CANCEL";
    public static final String ACTION_VISIT_VERIFY = "VISIT_VERIFY";
    public static final String ACTION_COMMENT_CREATE = "COMMENT_CREATE";
    public static final String ACTION_COMMENT_UPDATE = "COMMENT_UPDATE";
    public static final String ACTION_COMMENT_DELETE = "COMMENT_DELETE";
    public static final String ACTION_COMMUNITY_POST_CREATE = "COMMUNITY_POST_CREATE";
    public static final String ACTION_COMMUNITY_POST_UPDATE = "COMMUNITY_POST_UPDATE";
    public static final String ACTION_COMMUNITY_POST_DELETE = "COMMUNITY_POST_DELETE";
    public static final String ACTION_REPORT_CREATE = "REPORT_CREATE";

    public static final String TARGET_PLACE = "PLACE";
    public static final String TARGET_COMMENT = "COMMENT";
    public static final String TARGET_COMMUNITY_POST = "COMMUNITY_POST";
    public static final String TARGET_REPORT = "REPORT";

    private final UserActionLogWriter userActionLogWriter;
    private final ObjectMapper objectMapper;

    public UserActionLogService(UserActionLogWriter userActionLogWriter, ObjectMapper objectMapper) {
        this.userActionLogWriter = userActionLogWriter;
        this.objectMapper = objectMapper;
    }

    public void record(
            Long userId,
            String actionType,
            String targetType,
            Long targetId,
            Map<String, Object> metadata
    ) {
        String metadataJson = serializeMetadata(metadata);
        Runnable logWrite = () -> saveQuietly(userId, actionType, targetType, targetId, metadataJson);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    logWrite.run();
                }
            });
            return;
        }
        logWrite.run();
    }

    private void saveQuietly(
            Long userId,
            String actionType,
            String targetType,
            Long targetId,
            String metadataJson
    ) {
        try {
            userActionLogWriter.save(userId, actionType, targetType, targetId, metadataJson);
        } catch (RuntimeException ignored) {
            // User action logging is operational telemetry and must not break a completed domain action.
        }
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            return "{\"metadataSerializationFailed\":true}";
        }
    }
}
