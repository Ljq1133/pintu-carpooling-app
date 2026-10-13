package com.pintu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerificationCodeResponse {

    private long expiresIn;
    private long retryAfter;
    private String debugCode;
}

