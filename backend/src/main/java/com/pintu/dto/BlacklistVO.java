package com.pintu.dto;

import lombok.Data;

@Data
public class BlacklistVO {

    private String blockedUserId;
    private String blockedName;
    private String role;
    private String reason;
    private String createdAt;
}
