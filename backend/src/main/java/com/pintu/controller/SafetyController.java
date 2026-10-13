package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.DataRetentionPolicyVO;
import com.pintu.dto.EmergencyContactSaveRequest;
import com.pintu.dto.EmergencyContactVO;
import com.pintu.dto.LocationBatchReportRequest;
import com.pintu.dto.LocationPointVO;
import com.pintu.dto.LocationReportRequest;
import com.pintu.dto.SafetyAlertCreateRequest;
import com.pintu.dto.SafetyAlertVO;
import com.pintu.dto.TripShareCreateRequest;
import com.pintu.dto.TripShareLinkVO;
import com.pintu.dto.TripShareViewVO;
import com.pintu.dto.VirtualPhoneBindingVO;
import com.pintu.security.CurrentUser;
import com.pintu.service.SafetyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/safety")
@RequiredArgsConstructor
public class SafetyController {

    private final SafetyService safetyService;

    @GetMapping("/emergency-contacts")
    public Result<List<EmergencyContactVO>> listContacts(Authentication authentication) {
        return Result.ok(safetyService.listEmergencyContacts(CurrentUser.id(authentication)));
    }

    @PostMapping("/emergency-contacts")
    public Result<EmergencyContactVO> createContact(
            Authentication authentication,
            @RequestBody @Valid EmergencyContactSaveRequest request) {
        return Result.ok(safetyService.createEmergencyContact(CurrentUser.id(authentication), request));
    }

    @PutMapping("/emergency-contacts/{contactId}")
    public Result<EmergencyContactVO> updateContact(
            Authentication authentication,
            @PathVariable String contactId,
            @RequestBody @Valid EmergencyContactSaveRequest request) {
        return Result.ok(safetyService.updateEmergencyContact(CurrentUser.id(authentication), contactId, request));
    }

    @DeleteMapping("/emergency-contacts/{contactId}")
    public Result<Void> deleteContact(Authentication authentication, @PathVariable String contactId) {
        safetyService.deleteEmergencyContact(CurrentUser.id(authentication), contactId);
        return Result.ok();
    }

    @PostMapping("/orders/{orderId}/alerts")
    public Result<SafetyAlertVO> createOrderAlert(
            Authentication authentication,
            @PathVariable String orderId,
            @RequestBody @Valid SafetyAlertCreateRequest request) {
        return Result.ok(safetyService.createOrderAlert(CurrentUser.id(authentication), orderId, request));
    }

    @PostMapping("/orders/{orderId}/virtual-phone")
    public Result<VirtualPhoneBindingVO> bindVirtualPhone(
            Authentication authentication,
            @PathVariable String orderId) {
        return Result.ok(safetyService.ensureVirtualPhoneBound(orderId, CurrentUser.id(authentication)));
    }

    @GetMapping("/orders/{orderId}/virtual-phone")
    public Result<VirtualPhoneBindingVO> getVirtualPhone(
            Authentication authentication,
            @PathVariable String orderId) {
        return Result.ok(safetyService.getVirtualPhone(orderId, CurrentUser.id(authentication)));
    }

    @PostMapping("/orders/{orderId}/share-links")
    public Result<TripShareLinkVO> createShareLink(
            Authentication authentication,
            @PathVariable String orderId,
            @RequestBody(required = false) @Valid TripShareCreateRequest request) {
        return Result.ok(safetyService.createShareLink(CurrentUser.id(authentication), orderId, request));
    }

    @GetMapping("/share-links/{token}")
    public Result<TripShareViewVO> getShareView(@PathVariable String token) {
        return Result.ok(safetyService.getShareView(token));
    }

    @PostMapping("/orders/{orderId}/locations")
    public Result<LocationPointVO> reportLocation(
            Authentication authentication,
            @PathVariable String orderId,
            @RequestBody @Valid LocationReportRequest request) {
        return Result.ok(safetyService.reportDriverLocation(CurrentUser.id(authentication), orderId, request));
    }

    @PostMapping("/orders/{orderId}/locations/batch")
    public Result<List<LocationPointVO>> reportLocationBatch(
            Authentication authentication,
            @PathVariable String orderId,
            @RequestBody @Valid LocationBatchReportRequest request) {
        return Result.ok(safetyService.reportDriverLocationBatch(CurrentUser.id(authentication), orderId, request));
    }

    @GetMapping("/orders/{orderId}/trajectory")
    public Result<List<LocationPointVO>> trajectory(
            Authentication authentication,
            @PathVariable String orderId) {
        return Result.ok(safetyService.listTrajectory(CurrentUser.id(authentication), orderId));
    }

    @GetMapping("/retention-policies")
    public Result<List<DataRetentionPolicyVO>> retentionPolicies() {
        return Result.ok(safetyService.listRetentionPolicies());
    }
}
