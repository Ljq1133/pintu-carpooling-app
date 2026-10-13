package com.pintu.dto;

import lombok.Data;

import java.util.List;

@Data
public class SupportSessionVO {
    private String id;
    private String userId;
    private String userName;
    private String assigneeId;
    private String subject;
    private String status;
    private Long messageCount;
    private String latestMessage;
    private String createdAt;
    private String updatedAt;
    private String closedAt;
    private List<SupportMessageVO> messages;
}
