package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.Masking;
import com.pintu.common.util.TimeFormatter;
import com.pintu.dto.AdminMeVO;
import com.pintu.dto.AdminPermissionVO;
import com.pintu.dto.AuditDecisionRequest;
import com.pintu.dto.AuditAttachmentVO;
import com.pintu.dto.AuditDetailVO;
import com.pintu.dto.AuditHistoryVO;
import com.pintu.dto.AuditNotificationVO;
import com.pintu.dto.AuditVO;
import com.pintu.dto.CreditAdjustRequest;
import com.pintu.dto.UserVO;
import com.pintu.entity.AdminRoleAssignment;
import com.pintu.entity.AdminRolePermission;
import com.pintu.entity.AuditHistory;
import com.pintu.entity.AuditRecord;
import com.pintu.entity.CreditRecord;
import com.pintu.entity.IdentityApplication;
import com.pintu.entity.IdentityAttachment;
import com.pintu.entity.Notification;
import com.pintu.entity.User;
import com.pintu.entity.UserRole;
import com.pintu.entity.Vehicle;
import com.pintu.entity.VehicleAttachment;
import com.pintu.enums.AdminPermission;
import com.pintu.enums.AdminSubRole;
import com.pintu.enums.AuditStatus;
import com.pintu.mapper.AdminRoleAssignmentMapper;
import com.pintu.mapper.AdminRolePermissionMapper;
import com.pintu.mapper.AuditHistoryMapper;
import com.pintu.mapper.AuditRecordMapper;
import com.pintu.mapper.CreditRecordMapper;
import com.pintu.mapper.IdentityApplicationMapper;
import com.pintu.mapper.IdentityAttachmentMapper;
import com.pintu.mapper.NotificationMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.UserRoleMapper;
import com.pintu.mapper.VehicleAttachmentMapper;
import com.pintu.mapper.VehicleMapper;
import com.pintu.security.SensitiveDataCrypto;
import com.pintu.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final String ACTIVE = "ACTIVE";

    private final AuditRecordMapper auditRecordMapper;
    private final UserMapper userMapper;
    private final VehicleMapper vehicleMapper;
    private final CreditRecordMapper creditRecordMapper;
    private final SensitiveDataCrypto sensitiveDataCrypto;
    private final IdentityApplicationMapper identityApplicationMapper;
    private final UserRoleMapper userRoleMapper;
    private final AuditHistoryMapper auditHistoryMapper;
    private final NotificationMapper notificationMapper;
    private final IdentityAttachmentMapper identityAttachmentMapper;
    private final VehicleAttachmentMapper vehicleAttachmentMapper;
    private final AdminRoleAssignmentMapper adminRoleAssignmentMapper;
    private final AdminRolePermissionMapper adminRolePermissionMapper;

    @Override
    public AdminMeVO me(String adminId) {
        User admin = requireAdminUser(adminId);
        List<String> subRoles = listActiveSubRoles(adminId);
        Set<String> permissions = listPermissionCodes(subRoles);

        AdminMeVO vo = new AdminMeVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setName(admin.getName());
        vo.setRole(admin.getRole());
        vo.setSubRoles(subRoles);
        vo.setSubRoleLabels(subRoles.stream().map(AdminSubRole::labelOf).toList());
        vo.setPermissions(permissions.stream().map(this::toPermissionVO).toList());
        return vo;
    }

    @Override
    public void requirePermission(String adminId, String permission) {
        requireAdminUser(adminId);
        if (!listPermissionCodes(listActiveSubRoles(adminId)).contains(permission)) {
            throw new BusinessException(1004, "缺少管理权限：" + AdminPermission.labelOf(permission));
        }
    }

    @Override
    public List<AuditVO> listUserAudits(String adminId, String status, Integer page, Integer size) {
        requirePermission(adminId, AdminPermission.AUDIT_REVIEW.getCode());
        return listAuditsByType("user", status, page, size);
    }

    @Override
    public List<AuditVO> listVehicleAudits(String adminId, String status, Integer page, Integer size) {
        requirePermission(adminId, AdminPermission.AUDIT_REVIEW.getCode());
        return listAuditsByType("vehicle", status, page, size);
    }

    @Override
    public AuditDetailVO auditDetail(String adminId, String auditId) {
        requirePermission(adminId, AdminPermission.AUDIT_REVIEW.getCode());
        AuditRecord record = requireAudit(auditId);
        AuditDetailVO detail = toAuditDetailVO(record);
        List<AuditHistory> histories = auditHistoryMapper.selectList(new QueryWrapper<AuditHistory>()
                .eq("audit_record_id", auditId)
                .orderByDesc("created_at"));
        detail.setHistories(histories.stream().map(this::toAuditHistoryVO).toList());
        detail.setAttachments(loadAuditAttachments(record));
        detail.setNotifications(notificationMapper.selectList(new QueryWrapper<Notification>()
                        .eq("biz_type", record.getTargetType())
                        .eq("biz_id", record.getTargetId())
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toAuditNotificationVO)
                .toList());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuditVO approveUser(String adminId, String auditId) {
        requirePermission(adminId, AdminPermission.AUDIT_REVIEW.getCode());
        AuditVO vo = reviewAudit(auditId, AuditStatus.APPROVED.getValue(), null, adminId);
        syncIdentityStatus(auditId, AuditStatus.APPROVED.getValue(), null, ACTIVE);
        notifyAuditResult(auditId, true, null);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuditVO rejectUser(String adminId, String auditId, AuditDecisionRequest request) {
        requirePermission(adminId, AdminPermission.AUDIT_REVIEW.getCode());
        String reason = requireRejectReason(request);
        AuditVO vo = reviewAudit(auditId, AuditStatus.REJECTED.getValue(), reason, adminId);
        syncIdentityStatus(auditId, AuditStatus.REJECTED.getValue(), reason, "REJECTED");
        notifyAuditResult(auditId, false, reason);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuditVO approveVehicle(String adminId, String auditId) {
        requirePermission(adminId, AdminPermission.AUDIT_REVIEW.getCode());
        AuditVO vo = reviewAudit(auditId, AuditStatus.APPROVED.getValue(), null, adminId);
        syncVehicleStatus(auditId, "已认证");
        notifyAuditResult(auditId, true, null);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuditVO rejectVehicle(String adminId, String auditId, AuditDecisionRequest request) {
        requirePermission(adminId, AdminPermission.AUDIT_REVIEW.getCode());
        String reason = requireRejectReason(request);
        AuditVO vo = reviewAudit(auditId, AuditStatus.REJECTED.getValue(), reason, adminId);
        syncVehicleStatus(auditId, "已驳回");
        notifyAuditResult(auditId, false, reason);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuditNotificationVO retryNotification(String adminId, String notificationId) {
        requirePermission(adminId, AdminPermission.AUDIT_REVIEW.getCode());
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(1404, "通知不存在：" + notificationId);
        }
        notification.setSendStatus("已发送");
        notification.setRetryCount((notification.getRetryCount() == null ? 0 : notification.getRetryCount()) + 1);
        notification.setNextRetryAt(null);
        notificationMapper.updateById(notification);
        return toAuditNotificationVO(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO adjustCredit(String adminId, String userId, CreditAdjustRequest request) {
        requirePermission(adminId, AdminPermission.CREDIT_MANAGE.getCode());
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(1404, "用户不存在：" + userId);
        }

        int next = (user.getCredit() == null ? 100 : user.getCredit()) + request.getDelta();
        user.setCredit(Math.max(0, Math.min(100, next)));

        if (user.getCredit() < 60) {
            user.setStatus("driver".equals(user.getRole()) ? "限制接单" : "限制发单");
        } else if (user.getCredit() < 80) {
            user.setStatus("观察");
        } else {
            user.setStatus("正常");
        }
        userMapper.updateById(user);

        CreditRecord record = new CreditRecord();
        record.setId(IdGenerator.next(IdGenerator.PREFIX_CREDIT));
        record.setUserId(userId);
        record.setDelta(request.getDelta());
        record.setReason(request.getReason());
        record.setOperatorId(adminId);
        creditRecordMapper.insert(record);

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public List<UserVO> listCreditUsers(String adminId) {
        requirePermission(adminId, AdminPermission.CREDIT_MANAGE.getCode());
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.in("role", "passenger", "driver").orderByDesc("credit");
        List<User> users = userMapper.selectList(wrapper);
        List<UserVO> result = new ArrayList<>();
        for (User user : users) {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            vo.setName(Masking.name(user.getName()));
            vo.setPhone(Masking.phone(sensitiveDataCrypto.decrypt(user.getPhone())));
            result.add(vo);
        }
        return result;
    }

    private List<AuditVO> listAuditsByType(String type, String status, Integer page, Integer size) {
        QueryWrapper<AuditRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("target_type", type);
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("submitted_at");
        applyPage(wrapper, page, size);
        return auditRecordMapper.selectList(wrapper).stream().map(this::toAuditVO).toList();
    }

    private void applyPage(QueryWrapper<AuditRecord> wrapper, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;
        wrapper.last("LIMIT " + safeSize + " OFFSET " + offset);
    }

    private User requireAdminUser(String adminId) {
        User admin = userMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(1404, "管理员不存在：" + adminId);
        }
        if (!"admin".equals(admin.getRole())) {
            throw new BusinessException(1004, "当前账号不是管理员");
        }
        return admin;
    }

    private List<String> listActiveSubRoles(String adminId) {
        List<AdminRoleAssignment> assignments = adminRoleAssignmentMapper.selectList(new QueryWrapper<AdminRoleAssignment>()
                .eq("admin_id", adminId)
                .eq("status", ACTIVE)
                .orderByAsc("sub_role"));
        return assignments.stream().map(AdminRoleAssignment::getSubRole).distinct().toList();
    }

    private Set<String> listPermissionCodes(List<String> subRoles) {
        Set<String> result = new LinkedHashSet<>();
        if (subRoles.isEmpty()) {
            return result;
        }
        List<AdminRolePermission> records = adminRolePermissionMapper.selectList(new QueryWrapper<AdminRolePermission>()
                .in("sub_role", subRoles)
                .orderByAsc("permission"));
        for (AdminRolePermission record : records) {
            result.add(record.getPermission());
        }
        applyDerivedPermissions(subRoles, result);
        return result;
    }

    private void applyDerivedPermissions(List<String> subRoles, Set<String> permissions) {
        if (subRoles.contains(AdminSubRole.SUPER_ADMIN.name())) {
            for (AdminPermission permission : AdminPermission.values()) {
                permissions.add(permission.getCode());
            }
            return;
        }
        if (subRoles.contains(AdminSubRole.CUSTOMER_SERVICE.name())) {
            permissions.add(AdminPermission.APPEAL_MANAGE.getCode());
            permissions.add(AdminPermission.ARBITRATION_MANAGE.getCode());
            permissions.add(AdminPermission.SUPPORT_MANAGE.getCode());
        }
        if (subRoles.contains(AdminSubRole.OPS.name())) {
            permissions.add(AdminPermission.SAFETY_MONITOR.getCode());
            permissions.add(AdminPermission.RULE_MANAGE.getCode());
            permissions.add(AdminPermission.USER_RESTRICT.getCode());
        }
    }

    private AdminPermissionVO toPermissionVO(String code) {
        AdminPermissionVO vo = new AdminPermissionVO();
        vo.setCode(code);
        vo.setLabel(AdminPermission.labelOf(code));
        return vo;
    }

    private AuditRecord requireAudit(String auditId) {
        AuditRecord record = auditRecordMapper.selectById(auditId);
        if (record == null) {
            throw new BusinessException(1404, "审核记录不存在：" + auditId);
        }
        return record;
    }

    private String requireRejectReason(AuditDecisionRequest request) {
        String reason = request == null ? "" : request.getReason();
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(1002, "驳回原因不能为空");
        }
        return reason.trim();
    }

    private AuditVO reviewAudit(String auditId, String status, String reason, String adminId) {
        AuditRecord record = requireAudit(auditId);
        if (!AuditStatus.PENDING.getValue().equals(record.getStatus())) {
            throw new BusinessException(1018, "审核记录已处理：" + record.getStatus());
        }
        String fromStatus = record.getStatus();
        record.setStatus(status);
        record.setReason(reason);
        record.setHandledAt(TimeFormatter.nowDateTime());
        auditRecordMapper.updateById(record);

        AuditHistory history = new AuditHistory();
        history.setId(IdGenerator.next("ah"));
        history.setAuditRecordId(auditId);
        history.setAdminId(adminId);
        history.setFromStatus(fromStatus);
        history.setToStatus(status);
        history.setReason(reason);
        history.setCreatedAt(TimeFormatter.nowDateTime());
        auditHistoryMapper.insert(history);

        return toAuditVO(record);
    }

    private AuditVO toAuditVO(AuditRecord record) {
        AuditVO vo = new AuditVO();
        vo.setId(record.getId());
        vo.setTargetType(record.getTargetType());
        vo.setTargetId(record.getTargetId());
        vo.setName(Masking.name(record.getName()));
        vo.setPlate(Masking.plate(record.getPlate()));
        vo.setStatus(record.getStatus());
        vo.setReason(record.getReason());
        vo.setSubmittedAt(TimeFormatter.format(record.getSubmittedAt()));
        vo.setHandledAt(TimeFormatter.format(record.getHandledAt()));
        return vo;
    }

    private AuditDetailVO toAuditDetailVO(AuditRecord record) {
        AuditDetailVO vo = new AuditDetailVO();
        vo.setId(record.getId());
        vo.setTargetType(record.getTargetType());
        vo.setTargetId(record.getTargetId());
        vo.setName(Masking.name(record.getName()));
        vo.setPlate(Masking.plate(record.getPlate()));
        vo.setStatus(record.getStatus());
        vo.setReason(record.getReason());
        vo.setSubmittedAt(TimeFormatter.format(record.getSubmittedAt()));
        vo.setHandledAt(TimeFormatter.format(record.getHandledAt()));
        return vo;
    }

    private AuditHistoryVO toAuditHistoryVO(AuditHistory history) {
        AuditHistoryVO vo = new AuditHistoryVO();
        vo.setId(history.getId());
        vo.setAdminId(history.getAdminId());
        vo.setFromStatus(history.getFromStatus());
        vo.setToStatus(history.getToStatus());
        vo.setReason(history.getReason());
        vo.setCreatedAt(TimeFormatter.format(history.getCreatedAt()));
        return vo;
    }

    private List<AuditAttachmentVO> loadAuditAttachments(AuditRecord record) {
        if ("vehicle".equals(record.getTargetType())) {
            return vehicleAttachmentMapper.selectList(new QueryWrapper<VehicleAttachment>()
                            .eq("vehicle_id", record.getTargetId())
                            .orderByDesc("created_at"))
                    .stream()
                    .map(this::toAuditAttachmentVO)
                    .toList();
        }
        return identityAttachmentMapper.selectList(new QueryWrapper<IdentityAttachment>()
                        .eq("application_id", record.getTargetId())
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toAuditAttachmentVO)
                .toList();
    }

    private AuditAttachmentVO toAuditAttachmentVO(IdentityAttachment attachment) {
        AuditAttachmentVO vo = new AuditAttachmentVO();
        vo.setId(attachment.getId());
        vo.setAttachmentType(attachment.getAttachmentType());
        vo.setStorageKey(attachment.getStorageKey());
        vo.setContentType(attachment.getContentType());
        vo.setSizeBytes(attachment.getSizeBytes());
        vo.setCreatedAt(TimeFormatter.format(attachment.getCreatedAt()));
        return vo;
    }

    private AuditAttachmentVO toAuditAttachmentVO(VehicleAttachment attachment) {
        AuditAttachmentVO vo = new AuditAttachmentVO();
        vo.setId(attachment.getId());
        vo.setAttachmentType(attachment.getAttachmentType());
        vo.setStorageKey(attachment.getStorageKey());
        vo.setContentType(attachment.getContentType());
        vo.setSizeBytes(attachment.getSizeBytes());
        vo.setCreatedAt(TimeFormatter.format(attachment.getCreatedAt()));
        return vo;
    }

    private AuditNotificationVO toAuditNotificationVO(Notification notification) {
        AuditNotificationVO vo = new AuditNotificationVO();
        vo.setId(notification.getId());
        vo.setTitle(notification.getTitle());
        vo.setSendStatus(notification.getSendStatus());
        vo.setRetryCount(notification.getRetryCount());
        vo.setCreatedAt(TimeFormatter.format(notification.getCreatedAt()));
        return vo;
    }

    private void syncVehicleStatus(String auditId, String vehicleStatus) {
        AuditRecord record = auditRecordMapper.selectById(auditId);
        if (record == null || record.getTargetId() == null) return;
        Vehicle vehicle = vehicleMapper.selectById(record.getTargetId());
        if (vehicle != null) {
            vehicle.setStatus(vehicleStatus);
            vehicleMapper.updateById(vehicle);
        }
    }

    private void syncIdentityStatus(
            String auditId,
            String applicationStatus,
            String reason,
            String roleStatus) {
        AuditRecord record = auditRecordMapper.selectById(auditId);
        if (record == null || record.getTargetId() == null) {
            return;
        }
        IdentityApplication application = identityApplicationMapper.selectById(record.getTargetId());
        if (application == null) {
            return;
        }
        application.setStatus(applicationStatus);
        application.setRejectReason(reason);
        application.setHandledAt(TimeFormatter.nowDateTime());
        identityApplicationMapper.updateById(application);

        UserRole role = userRoleMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, application.getUserId())
                        .eq(UserRole::getRole, "driver"));
        if (role != null) {
            role.setStatus(roleStatus);
            userRoleMapper.updateById(role);
        }
    }

    private void notifyAuditResult(String auditId, boolean approved, String reason) {
        AuditRecord record = auditRecordMapper.selectById(auditId);
        String userId = resolveAuditOwner(record);
        if (userId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setId(IdGenerator.next("notice"));
        notification.setUserId(userId);
        notification.setType("AUDIT_RESULT");
        notification.setTitle(approved ? "Audit approved" : "Audit rejected");
        notification.setContent(approved
                ? "Your qualification audit was approved."
                : "Your qualification audit was rejected: "
                    + (reason == null ? "No reason provided" : reason));
        notification.setBizType(record.getTargetType());
        notification.setBizId(record.getTargetId());
        notification.setSendStatus("已发送");
        notification.setRetryCount(0);
        notification.setCreatedAt(TimeFormatter.nowDateTime());
        notificationMapper.insert(notification);
    }

    private String resolveAuditOwner(AuditRecord record) {
        if (record == null) {
            return null;
        }
        if ("vehicle".equals(record.getTargetType())) {
            Vehicle vehicle = vehicleMapper.selectById(record.getTargetId());
            return vehicle == null ? null : vehicle.getDriverId();
        }
        IdentityApplication application = identityApplicationMapper.selectById(record.getTargetId());
        if (application != null) {
            return application.getUserId();
        }
        return userMapper.selectById(record.getTargetId()) == null ? null : record.getTargetId();
    }
}
