package com.honeytong.auth.service;

public interface PhoneVerificationSender {

    void send(String phone, String code);
}
