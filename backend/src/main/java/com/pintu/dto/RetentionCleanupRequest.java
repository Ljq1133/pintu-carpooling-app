package com.pintu.dto;

import lombok.Data;

@Data
public class RetentionCleanupRequest {
    private Boolean dryRun = true;
}
