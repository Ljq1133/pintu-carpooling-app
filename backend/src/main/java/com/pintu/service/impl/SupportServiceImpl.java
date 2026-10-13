package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.Masking;
import com.pintu.common.util.TimeFormatter;
import com.pintu.dto.SupportMessageRequest;
import com.pintu.dto.SupportMessageVO;
import com.pintu.dto.SupportSessionCreateRequest;
import com.pintu.dto.SupportSessionVO;
import com.pintu.entity.SupportMessage;
import com.pintu.entity.SupportSession;
import com.pintu.entity.User;
import com.pintu.enums.AdminPermission;
import com.pintu.mapper.SupportMessageMapper;
import com.pintu.mapper.SupportSessionMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.service.AdminService;
import com.pintu.service.SecurityAuditService;
import com.pintu.service.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportServiceImpl implements SupportService {

    private static final String OPEN = "OPEN";
    private static final String CLAIMED = "CLAIMED";
    private static final String CLOSED = "CLOSED";

    private final SupportSessionMapper supportSessionMapper;
    private final SupportMessageMapper supportMessageMapper;
    private final UserMapper userMapper;
    private final AdminService adminService;
    private final SecurityAuditService securityAuditService;

    @Override
    public List<SupportSessionVO> listUserSessions(String userId) {
        return supportSessionMapper.selectList(new QueryWrapper<SupportSession>()
                        .eq("user_id", userId)
                        .orderByDesc("updated_at"))
                .stream()
                .map(session -> toSessionVO(session, false))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupportSessionVO createUserSession(String userId, SupportSessionCreateRequest request) {
        requireUser(userId);
        LocalDateTime now = TimeFormatter.nowDateTime();
        SupportSession session = new SupportSession();
        session.setId(IdGenerator.next("support"));
        session.setUserId(userId);
        session.setSubject(request.getSubject().trim());
        session.setStatus(OPEN);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        supportSessionMapper.insert(session);
        if ((request.getFirstMessage() != null && !request.getFirstMessage().isBlank())
                || (request.getAttachmentKey() != null && !request.getAttachmentKey().isBlank())) {
            SupportMessageRequest messageRequest = new SupportMessageRequest();
            messageRequest.setMessageType(request.getAttachmentKey() == null || request.getAttachmentKey().isBlank()
                    ? "text" : "image");
            messageRequest.setContent(request.getFirstMessage());
            messageRequest.setAttachmentKey(request.getAttachmentKey());
            createMessage(session, userId, "USER", messageRequest);
        }
        securityAuditService.record(userId, "SUPPORT_SESSION_CREATE", session.getId(), null, true, session.getSubject());
        return toSessionVO(session, true);
    }

    @Override
    public List<SupportMessageVO> listUserMessages(String userId, String sessionId) {
        SupportSession session = requireUserSession(userId, sessionId, false);
        return listMessages(session.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupportMessageVO sendUserMessage(String userId, String sessionId, SupportMessageRequest request) {
        SupportSession session = requireUserSession(userId, sessionId, true);
        SupportMessageVO message = createMessage(session, userId, "USER", request);
        touch(session);
        return message;
    }

    @Override
    public List<SupportSessionVO> listAdminSessions(String adminId, String status) {
        requireSupportPermission(adminId);
        QueryWrapper<SupportSession> wrapper = new QueryWrapper<SupportSession>().orderByDesc("updated_at");
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status.trim());
        }
        return supportSessionMapper.selectList(wrapper)
                .stream()
                .map(session -> toSessionVO(session, true))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupportSessionVO claim(String adminId, String sessionId) {
        requireSupportPermission(adminId);
        SupportSession session = requireSession(sessionId);
        if (CLOSED.equals(session.getStatus())) {
            throw new BusinessException(1018, "Support session is closed");
        }
        session.setAssigneeId(adminId);
        session.setStatus(CLAIMED);
        touch(session);
        securityAuditService.record(adminId, "SUPPORT_SESSION_CLAIM", sessionId, null, true, null);
        return toSessionVO(session, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupportMessageVO sendAdminMessage(String adminId, String sessionId, SupportMessageRequest request) {
        requireSupportPermission(adminId);
        SupportSession session = requireAdminSession(adminId, sessionId, true);
        SupportMessageVO message = createMessage(session, adminId, "ADMIN", request);
        touch(session);
        securityAuditService.record(adminId, "SUPPORT_MESSAGE_SEND", sessionId, null, true, request.getMessageType());
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupportSessionVO close(String adminId, String sessionId) {
        requireSupportPermission(adminId);
        SupportSession session = requireAdminSession(adminId, sessionId, false);
        session.setStatus(CLOSED);
        session.setClosedAt(TimeFormatter.nowDateTime());
        touch(session);
        securityAuditService.record(adminId, "SUPPORT_SESSION_CLOSE", sessionId, null, true, null);
        return toSessionVO(session, true);
    }

    private void requireSupportPermission(String adminId) {
        adminService.requirePermission(adminId, AdminPermission.SUPPORT_MANAGE.getCode());
    }

    private User requireUser(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(1404, "User does not exist");
        }
        return user;
    }

    private SupportSession requireUserSession(String userId, String sessionId, boolean requireOpen) {
        SupportSession session = requireSession(sessionId);
        if (!userId.equals(session.getUserId())) {
            throw new BusinessException(1004, "Cannot access another user's support session");
        }
        if (requireOpen && CLOSED.equals(session.getStatus())) {
            throw new BusinessException(1018, "Support session is closed");
        }
        return session;
    }

    private SupportSession requireAdminSession(String adminId, String sessionId, boolean requireOpen) {
        SupportSession session = requireSession(sessionId);
        if (session.getAssigneeId() == null) {
            session.setAssigneeId(adminId);
            session.setStatus(CLAIMED);
            touch(session);
        }
        if (!adminId.equals(session.getAssigneeId())) {
            throw new BusinessException(1004, "Support session is assigned to another admin");
        }
        if (requireOpen && CLOSED.equals(session.getStatus())) {
            throw new BusinessException(1018, "Support session is closed");
        }
        return session;
    }

    private SupportSession requireSession(String sessionId) {
        SupportSession session = supportSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(1404, "Support session does not exist");
        }
        return session;
    }

    private SupportMessageVO createMessage(
            SupportSession session,
            String senderId,
            String senderRole,
            SupportMessageRequest request) {
        validateMessage(request);
        if (request.getClientMessageId() != null && !request.getClientMessageId().isBlank()) {
            SupportMessage existing = supportMessageMapper.selectOne(new QueryWrapper<SupportMessage>()
                    .eq("session_id", session.getId())
                    .eq("client_message_id", request.getClientMessageId())
                    .last("limit 1"));
            if (existing != null) {
                return toMessageVO(existing);
            }
        }
        SupportMessage message = new SupportMessage();
        message.setId(IdGenerator.next("supmsg"));
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setMessageType(request.getMessageType().trim().toLowerCase());
        message.setContent(normalize(request.getContent()));
        message.setAttachmentKey(normalize(request.getAttachmentKey()));
        message.setClientMessageId(normalize(request.getClientMessageId()));
        message.setCreatedAt(TimeFormatter.nowDateTime());
        supportMessageMapper.insert(message);
        return toMessageVO(message);
    }

    private void validateMessage(SupportMessageRequest request) {
        String type = request.getMessageType() == null ? "" : request.getMessageType().trim().toLowerCase();
        if (!List.of("text", "image").contains(type)) {
            throw new BusinessException(1002, "Support message only supports text or image");
        }
        if ("text".equals(type) && (request.getContent() == null || request.getContent().isBlank())) {
            throw new BusinessException(1002, "Text message cannot be blank");
        }
        if ("image".equals(type) && (request.getAttachmentKey() == null || request.getAttachmentKey().isBlank())) {
            throw new BusinessException(1002, "Image message requires attachmentKey");
        }
    }

    private List<SupportMessageVO> listMessages(String sessionId) {
        return supportMessageMapper.selectList(new QueryWrapper<SupportMessage>()
                        .eq("session_id", sessionId)
                        .orderByAsc("created_at"))
                .stream()
                .map(this::toMessageVO)
                .toList();
    }

    private void touch(SupportSession session) {
        session.setUpdatedAt(TimeFormatter.nowDateTime());
        supportSessionMapper.updateById(session);
    }

    private SupportSessionVO toSessionVO(SupportSession session, boolean includeMessages) {
        User user = userMapper.selectById(session.getUserId());
        SupportMessage latest = supportMessageMapper.selectOne(new QueryWrapper<SupportMessage>()
                .eq("session_id", session.getId())
                .orderByDesc("created_at")
                .last("limit 1"));
        SupportSessionVO vo = new SupportSessionVO();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setUserName(user == null ? null : Masking.name(user.getName()));
        vo.setAssigneeId(session.getAssigneeId());
        vo.setSubject(session.getSubject());
        vo.setStatus(session.getStatus());
        vo.setMessageCount(supportMessageMapper.selectCount(new QueryWrapper<SupportMessage>()
                .eq("session_id", session.getId())));
        vo.setLatestMessage(latest == null ? null : latest.getContent());
        vo.setCreatedAt(TimeFormatter.format(session.getCreatedAt()));
        vo.setUpdatedAt(TimeFormatter.format(session.getUpdatedAt()));
        vo.setClosedAt(TimeFormatter.format(session.getClosedAt()));
        if (includeMessages) {
            vo.setMessages(listMessages(session.getId()));
        }
        return vo;
    }

    private SupportMessageVO toMessageVO(SupportMessage message) {
        SupportMessageVO vo = new SupportMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setSenderId(message.getSenderId());
        vo.setSenderRole(message.getSenderRole());
        vo.setMessageType(message.getMessageType());
        vo.setContent(message.getContent());
        vo.setAttachmentKey(message.getAttachmentKey());
        vo.setClientMessageId(message.getClientMessageId());
        vo.setCreatedAt(TimeFormatter.format(message.getCreatedAt()));
        return vo;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
