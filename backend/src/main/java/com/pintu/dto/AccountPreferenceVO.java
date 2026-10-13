package com.pintu.dto;

import lombok.Data;

@Data
public class AccountPreferenceVO {
    private Boolean darkMode;
    private String fontScale;
    private String locale;
    private String protocolVersion;
    private String updatedAt;
}
