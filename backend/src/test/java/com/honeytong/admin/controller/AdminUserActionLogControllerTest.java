package com.honeytong.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.admin.dto.AdminUserActionLogResponse;
import com.honeytong.admin.service.AdminUserActionLogService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminUserActionLogControllerTest {

    @Test
    void getUserActionLogs_returnsServiceResponse() {
        AdminUserActionLogService service = org.mockito.Mockito.mock(AdminUserActionLogService.class);
        AdminUserActionLogController controller = new AdminUserActionLogController(service);
        AdminUserActionLogResponse log = new AdminUserActionLogResponse(
                1L,
                2L,
                "bee",
                "PLACE_CREATE",
                "PLACE",
                100L,
                null,
                null,
                "{\"dongId\":123}",
                LocalDateTime.of(2026, 5, 2, 12, 0)
        );
        when(service.getUserActionLogs(10L)).thenReturn(List.of(log));

        var response = controller.getUserActionLogs(10L);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).containsExactly(log);
        assertThat(response.message()).isEqualTo("OK");
        verify(service).getUserActionLogs(10L);
    }
}
