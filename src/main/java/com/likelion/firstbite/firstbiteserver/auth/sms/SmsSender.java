package com.likelion.firstbite.firstbiteserver.auth.sms;

public interface SmsSender {
    void sendVerificationCode(String phoneNumber, String code);
}
