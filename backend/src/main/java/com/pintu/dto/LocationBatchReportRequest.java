package com.pintu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class LocationBatchReportRequest {
    @Size(max = 64)
    private String batchId;

    @NotEmpty
    @Size(max = 50)
    @Valid
    private List<LocationReportRequest> points;
}
