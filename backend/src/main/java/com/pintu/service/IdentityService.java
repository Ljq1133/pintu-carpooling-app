package com.pintu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.TimeFormatter;
import com.pintu.dto.DriverIdentityApplyRequest;
import com.pintu.dto.IdentityApplicationVO;
import com.pintu.entity.AuditRecord;
import com.pintu.entity.IdentityApplication;
import com.pintu.entity.IdentityAttachment;
import com.pintu.entity.User;
import com.pintu.entity.UserRole;
import com.pintu.enums.AuditStatus;
import com.pintu.mapper.AuditRecordMapper;
import com.pintu.mapper.IdentityApplicationMapper;
import com.pintu.mapper.IdentityAttachmentMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.UserRoleMapper;
import com.pintu.security.SensitiveDataCrypto;
import com.pintu.service.identity.IdentityVerificationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdentityService {

    private final IdentityApplicationMapper applicationMapper;
    private final IdentityAttachmentMapper attachmentMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserMapper userMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final IdentityVerificationProvider verificationProvider;
    private final SensitiveDataCrypto crypto;

    @Transactional(rollbackFor = Exception.class)
    public IdentityApplicationVO applyForDriver(String userId, DriverIdentityApplyRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(1404, "User does not exist");
        }
        UserRole driverRole = findRole(userId, "driver");
        if (driverRole != null && "ACTIVE".equals(driverRole.getStatus())) {
            throw new BusinessException(1020, "Driver role is already active");
        }
        IdentityApplication latest = latest(userId);
        if (latest != null && AuditStatus.PENDING.getValue().equals(latest.getStatus())) {
            throw new BusinessException(1021, "A driver identity application is already pending");
        }
        if (!verificationProvider.verify(
                request.getRealName(), request.getIdCard(), request.getLivenessResult())) {
            throw new BusinessException(1022, "Identity verification provider rejected the application");
        }

        IdentityApplication application = new IdentityApplication();
        application.setId(IdGenerator.next("identity"));
        application.setUserId(userId);
        application.setRealNameCipher(crypto.encrypt(request.getRealName()));
        application.setIdCardCipher(crypto.encrypt(request.getIdCard().toUpperCase()));
        application.setIdCardMasked(maskIdCard(request.getIdCard()));
        application.setLivenessResult(request.getLivenessResult().toUpperCase());
        application.setStatus(AuditStatus.PENDING.getValue());
        application.setSubmittedAt(TimeFormatter.nowDateTime());
        applicationMapper.insert(application);

        insertAttachment(application.getId(), "ID_CARD_FRONT", request.getIdCardFrontKey());
        insertAttachment(application.getId(), "ID_CARD_BACK", request.getIdCardBackKey());

        if (driverRole == null) {
            driverRole = new UserRole();
            driverRole.setId(IdGenerator.next("ur"));
            driverRole.setUserId(userId);
            driverRole.setRole("driver");
            driverRole.setStatus("PENDING");
            userRoleMapper.insert(driverRole);
        } else {
            driverRole.setStatus("PENDING");
            userRoleMapper.updateById(driverRole);
        }

        AuditRecord audit = new AuditRecord();
        audit.setId(IdGenerator.next("audit"));
        audit.setTargetType("user");
        audit.setTargetId(application.getId());
        audit.setName(user.getName());
        audit.setStatus(AuditStatus.PENDING.getValue());
        audit.setSubmittedAt(TimeFormatter.nowDateTime());
        auditRecordMapper.insert(audit);
        return toVO(application);
    }

    public IdentityApplicationVO status(String userId) {
        IdentityApplication application = latest(userId);
        if (application == null) {
            throw new BusinessException(1404, "Identity application does not exist");
        }
        return toVO(application);
    }

    private IdentityApplication latest(String userId) {
        return applicationMapper.selectOne(new LambdaQueryWrapper<IdentityApplication>()
                .eq(IdentityApplication::getUserId, userId)
                .orderByDesc(IdentityApplication::getSubmittedAt)
                .last("LIMIT 1"));
    }

    private UserRole findRole(String userId, String role) {
        return userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRole, role));
    }

    private void insertAttachment(String applicationId, String type, String storageKey) {
        IdentityAttachment attachment = new IdentityAttachment();
        attachment.setId(IdGenerator.next("ia"));
        attachment.setApplicationId(applicationId);
        attachment.setAttachmentType(type);
        attachment.setStorageKey(storageKey);
        attachmentMapper.insert(attachment);
    }

    private String maskIdCard(String idCard) {
        if (idCard.length() < 8) {
            return "****";
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private IdentityApplicationVO toVO(IdentityApplication application) {
        IdentityApplicationVO vo = new IdentityApplicationVO();
        vo.setId(application.getId());
        vo.setIdCardMasked(application.getIdCardMasked());
        vo.setLivenessResult(application.getLivenessResult());
        vo.setStatus(application.getStatus());
        vo.setRejectReason(application.getRejectReason());
        vo.setSubmittedAt(TimeFormatter.format(application.getSubmittedAt()));
        vo.setHandledAt(TimeFormatter.format(application.getHandledAt()));
        return vo;
    }
}

