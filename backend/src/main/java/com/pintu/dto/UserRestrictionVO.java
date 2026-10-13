package com.pintu.dto;

import lombok.Data;

@Data
public class UserRestrictionVO {
    private String id;
    private String userId;
    private String type;
    private String reason;
    private String operatorId;
    private String status;
    private String startsAt;
    private String releasedAt;
}
