package com.honeytong.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.security.phone-verification.sender",
        name = "provider",
        havingValue = "dev",
        matchIfMissing = true
)
public class DevPhoneVerificationSender implements PhoneVerificationSender {

    private static final Logger log = LoggerFactory.getLogger(DevPhoneVerificationSender.class);

    @Override
    public void send(String phone, String code) {
        String maskedPhone = maskPhone(phone);
        log.info("Phone verification code issued. phone={}", maskedPhone);
        log.debug("Development phone verification code issued. phone={}, code={}", maskedPhone, code);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }
}
