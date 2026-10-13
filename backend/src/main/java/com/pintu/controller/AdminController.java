package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.AdminMeVO;
import com.pintu.dto.AdminOrderVO;
import com.pintu.dto.AdminOrderQuery;
import com.pintu.dto.AdminOrderSummaryVO;
import com.pintu.dto.AuditDecisionRequest;
import com.pintu.dto.AuditDetailVO;
import com.pintu.dto.AuditNotificationVO;
import com.pintu.dto.AuditVO;
import com.pintu.dto.CreditAdjustRequest;
import com.pintu.dto.UserVO;
import com.pintu.enums.AdminPermission;
import com.pintu.service.AdminService;
import com.pintu.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.security.core.Authentication;
import com.pintu.security.CurrentUser;

/**
 * 管理端接口。对应 C 组接口文档 §2。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;

    @GetMapping("/me")
    public Result<AdminMeVO> me(Authentication authentication) {
        return Result.ok(adminService.me(CurrentUser.id(authentication)));
    }

    @GetMapping("/audits/users")
    public Result<List<AuditVO>> userAudits(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        return Result.ok(adminService.listUserAudits(CurrentUser.id(authentication), status, page, size));
    }

    @GetMapping("/audits/{id}")
    public Result<AuditDetailVO> auditDetail(@PathVariable String id, Authentication authentication) {
        return Result.ok(adminService.auditDetail(CurrentUser.id(authentication), id));
    }

    @PostMapping("/audits/users/{id}/approve")
    public Result<AuditVO> approveUser(@PathVariable String id, Authentication authentication) {
        return Result.ok(adminService.approveUser(CurrentUser.id(authentication), id));
    }

    @PostMapping("/audits/users/{id}/reject")
    public Result<AuditVO> rejectUser(@PathVariable String id,
                                      @RequestBody(required = false) AuditDecisionRequest request,
                                      Authentication authentication) {
        return Result.ok(adminService.rejectUser(CurrentUser.id(authentication), id, request));
    }

    @GetMapping("/audits/vehicles")
    public Result<List<AuditVO>> vehicleAudits(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        return Result.ok(adminService.listVehicleAudits(CurrentUser.id(authentication), status, page, size));
    }

    @PostMapping("/audits/vehicles/{id}/approve")
    public Result<AuditVO> approveVehicle(@PathVariable String id, Authentication authentication) {
        return Result.ok(adminService.approveVehicle(CurrentUser.id(authentication), id));
    }

    @PostMapping("/audits/vehicles/{id}/reject")
    public Result<AuditVO> rejectVehicle(@PathVariable String id,
                                         @RequestBody(required = false) AuditDecisionRequest request,
                                         Authentication authentication) {
        return Result.ok(adminService.rejectVehicle(CurrentUser.id(authentication), id, request));
    }

    @PostMapping("/notifications/{id}/retry")
    public Result<AuditNotificationVO> retryNotification(@PathVariable String id, Authentication authentication) {
        return Result.ok(adminService.retryNotification(CurrentUser.id(authentication), id));
    }

    @GetMapping("/orders")
    public Result<List<AdminOrderVO>> orders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            Authentication authentication) {
        adminService.requirePermission(CurrentUser.id(authentication), AdminPermission.ORDER_VIEW.getCode());
        return Result.ok(orderService.listForAdmin(orderQuery(status, paymentStatus, keyword, dateFrom, dateTo)));
    }

    @GetMapping("/orders/summary")
    public Result<AdminOrderSummaryVO> orderSummary(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            Authentication authentication) {
        adminService.requirePermission(CurrentUser.id(authentication), AdminPermission.ORDER_VIEW.getCode());
        return Result.ok(orderService.summarizeForAdmin(orderQuery(status, paymentStatus, keyword, dateFrom, dateTo)));
    }

    @GetMapping("/users/credit")
    public Result<List<UserVO>> creditUsers(Authentication authentication) {
        return Result.ok(adminService.listCreditUsers(CurrentUser.id(authentication)));
    }

    @PostMapping("/users/{id}/credit")
    public Result<UserVO> adjustCredit(
            @PathVariable String id,
            @RequestBody @Valid CreditAdjustRequest request,
            Authentication authentication) {
        return Result.ok(adminService.adjustCredit(CurrentUser.id(authentication), id, request));
    }

    private AdminOrderQuery orderQuery(
            String status,
            String paymentStatus,
            String keyword,
            String dateFrom,
            String dateTo) {
        AdminOrderQuery query = new AdminOrderQuery();
        query.setStatus(status);
        query.setPaymentStatus(paymentStatus);
        query.setKeyword(keyword);
        query.setDateFrom(dateFrom);
        query.setDateTo(dateTo);
        return query;
    }
}
