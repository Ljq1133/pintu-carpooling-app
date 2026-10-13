package com.pintu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.Hashing;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.TimeFormatter;
import com.pintu.dto.VerificationCodeResponse;
import com.pintu.entity.VerificationCode;
import com.pintu.mapper.VerificationCodeMapper;
import com.pintu.service.sms.SmsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private final VerificationCodeMapper verificationCodeMapper;
    private final SmsProvider smsProvider;
    private final SecurityAuditService securityAuditService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.verification.ttl-minutes:5}")
    private long ttlMinutes;
    @Value("${security.verification.resend-seconds:60}")
    private long resendSeconds;
    @Value("${security.verification.max-attempts:5}")
    private int maxAttempts;
    @Value("${security.verification.expose-debug-code:false}")
    private boolean exposeDebugCode;

    @Transactional(rollbackFor = Exception.class)
    public VerificationCodeResponse send(String phone, String purpose) {
        LocalDateTime now = TimeFormatter.nowDateTime();
        String phoneHash = Hashing.sha256(phone);
        VerificationCode latest = latest(phoneHash, purpose);
        if (latest != null && latest.getCreatedAt() != null
                && latest.getCreatedAt().plusSeconds(resendSeconds).isAfter(now)) {
            long remaining = java.time.Duration.between(
                    now, latest.getCreatedAt().plusSeconds(resendSeconds)).toSeconds();
            throw new BusinessException(1012, "Verification code requested too frequently; retry in "
                    + Math.max(1, remaining) + " seconds");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        VerificationCode record = new VerificationCode();
        record.setId(IdGenerator.next("vc"));
        record.setPhoneHash(phoneHash);
        record.setPurpose(purpose);
        record.setCodeHash(Hashing.sha256(code));
        record.setExpiresAt(now.plusMinutes(ttlMinutes));
        record.setAttemptCount(0);
        record.setCreatedAt(now);
        verificationCodeMapper.insert(record);
        smsProvider.send(phone, code, purpose);
        securityAuditService.record(null, "VERIFICATION_CODE_SENT", phone, null, true, purpose);
        return new VerificationCodeResponse(ttlMinutes * 60, resendSeconds, exposeDebugCode ? code : null);
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = BusinessException.class
    )
    public void consume(String phone, String purpose, String code) {
        LocalDateTime now = TimeFormatter.nowDateTime();
        VerificationCode record = latest(Hashing.sha256(phone), purpose);
        if (record == null || record.getConsumedAt() != null || record.getExpiresAt().isBefore(now)) {
            throw new BusinessException(1013, "Verification code is invalid or expired");
        }
        if (record.getAttemptCount() >= maxAttempts) {
            throw new BusinessException(1014, "Too many verification attempts");
        }
        if (!Hashing.sha256(code).equals(record.getCodeHash())) {
            verificationCodeMapper.incrementAttempt(record.getId(), maxAttempts);
            throw new BusinessException(1013, "Verification code is invalid or expired");
        }
        if (verificationCodeMapper.consume(record.getId(), now, maxAttempts) != 1) {
            throw new BusinessException(1013, "Verification code was already consumed");
        }
    }

    private VerificationCode latest(String phoneHash, String purpose) {
        return verificationCodeMapper.selectOne(new LambdaQueryWrapper<VerificationCode>()
                .eq(VerificationCode::getPhoneHash, phoneHash)
                .eq(VerificationCode::getPurpose, purpose)
                .orderByDesc(VerificationCode::getCreatedAt)
                .last("LIMIT 1"));
    }
}
