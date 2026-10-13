package com.pintu.service;

import com.pintu.common.util.Hashing;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.TimeFormatter;
import com.pintu.entity.SecurityAuditEvent;
import com.pintu.mapper.SecurityAuditEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final SecurityAuditEventMapper mapper;

    @Transactional
    public void record(
            String userId,
            String eventType,
            String identity,
            String ipAddress,
            boolean success,
            String detail) {
        SecurityAuditEvent event = new SecurityAuditEvent();
        event.setId(IdGenerator.next("se"));
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setIdentity(identity == null ? null : Hashing.sha256(identity));
        event.setIpAddress(ipAddress);
        event.setSuccess(success);
        event.setDetail(detail);
        event.setCreatedAt(TimeFormatter.nowDateTime());
        mapper.insert(event);
    }
}
