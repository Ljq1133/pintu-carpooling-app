package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminOrderDetailVO {
    private String id;
    private String passengerOrderId;
    private String driverTripId;
    private String matchRequestId;
    private String passengerId;
    private String driverId;
    private String passenger;
    private String driver;
    private String route;
    private String startLocation;
    private String endLocation;
    private BigDecimal startLng;
    private BigDecimal startLat;
    private BigDecimal endLng;
    private BigDecimal endLat;
    private String departureTime;
    private String actualDeparture;
    private String actualArrival;
    private Integer passengerCount;
    private BigDecimal price;
    private String status;
    private String paymentStatus;
    private String reviewStatus;
    private String cancelReason;
    private String adminNote;
    private String createdAt;
    private String updatedAt;
    private LocationPointVO latestLocation;
    private List<LocationPointVO> trajectory = new ArrayList<>();
    private List<SafetyAlertVO> alerts = new ArrayList<>();
    private List<TimelineEvent> timeline = new ArrayList<>();
    private List<PaymentSnapshot> payments = new ArrayList<>();
    private List<ReviewSnapshot> reviews = new ArrayList<>();
    private List<CreditSnapshot> creditEvents = new ArrayList<>();
    private List<AppealSnapshot> appeals = new ArrayList<>();
    private List<NegotiationSnapshot> negotiations = new ArrayList<>();

    @Data
    public static class TimelineEvent {
        private String id;
        private String operatorId;
        private String operatorRole;
        private String fromStatus;
        private String toStatus;
        private String reason;
        private String createdAt;
    }

    @Data
    public static class PaymentSnapshot {
        private String id;
        private BigDecimal amount;
        private String method;
        private String status;
        private String paidAt;
        private String createdAt;
    }

    @Data
    public static class ReviewSnapshot {
        private String id;
        private String passengerId;
        private String driverId;
        private Integer rating;
        private String comment;
        private String createdAt;
    }

    @Data
    public static class CreditSnapshot {
        private String id;
        private String userId;
        private Integer delta;
        private String reason;
        private String operatorId;
        private String createdAt;
    }

    @Data
    public static class AppealSnapshot {
        private String id;
        private String applicantId;
        private String assigneeId;
        private String reasonType;
        private String description;
        private String status;
        private String resolution;
        private String createdAt;
        private String updatedAt;
        private String closedAt;
        private List<EvidenceSnapshot> evidence = new ArrayList<>();
        private List<ArbitrationSnapshot> arbitrations = new ArrayList<>();
    }

    @Data
    public static class EvidenceSnapshot {
        private String id;
        private String uploaderId;
        private String storageKey;
        private String contentType;
        private String createdAt;
    }

    @Data
    public static class ArbitrationSnapshot {
        private String id;
        private String appealId;
        private String adminId;
        private String actionType;
        private BigDecimal amount;
        private Integer creditDelta;
        private String remark;
        private String confirmedBy;
        private String createdAt;
    }

    @Data
    public static class NegotiationSnapshot {
        private String id;
        private String conversationId;
        private Integer version;
        private String proposerId;
        private BigDecimal price;
        private String departureTime;
        private Integer passengerCount;
        private String pickupLocation;
        private String note;
        private String createdAt;
    }
}
