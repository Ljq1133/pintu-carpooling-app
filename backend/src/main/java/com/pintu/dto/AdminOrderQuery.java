package com.pintu.dto;

import lombok.Data;

@Data
public class AdminOrderQuery {

    private String status;
    private String paymentStatus;
    private String keyword;
    private String dateFrom;
    private String dateTo;
}
