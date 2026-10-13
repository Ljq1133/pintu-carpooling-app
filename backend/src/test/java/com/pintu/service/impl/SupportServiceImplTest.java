package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.dto.SupportMessageRequest;
import com.pintu.dto.SupportSessionVO;
import com.pintu.entity.SupportMessage;
import com.pintu.entity.SupportSession;
import com.pintu.entity.User;
import com.pintu.mapper.SupportMessageMapper;
import com.pintu.mapper.SupportSessionMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.service.AdminService;
import com.pintu.service.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupportServiceImplTest {

    private SupportSessionMapper supportSessionMapper;
    private SupportMessageMapper supportMessageMapper;
    private UserMapper userMapper;
    private AdminService adminService;
    private SecurityAuditService auditService;
    private SupportServiceImpl service;

    @BeforeEach
    void setUp() {
        supportSessionMapper = mock(SupportSessionMapper.class);
        supportMessageMapper = mock(SupportMessageMapper.class);
        userMapper = mock(UserMapper.class);
        adminService = mock(AdminService.class);
        auditService = mock(SecurityAuditService.class);
        service = new SupportServiceImpl(
                supportSessionMapper,
                supportMessageMapper,
                userMapper,
                adminService,
                auditService
        );
    }

    @Test
    void adminSessionListIncludesMessagesWithMaskedUserName() {
        SupportSession session = session("support-1", "passenger-001", "OPEN");
        when(supportSessionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(session));
        when(supportMessageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(message("support-1", "USER")));
        User passenger = new User();
        passenger.setName("Alice Passenger");
        when(userMapper.selectById("passenger-001")).thenReturn(passenger);

        List<SupportSessionVO> result = service.listAdminSessions("admin-001", "");

        assertEquals(1, result.size());
        assertEquals("A**", result.get(0).getUserName());
        assertEquals(1, result.get(0).getMessages().size());
        verify(adminService).requirePermission("admin-001", "SUPPORT_MANAGE");
    }

    @Test
    void imageMessageRequiresAttachmentKey() {
        when(supportSessionMapper.selectById("support-1"))
                .thenReturn(session("support-1", "passenger-001", "OPEN"));
        SupportMessageRequest request = new SupportMessageRequest();
        request.setMessageType("image");
        request.setContent("missing image");

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.sendUserMessage("passenger-001", "support-1", request)
        );

        assertEquals(1002, error.getCode());
    }

    @Test
    void adminReplyClaimsUnassignedSessionAndWritesMessage() {
        SupportSession session = session("support-1", "passenger-001", "OPEN");
        when(supportSessionMapper.selectById("support-1")).thenReturn(session);
        when(supportMessageMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        SupportMessageRequest request = new SupportMessageRequest();
        request.setMessageType("text");
        request.setContent("handled");
        request.setClientMessageId("client-msg-1");

        service.sendAdminMessage("admin-001", "support-1", request);

        assertEquals("CLAIMED", session.getStatus());
        assertEquals("admin-001", session.getAssigneeId());
        ArgumentCaptor<SupportMessage> message = ArgumentCaptor.forClass(SupportMessage.class);
        verify(supportMessageMapper).insert(message.capture());
        assertEquals("ADMIN", message.getValue().getSenderRole());
        verify(auditService).record(eq("admin-001"), eq("SUPPORT_MESSAGE_SEND"), eq("support-1"),
                eq(null), eq(true), eq("text"));
    }

    private SupportSession session(String id, String userId, String status) {
        SupportSession session = new SupportSession();
        session.setId(id);
        session.setUserId(userId);
        session.setSubject("Help");
        session.setStatus(status);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        return session;
    }

    private SupportMessage message(String sessionId, String role) {
        SupportMessage message = new SupportMessage();
        message.setId("message-1");
        message.setSessionId(sessionId);
        message.setSenderId("passenger-001");
        message.setSenderRole(role);
        message.setMessageType("text");
        message.setContent("hello");
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}
