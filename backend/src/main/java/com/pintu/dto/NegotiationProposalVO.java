package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class NegotiationProposalVO {
    private String id;
    private String conversationId;
    private Integer version;
    private String proposerId;
    private BigDecimal price;
    private String departureTime;
    private Integer passengerCount;
    private String pickupLocation;
    private BigDecimal pickupLng;
    private BigDecimal pickupLat;
    private String note;
    private List<String> confirmedRoles;
    private String orderId;
    private String createdAt;
}
