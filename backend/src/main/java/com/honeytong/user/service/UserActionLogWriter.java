package com.honeytong.user.service;

import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserActionLog;
import com.honeytong.user.repository.UserActionLogRepository;
import com.honeytong.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserActionLogWriter {

    private final UserActionLogRepository userActionLogRepository;
    private final UserRepository userRepository;

    public UserActionLogWriter(
            UserActionLogRepository userActionLogRepository,
            UserRepository userRepository
    ) {
        this.userActionLogRepository = userActionLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(
            Long userId,
            String actionType,
            String targetType,
            Long targetId,
            String metadataJson
    ) {
        User user = userRepository.getReferenceById(userId);
        userActionLogRepository.saveAndFlush(new UserActionLog(
                user,
                actionType,
                targetType,
                targetId,
                null,
                null,
                metadataJson
        ));
    }
}
