package com.pintu.service.impl;

import com.pintu.common.exception.BusinessException;
import com.pintu.dto.AdminMeVO;
import com.pintu.dto.AuditDecisionRequest;
import com.pintu.dto.AuditVO;
import com.pintu.dto.CreditAdjustRequest;
import com.pintu.entity.AdminRoleAssignment;
import com.pintu.entity.AdminRolePermission;
import com.pintu.entity.AuditHistory;
import com.pintu.entity.AuditRecord;
import com.pintu.entity.User;
import com.pintu.enums.AdminPermission;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceImplTest {

    private AuditRecordMapper auditRecordMapper;
    private UserMapper userMapper;
    private AuditHistoryMapper auditHistoryMapper;
    private AdminRoleAssignmentMapper roleAssignmentMapper;
    private AdminRolePermissionMapper rolePermissionMapper;
    private AdminServiceImpl service;

    @BeforeEach
    void setUp() {
        auditRecordMapper = mock(AuditRecordMapper.class);
        userMapper = mock(UserMapper.class);
        auditHistoryMapper = mock(AuditHistoryMapper.class);
        roleAssignmentMapper = mock(AdminRoleAssignmentMapper.class);
        rolePermissionMapper = mock(AdminRolePermissionMapper.class);
        service = new AdminServiceImpl(
                auditRecordMapper,
                userMapper,
                mock(VehicleMapper.class),
                mock(CreditRecordMapper.class),
                mock(SensitiveDataCrypto.class),
                mock(IdentityApplicationMapper.class),
                mock(UserRoleMapper.class),
                auditHistoryMapper,
                mock(NotificationMapper.class),
                mock(IdentityAttachmentMapper.class),
                mock(VehicleAttachmentMapper.class),
                roleAssignmentMapper,
                rolePermissionMapper
        );
        when(userMapper.selectById("admin-001")).thenReturn(admin("admin-001"));
    }

    @Test
    void adminMeReturnsSuperAdminPermissions() {
        grant("SUPER_ADMIN",
                AdminPermission.AUDIT_REVIEW.getCode(),
                AdminPermission.ORDER_VIEW.getCode(),
                AdminPermission.CREDIT_MANAGE.getCode());

        AdminMeVO result = service.me("admin-001");

        assertEquals(List.of("SUPER_ADMIN"), result.getSubRoles());
        assertEquals(9, result.getPermissions().size());
        assertEquals("超级管理员", result.getSubRoleLabels().get(0));
    }

    @Test
    void approveUserRejectsAdminWithoutAuditPermission() {
        grant("OPS", AdminPermission.ORDER_VIEW.getCode());

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.approveUser("admin-001", "audit-001")
        );

        assertEquals(1004, error.getCode());
    }

    @Test
    void adjustCreditRejectsAdminWithoutCreditPermission() {
        grant("AUDITOR", AdminPermission.AUDIT_REVIEW.getCode());
        CreditAdjustRequest request = new CreditAdjustRequest();
        request.setDelta(-10);
        request.setReason("测试扣分");

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.adjustCredit("admin-001", "passenger-001", request)
        );

        assertEquals(1004, error.getCode());
    }

    @Test
    void rejectUserRequiresReasonBeforeChangingAudit() {
        grant("AUDITOR", AdminPermission.AUDIT_REVIEW.getCode());
        AuditDecisionRequest request = new AuditDecisionRequest();
        request.setReason(" ");

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.rejectUser("admin-001", "audit-001", request)
        );

        assertEquals(1002, error.getCode());
    }

    @Test
    void rejectUserWritesAuditHistory() {
        grant("AUDITOR", AdminPermission.AUDIT_REVIEW.getCode());
        AuditRecord audit = audit("audit-001", "user", "identity-001");
        when(auditRecordMapper.selectById("audit-001")).thenReturn(audit);
        AuditDecisionRequest request = new AuditDecisionRequest();
        request.setReason("证件材料不清晰");

        AuditVO result = service.rejectUser("admin-001", "audit-001", request);

        assertEquals(AuditStatus.REJECTED.getValue(), result.getStatus());
        assertEquals("证件材料不清晰", audit.getReason());
        verify(auditHistoryMapper).insert(any(AuditHistory.class));
        verify(auditRecordMapper).updateById(audit);
    }

    private void grant(String role, String... permissions) {
        AdminRoleAssignment assignment = new AdminRoleAssignment();
        assignment.setAdminId("admin-001");
        assignment.setSubRole(role);
        assignment.setStatus("ACTIVE");
        when(roleAssignmentMapper.selectList(any())).thenReturn(List.of(assignment));
        when(rolePermissionMapper.selectList(any())).thenReturn(
                java.util.Arrays.stream(permissions).map((permission) -> permission(role, permission)).toList()
        );
    }

    private AdminRolePermission permission(String role, String code) {
        AdminRolePermission permission = new AdminRolePermission();
        permission.setSubRole(role);
        permission.setPermission(code);
        return permission;
    }

    private User admin(String id) {
        User user = new User();
        user.setId(id);
        user.setUsername("admin_demo");
        user.setName("平台管理员");
        user.setRole("admin");
        return user;
    }

    private AuditRecord audit(String id, String type, String targetId) {
        AuditRecord audit = new AuditRecord();
        audit.setId(id);
        audit.setTargetType(type);
        audit.setTargetId(targetId);
        audit.setName("王测试");
        audit.setStatus(AuditStatus.PENDING.getValue());
        audit.setSubmittedAt(LocalDateTime.of(2026, 6, 14, 20, 0));
        return audit;
    }
}
