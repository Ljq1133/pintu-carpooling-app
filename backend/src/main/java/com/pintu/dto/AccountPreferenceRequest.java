package com.pintu.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AccountPreferenceRequest {
    private Boolean darkMode;

    @Pattern(regexp = "compact|standard|large", message = "fontScale must be compact, standard or large")
    private String fontScale;

    @Pattern(regexp = "zh-CN|en-US", message = "locale must be zh-CN or en-US")
    private String locale;
    private String protocolVersion;
}
