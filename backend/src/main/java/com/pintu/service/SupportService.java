package com.pintu.service;

import com.pintu.dto.SupportMessageRequest;
import com.pintu.dto.SupportMessageVO;
import com.pintu.dto.SupportSessionCreateRequest;
import com.pintu.dto.SupportSessionVO;

import java.util.List;

public interface SupportService {

    List<SupportSessionVO> listUserSessions(String userId);

    SupportSessionVO createUserSession(String userId, SupportSessionCreateRequest request);

    List<SupportMessageVO> listUserMessages(String userId, String sessionId);

    SupportMessageVO sendUserMessage(String userId, String sessionId, SupportMessageRequest request);

    List<SupportSessionVO> listAdminSessions(String adminId, String status);

    SupportSessionVO claim(String adminId, String sessionId);

    SupportMessageVO sendAdminMessage(String adminId, String sessionId, SupportMessageRequest request);

    SupportSessionVO close(String adminId, String sessionId);
}
