package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.dto.AppealCreateRequest;
import com.pintu.dto.AppealTicketVO;
import com.pintu.dto.AppealTransitionRequest;
import com.pintu.dto.ArbitrationRequest;
import com.pintu.entity.AppealEvidence;
import com.pintu.entity.AppealTicket;
import com.pintu.entity.AppealTransition;
import com.pintu.entity.CreditRecord;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.User;
import com.pintu.mapper.AppealEvidenceMapper;
import com.pintu.mapper.AppealTicketMapper;
import com.pintu.mapper.AppealTransitionMapper;
import com.pintu.mapper.ArbitrationRecordMapper;
import com.pintu.mapper.CreditRecordMapper;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.service.AdminService;
import com.pintu.service.SecurityAuditService;
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

class AppealServiceImplTest {

    private AppealTicketMapper appealTicketMapper;
    private AppealEvidenceMapper appealEvidenceMapper;
    private AppealTransitionMapper appealTransitionMapper;
    private ArbitrationRecordMapper arbitrationRecordMapper;
    private OrderRecordMapper orderRecordMapper;
    private UserMapper userMapper;
    private CreditRecordMapper creditRecordMapper;
    private AdminService adminService;
    private AppealServiceImpl service;

    @BeforeEach
    void setUp() {
        appealTicketMapper = mock(AppealTicketMapper.class);
        appealEvidenceMapper = mock(AppealEvidenceMapper.class);
        appealTransitionMapper = mock(AppealTransitionMapper.class);
        arbitrationRecordMapper = mock(ArbitrationRecordMapper.class);
        orderRecordMapper = mock(OrderRecordMapper.class);
        userMapper = mock(UserMapper.class);
        creditRecordMapper = mock(CreditRecordMapper.class);
        adminService = mock(AdminService.class);
        service = new AppealServiceImpl(
                appealTicketMapper,
                appealTransitionMapper,
                appealEvidenceMapper,
                arbitrationRecordMapper,
                orderRecordMapper,
                userMapper,
                creditRecordMapper,
                adminService,
                mock(SecurityAuditService.class)
        );
    }

    @Test
    void passengerCannotAppealAnotherPassengersOrder() {
        when(orderRecordMapper.selectById("order-1")).thenReturn(order("passenger-001"));

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.createPassengerAppeal("passenger-002", "order-1", createRequest())
        );

        assertEquals(1004, error.getCode());
    }

    @Test
    void createAppealPersistsTicketEvidenceAndInitialTransition() {
        OrderRecord order = order("passenger-001");
        when(orderRecordMapper.selectById("order-1")).thenReturn(order);
        when(appealEvidenceMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(appealTransitionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(arbitrationRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(userMapper.selectById("passenger-001")).thenReturn(user("passenger-001", "Alice"));

        AppealTicketVO result = service.createPassengerAppeal("passenger-001", "order-1", createRequest());

        assertEquals("order-1", result.getOrderId());
        assertEquals("OPEN", result.getStatus());
        verify(appealTicketMapper).insert(any(AppealTicket.class));
        verify(appealEvidenceMapper).insert(any(AppealEvidence.class));
        verify(appealTransitionMapper).insert(any(AppealTransition.class));
    }

    @Test
    void adminTransitionChecksPermissionAndCanClose() {
        AppealTicket ticket = ticket();
        when(appealTicketMapper.selectById("appeal-1")).thenReturn(ticket);
        when(orderRecordMapper.selectById("order-1")).thenReturn(order("passenger-001"));
        when(appealEvidenceMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(appealTransitionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(arbitrationRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        AppealTransitionRequest request = new AppealTransitionRequest();
        request.setStatus("CLOSED");
        request.setComment("resolved");

        AppealTicketVO result = service.transitionByAdmin("admin-001", "appeal-1", request);

        assertEquals("CLOSED", result.getStatus());
        assertEquals("resolved", ticket.getResolution());
        verify(adminService).requirePermission("admin-001", "APPEAL_MANAGE");
        verify(appealTicketMapper).updateById(ticket);
    }

    @Test
    void arbitrationRequiresSecondConfirmation() {
        ArbitrationRequest request = new ArbitrationRequest();
        request.setActionType("ORDER_NOTE");

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.arbitrate("admin-001", "appeal-1", request)
        );

        assertEquals(1002, error.getCode());
    }

    @Test
    void arbitrationCanCorrectCredit() {
        AppealTicket ticket = ticket();
        User applicant = user("passenger-001", "Alice");
        applicant.setCredit(80);
        when(appealTicketMapper.selectById("appeal-1")).thenReturn(ticket);
        when(orderRecordMapper.selectById("order-1")).thenReturn(order("passenger-001"));
        when(userMapper.selectById("passenger-001")).thenReturn(applicant);
        ArbitrationRequest request = new ArbitrationRequest();
        request.setActionType("CREDIT_CORRECTION");
        request.setCreditDelta(-10);
        request.setConfirmed(true);

        service.arbitrate("admin-001", "appeal-1", request);

        assertEquals(70, applicant.getCredit());
        verify(adminService).requirePermission("admin-001", "ARBITRATION_MANAGE");
        verify(creditRecordMapper).insert(any(CreditRecord.class));
    }

    private AppealCreateRequest createRequest() {
        AppealCreateRequest request = new AppealCreateRequest();
        request.setReasonType("PRICE_DISPUTE");
        request.setDescription("charged too much");
        request.setEvidenceKeys(List.of("appeal/order-1/photo.png"));
        return request;
    }

    private AppealTicket ticket() {
        AppealTicket ticket = new AppealTicket();
        ticket.setId("appeal-1");
        ticket.setOrderId("order-1");
        ticket.setApplicantId("passenger-001");
        ticket.setStatus("OPEN");
        ticket.setReasonType("PRICE_DISPUTE");
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticket;
    }

    private OrderRecord order(String passengerId) {
        OrderRecord order = new OrderRecord();
        order.setId("order-1");
        order.setPassengerId(passengerId);
        order.setDriverId("driver-001");
        order.setStartLocation("A");
        order.setEndLocation("B");
        return order;
    }

    private User user(String id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }
}
