package com.pintu.service.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "security.verification.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsProvider implements SmsProvider {

    @Override
    public void send(String phone, String code, String purpose) {
        // The course environment intentionally performs no external network call.
    }
}

