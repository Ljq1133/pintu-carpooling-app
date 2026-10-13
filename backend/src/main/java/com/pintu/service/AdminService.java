package com.pintu.service;

import com.pintu.dto.AuditDecisionRequest;
import com.pintu.dto.AuditDetailVO;
import com.pintu.dto.AuditNotificationVO;
import com.pintu.dto.AuditVO;
import com.pintu.dto.CreditAdjustRequest;
import com.pintu.dto.AdminMeVO;
import com.pintu.dto.UserVO;

import java.util.List;

/**
 * 管理端服务：审核 + 信用分。
 */
public interface AdminService {

    AdminMeVO me(String adminId);

    void requirePermission(String adminId, String permission);

    List<AuditVO> listUserAudits(String adminId, String status, Integer page, Integer size);

    List<AuditVO> listVehicleAudits(String adminId, String status, Integer page, Integer size);

    AuditDetailVO auditDetail(String adminId, String auditId);

    AuditVO approveUser(String adminId, String auditId);

    AuditVO rejectUser(String adminId, String auditId, AuditDecisionRequest request);

    AuditVO approveVehicle(String adminId, String auditId);

    AuditVO rejectVehicle(String adminId, String auditId, AuditDecisionRequest request);

    AuditNotificationVO retryNotification(String adminId, String notificationId);

    UserVO adjustCredit(String adminId, String userId, CreditAdjustRequest request);

    List<UserVO> listCreditUsers(String adminId);
}
