package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.AdminOpsStatisticsVO;
import com.pintu.dto.AdminOrderDetailVO;
import com.pintu.dto.AdminOrderQuery;
import com.pintu.dto.AdminSafetyOrderVO;
import com.pintu.dto.DataRetentionPolicyRequest;
import com.pintu.dto.DataRetentionPolicyVO;
import com.pintu.dto.LocationPointVO;
import com.pintu.dto.RetentionCleanupRequest;
import com.pintu.dto.RetentionCleanupVO;
import com.pintu.dto.RuleConfigRequest;
import com.pintu.dto.RuleConfigVO;
import com.pintu.dto.RuleRollbackRequest;
import com.pintu.dto.SafetyAlertVO;
import com.pintu.dto.UserRestrictionRequest;
import com.pintu.dto.UserRestrictionVO;
import com.pintu.security.CurrentUser;
import com.pintu.service.AdminGovernanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminGovernanceController {

    private final AdminGovernanceService service;

    @GetMapping("/safety/orders")
    public Result<List<AdminSafetyOrderVO>> safetyOrders(Authentication authentication) {
        return Result.ok(service.listSafetyOrders(CurrentUser.id(authentication)));
    }

    @GetMapping("/safety/orders/{orderId}/trajectory")
    public Result<List<LocationPointVO>> safetyTrajectory(
            @PathVariable String orderId,
            Authentication authentication) {
        return Result.ok(service.listSafetyTrajectory(CurrentUser.id(authentication), orderId));
    }

    @GetMapping("/safety/alerts")
    public Result<List<SafetyAlertVO>> safetyAlerts(Authentication authentication) {
        return Result.ok(service.listSafetyAlerts(CurrentUser.id(authentication)));
    }

    @GetMapping("/safety/retention-policies")
    public Result<List<DataRetentionPolicyVO>> retentionPolicies(Authentication authentication) {
        return Result.ok(service.listRetentionPolicies(CurrentUser.id(authentication)));
    }

    @PutMapping("/safety/retention-policies")
    public Result<DataRetentionPolicyVO> saveRetentionPolicy(
            @RequestBody @Valid DataRetentionPolicyRequest request,
            Authentication authentication) {
        return Result.ok(service.saveRetentionPolicy(CurrentUser.id(authentication), request));
    }

    @PostMapping("/safety/retention-cleanup")
    public Result<RetentionCleanupVO> cleanupRetention(
            @RequestBody(required = false) RetentionCleanupRequest request,
            Authentication authentication) {
        return Result.ok(service.cleanupRetention(CurrentUser.id(authentication), request));
    }

    @GetMapping("/orders/{id}")
    public Result<AdminOrderDetailVO> orderDetail(
            @PathVariable String id,
            Authentication authentication) {
        return Result.ok(service.getOrderDetail(CurrentUser.id(authentication), id));
    }

    @GetMapping("/ops/statistics")
    public Result<AdminOpsStatisticsVO> opsStatistics(Authentication authentication) {
        return Result.ok(service.getOpsStatistics(CurrentUser.id(authentication)));
    }

    @GetMapping("/orders/export.csv")
    public ResponseEntity<String> exportOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            Authentication authentication) {
        AdminOrderQuery query = new AdminOrderQuery();
        query.setStatus(status);
        query.setPaymentStatus(paymentStatus);
        query.setKeyword(keyword);
        query.setDateFrom(dateFrom);
        query.setDateTo(dateTo);
        String csv = service.exportOrdersCsv(CurrentUser.id(authentication), query);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("pintu-orders.csv", StandardCharsets.UTF_8).build().toString())
                .body(csv);
    }

    @GetMapping("/rules")
    public Result<List<RuleConfigVO>> rules(Authentication authentication) {
        return Result.ok(service.listRules(CurrentUser.id(authentication)));
    }

    @PostMapping("/rules")
    public Result<RuleConfigVO> createRule(
            @RequestBody @Valid RuleConfigRequest request,
            Authentication authentication) {
        return Result.ok(service.createRule(CurrentUser.id(authentication), request));
    }

    @PutMapping("/rules/{id}")
    public Result<RuleConfigVO> updateRule(
            @PathVariable String id,
            @RequestBody @Valid RuleConfigRequest request,
            Authentication authentication) {
        return Result.ok(service.updateRule(CurrentUser.id(authentication), id, request));
    }

    @PostMapping("/rules/{id}/publish")
    public Result<RuleConfigVO> publishRule(@PathVariable String id, Authentication authentication) {
        return Result.ok(service.publishRule(CurrentUser.id(authentication), id));
    }

    @PostMapping("/rules/{id}/rollback")
    public Result<RuleConfigVO> rollbackRule(
            @PathVariable String id,
            @RequestBody @Valid RuleRollbackRequest request,
            Authentication authentication) {
        return Result.ok(service.rollbackRule(CurrentUser.id(authentication), id, request));
    }

    @PostMapping("/users/{id}/restrictions")
    public Result<UserRestrictionVO> restrictUser(
            @PathVariable String id,
            @RequestBody @Valid UserRestrictionRequest request,
            Authentication authentication) {
        return Result.ok(service.restrictUser(CurrentUser.id(authentication), id, request));
    }

    @DeleteMapping("/users/{id}/restrictions/{type}")
    public Result<Void> releaseRestriction(
            @PathVariable String id,
            @PathVariable String type,
            Authentication authentication) {
        service.releaseRestriction(CurrentUser.id(authentication), id, type);
        return Result.ok();
    }
}
