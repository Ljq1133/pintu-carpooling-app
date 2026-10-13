package com.pintu.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AppealTicketVO {
    private String id;
    private String orderId;
    private String applicantId;
    private String assigneeId;
    private String reasonType;
    private String description;
    private String status;
    private String resolution;
    private String route;
    private String applicantName;
    private String createdAt;
    private String updatedAt;
    private String closedAt;
    private List<AppealEvidenceVO> evidences = new ArrayList<>();
    private List<AppealTransitionVO> transitions = new ArrayList<>();
    private List<ArbitrationVO> arbitrations = new ArrayList<>();
}
