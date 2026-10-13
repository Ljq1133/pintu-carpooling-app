package com.pintu.dto;

import lombok.Data;

@Data
public class VirtualPhoneBindingVO {
    private String id;
    private String orderId;
    private String virtualNumber;
    private String status;
    private String boundAt;
    private String unboundAt;
}
