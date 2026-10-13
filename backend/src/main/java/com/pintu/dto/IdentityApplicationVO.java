package com.pintu.dto;

import lombok.Data;

@Data
public class IdentityApplicationVO {

    private String id;
    private String idCardMasked;
    private String livenessResult;
    private String status;
    private String rejectReason;
    private String submittedAt;
    private String handledAt;
}

