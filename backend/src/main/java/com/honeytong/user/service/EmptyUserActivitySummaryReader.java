package com.honeytong.user.service;

import com.honeytong.user.dto.UserActivitySummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class EmptyUserActivitySummaryReader implements UserActivitySummaryReader {

    @Override
    public UserActivitySummaryResponse read(Long userId) {
        return new UserActivitySummaryResponse(0, 0, 0, 0);
    }
}
