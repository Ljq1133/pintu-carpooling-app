package com.pintu.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AdminOrderSummaryVO {

    private int total;
    private int pendingPayment;
    private int completed;
    private int cancelled;
    private Map<String, Integer> statusCounts = new LinkedHashMap<>();
    private Map<String, Integer> paymentStatusCounts = new LinkedHashMap<>();
}
