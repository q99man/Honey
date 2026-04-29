package com.honeytong.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DevPhoneVerificationSender implements PhoneVerificationSender {

    private static final Logger log = LoggerFactory.getLogger(DevPhoneVerificationSender.class);

    @Override
    public void send(String phone, String code) {
        log.info("Phone verification code issued. phone={}, code={}", phone, code);
    }
}
