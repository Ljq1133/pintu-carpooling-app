package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pintu.dto.AdminOrderQuery;
import com.pintu.dto.AdminOrderVO;
import com.pintu.dto.UserRestrictionRequest;
import com.pintu.dto.UserRestrictionVO;
import com.pintu.entity.BanRecord;
import com.pintu.entity.User;
import com.pintu.mapper.BanRecordMapper;
import com.pintu.mapper.DataRetentionPolicyMapper;
import com.pintu.mapper.LocationReportMapper;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.OrderStatusHistoryMapper;
import com.pintu.mapper.PaymentMapper;
import com.pintu.mapper.ReviewMapper;
import com.pintu.mapper.CreditRecordMapper;
import com.pintu.mapper.AppealTicketMapper;
import com.pintu.mapper.AppealEvidenceMapper;
import com.pintu.mapper.ArbitrationRecordMapper;
import com.pintu.mapper.ConversationMapper;
import com.pintu.mapper.NegotiationProposalMapper;
import com.pintu.mapper.RuleConfigMapper;
import com.pintu.mapper.RuleRollbackMapper;
import com.pintu.mapper.RuleVersionMapper;
import com.pintu.mapper.SafetyAlertMapper;
import com.pintu.mapper.TrajectoryPointMapper;
import com.pintu.mapper.TripShareTokenMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.VirtualPhoneBindingMapper;
import com.pintu.service.AdminService;
import com.pintu.service.OrderService;
import com.pintu.service.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminGovernanceServiceImplTest {

    private AdminService adminService;
    private OrderService orderService;
    private SecurityAuditService auditService;
    private UserMapper userMapper;
    private BanRecordMapper banRecordMapper;
    private AdminGovernanceServiceImpl service;

    @BeforeEach
    void setUp() {
        adminService = mock(AdminService.class);
        orderService = mock(OrderService.class);
        auditService = mock(SecurityAuditService.class);
        userMapper = mock(UserMapper.class);
        banRecordMapper = mock(BanRecordMapper.class);
        service = new AdminGovernanceServiceImpl(
                adminService,
                orderService,
                auditService,
                mock(OrderRecordMapper.class),
                userMapper,
                mock(LocationReportMapper.class),
                mock(TrajectoryPointMapper.class),
                mock(VirtualPhoneBindingMapper.class),
                mock(TripShareTokenMapper.class),
                mock(SafetyAlertMapper.class),
                mock(OrderStatusHistoryMapper.class),
                mock(PaymentMapper.class),
                mock(ReviewMapper.class),
                mock(CreditRecordMapper.class),
                mock(AppealTicketMapper.class),
                mock(AppealEvidenceMapper.class),
                mock(ArbitrationRecordMapper.class),
                mock(ConversationMapper.class),
                mock(NegotiationProposalMapper.class),
                mock(RuleConfigMapper.class),
                mock(RuleVersionMapper.class),
                mock(RuleRollbackMapper.class),
                mock(DataRetentionPolicyMapper.class),
                banRecordMapper,
                new ObjectMapper()
        );
    }

    @Test
    void exportOrdersCsvUsesMaskedRowsAndWritesAudit() {
        AdminOrderVO order = new AdminOrderVO();
        order.setId("order-1");
        order.setRoute("A -> B");
        order.setPassenger("张**");
        order.setDriver("李**");
        order.setStatus("履约中");
        order.setPaymentStatus("未支付");
        order.setPrice(new BigDecimal("18.6"));
        order.setRisk("正常");
        order.setCreatedAt("2026-06-16 20:00");
        when(orderService.listForAdmin(any(AdminOrderQuery.class))).thenReturn(List.of(order));

        String csv = service.exportOrdersCsv("admin-001", new AdminOrderQuery());

        assertTrue(csv.contains("张**"));
        assertTrue(csv.contains("李**"));
        verify(adminService).requirePermission("admin-001", "ORDER_VIEW");
        verify(auditService).record(eq("admin-001"), eq("ADMIN_ORDER_EXPORT"), eq("orders"),
                eq(null), eq(true), eq("count=1"));
    }

    @Test
    void restrictUserWritesBanAndUpdatesStatus() {
        User user = new User();
        user.setId("passenger-001");
        user.setRole("passenger");
        user.setCredit(95);
        user.setStatus("正常");
        when(userMapper.selectById("passenger-001")).thenReturn(user);
        when(banRecordMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        UserRestrictionRequest request = new UserRestrictionRequest();
        request.setType("LIMIT_POST");
        request.setReason("manual risk");

        UserRestrictionVO result = service.restrictUser("admin-001", "passenger-001", request);

        assertEquals("LIMIT_POST", result.getType());
        assertEquals("限制发单", user.getStatus());
        ArgumentCaptor<BanRecord> ban = ArgumentCaptor.forClass(BanRecord.class);
        verify(banRecordMapper).insert(ban.capture());
        assertEquals("manual risk", ban.getValue().getReason());
        verify(adminService).requirePermission("admin-001", "USER_RESTRICT");
        verify(auditService).record(eq("admin-001"), eq("USER_RESTRICT"), eq("passenger-001"),
                eq(null), eq(true), eq("LIMIT_POST"));
    }
}
