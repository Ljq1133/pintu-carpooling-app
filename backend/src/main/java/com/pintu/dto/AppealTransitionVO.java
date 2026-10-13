package com.pintu.dto;

import lombok.Data;

@Data
public class AppealTransitionVO {
    private String id;
    private String operatorId;
    private String fromStatus;
    private String toStatus;
    private String comment;
    private String createdAt;
}
