package com.honeytong.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActionLogServiceTest {

    private static final long USER_ID = 1L;
    private static final long PLACE_ID = 100L;

    @Mock
    private UserActionLogWriter userActionLogWriter;

    private UserActionLogService userActionLogService;

    @BeforeEach
    void setUp() {
        userActionLogService = new UserActionLogService(userActionLogWriter, new ObjectMapper());
    }

    @Test
    void record_writesSerializedMetadataImmediatelyWhenNoTransactionSynchronizationIsActive() {
        userActionLogService.record(
                USER_ID,
                UserActionLogService.ACTION_PLACE_CREATE,
                UserActionLogService.TARGET_PLACE,
                PLACE_ID,
                Map.of("categoryCode", "KOREAN")
        );

        verify(userActionLogWriter).save(
                eq(USER_ID),
                eq(UserActionLogService.ACTION_PLACE_CREATE),
                eq(UserActionLogService.TARGET_PLACE),
                eq(PLACE_ID),
                contains("\"categoryCode\":\"KOREAN\"")
        );
    }

    @Test
    void record_doesNotThrowWhenLogWriterFails() {
        doThrow(new RuntimeException("log write failed"))
                .when(userActionLogWriter)
                .save(
                        eq(USER_ID),
                        eq(UserActionLogService.ACTION_VISIT_VERIFY),
                        eq(UserActionLogService.TARGET_PLACE),
                        eq(PLACE_ID),
                        org.mockito.Mockito.any()
                );

        assertThatCode(() -> userActionLogService.record(
                USER_ID,
                UserActionLogService.ACTION_VISIT_VERIFY,
                UserActionLogService.TARGET_PLACE,
                PLACE_ID,
                Map.of("distanceMeter", 10)
        )).doesNotThrowAnyException();
    }
}
