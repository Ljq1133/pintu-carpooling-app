package com.pintu.dto;

import lombok.Data;

@Data
public class CreditRecordVO {

    private String id;
    private Integer delta;
    private String reason;
    private String operatorId;
    private String createdAt;
}
