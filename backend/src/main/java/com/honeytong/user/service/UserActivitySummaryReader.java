package com.honeytong.user.service;

import com.honeytong.user.dto.UserActivitySummaryResponse;

public interface UserActivitySummaryReader {

    UserActivitySummaryResponse read(Long userId);
}
