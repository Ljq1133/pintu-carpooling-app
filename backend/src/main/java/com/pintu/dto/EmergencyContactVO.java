package com.pintu.dto;

import lombok.Data;

@Data
public class EmergencyContactVO {

    private String id;
    private String name;
    private String phoneMasked;
    private String relationship;
    private Boolean isPrimary;
    private String createdAt;
    private String updatedAt;
}
