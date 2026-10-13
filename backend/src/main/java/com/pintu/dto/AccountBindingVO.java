package com.pintu.dto;

import lombok.Data;

@Data
public class AccountBindingVO {
    private String id;
    private String bindingType;
    private String provider;
    private String accountLabel;
    private String maskedValue;
    private String status;
    private String createdAt;
    private String updatedAt;
}
