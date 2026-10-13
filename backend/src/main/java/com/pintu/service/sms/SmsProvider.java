package com.pintu.service.sms;

public interface SmsProvider {

    void send(String phone, String code, String purpose);
}

