package com.pintu.service.identity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "identity.verification.provider",
        havingValue = "mock",
        matchIfMissing = true
)
public class MockIdentityVerificationProvider implements IdentityVerificationProvider {

    @Override
    public boolean verify(String realName, String idCard, String livenessResult) {
        return realName != null && !realName.isBlank()
                && idCard != null && (idCard.length() == 15 || idCard.length() == 18)
                && "PASS".equalsIgnoreCase(livenessResult);
    }
}
