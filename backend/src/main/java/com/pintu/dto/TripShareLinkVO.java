package com.pintu.dto;

import lombok.Data;

@Data
public class TripShareLinkVO {
    private String token;
    private String publicPath;
    private String orderId;
    private String expiresAt;
}
