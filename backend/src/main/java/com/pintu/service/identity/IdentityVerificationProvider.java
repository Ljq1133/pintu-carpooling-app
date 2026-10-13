package com.pintu.service.identity;

public interface IdentityVerificationProvider {

    boolean verify(String realName, String idCard, String livenessResult);
}

