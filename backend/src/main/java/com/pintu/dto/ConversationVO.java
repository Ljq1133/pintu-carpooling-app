package com.pintu.dto;

import lombok.Data;

@Data
public class ConversationVO {
    private String id;
    private String orderId;
    private String matchRequestId;
    private String passengerId;
    private String driverId;
    private String status;
    private long unreadCount;
    private String createdAt;
    private String closedAt;
}
