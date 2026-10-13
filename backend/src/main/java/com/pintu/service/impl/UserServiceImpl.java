package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.Hashing;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.Masking;
import com.pintu.common.util.TimeFormatter;
import com.pintu.dto.AccountBindingRequest;
import com.pintu.dto.AccountBindingVO;
import com.pintu.dto.AccountPreferenceRequest;
import com.pintu.dto.AccountPreferenceVO;
import com.pintu.dto.ChangePasswordRequest;
import com.pintu.dto.ChangePhoneRequest;
import com.pintu.dto.LoginRequest;
import com.pintu.dto.LoginResponse;
import com.pintu.dto.RefreshTokenRequest;
import com.pintu.dto.RegisterRequest;
import com.pintu.dto.ResetPasswordRequest;
import com.pintu.dto.UserVO;
import com.pintu.dto.VerificationCodeLoginRequest;
import com.pintu.dto.ProfileUpdateRequest;
import com.pintu.dto.CreditRecordVO;
import com.pintu.entity.AccountBinding;
import com.pintu.entity.AccountPreference;
import com.pintu.entity.AuthRefreshToken;
import com.pintu.entity.CreditRecord;
import com.pintu.entity.LoginFailure;
import com.pintu.entity.User;
import com.pintu.entity.UserRole;
import com.pintu.enums.UserStatus;
import com.pintu.mapper.AuthRefreshTokenMapper;
import com.pintu.mapper.AccountBindingMapper;
import com.pintu.mapper.AccountPreferenceMapper;
import com.pintu.mapper.CreditRecordMapper;
import com.pintu.mapper.LoginFailureMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.UserRoleMapper;
import com.pintu.security.JwtService;
import com.pintu.security.SensitiveDataCrypto;
import com.pintu.service.UserService;
import com.pintu.service.VerificationCodeService;
import com.pintu.service.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final AuthRefreshTokenMapper refreshTokenMapper;
    private final LoginFailureMapper loginFailureMapper;
    private final CreditRecordMapper creditRecordMapper;
    private final AccountPreferenceMapper accountPreferenceMapper;
    private final AccountBindingMapper accountBindingMapper;
    private final VerificationCodeService verificationCodeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SensitiveDataCrypto sensitiveDataCrypto;
    private final SecurityAuditService securityAuditService;
    private final UserRoleMapper userRoleMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.jwt.refresh-ttl-days:30}")
    private long refreshTtlDays;
    @Value("${security.login.max-failures:5}")
    private int maxLoginFailures;
    @Value("${security.login.lock-minutes:15}")
    private long loginLockMinutes;

    @Override
    public LoginResponse login(LoginRequest request) {
        ensureNotTemporarilyLocked(request.getUsername());
        User user = findByUsername(request.getUsername());
        if (user == null && request.getUsername().matches("^1[3-9]\\d{9}$")) {
            user = findByPhone(request.getUsername());
        }
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordLoginFailure(request.getUsername(), request.getIpAddress(), "INVALID_CREDENTIALS");
            securityAuditService.record(null, "PASSWORD_LOGIN", request.getUsername(),
                    request.getIpAddress(), false, "INVALID_CREDENTIALS");
            throw new BusinessException(1003, "Invalid username or password");
        }
        ensureUsable(user);
        String activeRole = request.getRole() == null || request.getRole().isBlank()
                ? user.getRole() : request.getRole();
        if (!hasActiveRole(user.getId(), activeRole)) {
            recordLoginFailure(request.getUsername(), request.getIpAddress(), "ROLE_MISMATCH");
            securityAuditService.record(user.getId(), "PASSWORD_LOGIN", request.getUsername(),
                    request.getIpAddress(), false, "ROLE_MISMATCH");
            throw new BusinessException(1004, "Role does not match account");
        }
        clearLoginFailures(request.getUsername());
        securityAuditService.record(user.getId(), "PASSWORD_LOGIN", request.getUsername(),
                request.getIpAddress(), true, null);
        return issueTokens(user, activeRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        if (findByPhone(request.getPhone()) != null || findByUsername(request.getPhone()) != null) {
            throw new BusinessException(1008, "Phone number is already registered");
        }
        verificationCodeService.consume(request.getPhone(), "REGISTER", request.getCode());
        User user = new User();
        user.setId(IdGenerator.next("passenger"));
        user.setUsername("u_" + user.getId());
        user.setPhone(sensitiveDataCrypto.encrypt(request.getPhone()));
        user.setPhoneHash(sensitiveDataCrypto.blindIndex(request.getPhone()));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole("passenger");
        user.setCredit(80);
        user.setStatus(UserStatus.NORMAL.getValue());
        user.setTokenVersion(0);
        user.setTripCount(0);
        user.setSavedMoney(BigDecimal.ZERO);
        user.setCarbonSaved(BigDecimal.ZERO);
        userMapper.insert(user);
        UserRole passengerRole = new UserRole();
        passengerRole.setId(IdGenerator.next("ur"));
        passengerRole.setUserId(user.getId());
        passengerRole.setRole("passenger");
        passengerRole.setStatus("ACTIVE");
        userRoleMapper.insert(passengerRole);
        securityAuditService.record(user.getId(), "REGISTER", request.getPhone(), null, true, null);
        return issueTokens(user, "passenger");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse loginWithCode(VerificationCodeLoginRequest request) {
        User user = findByPhone(request.getPhone());
        if (user == null) {
            throw new BusinessException(1003, "Account does not exist");
        }
        ensureUsable(user);
        verificationCodeService.consume(request.getPhone(), "LOGIN", request.getCode());
        clearLoginFailures(user.getUsername());
        securityAuditService.record(user.getId(), "CODE_LOGIN", request.getPhone(), null, true, null);
        return issueTokens(user, user.getRole());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequest request) {
        User user = findByPhone(request.getPhone());
        if (user == null) {
            throw new BusinessException(1003, "Account does not exist");
        }
        verificationCodeService.consume(request.getPhone(), "RESET_PASSWORD", request.getCode());
        updatePasswordAndRevokeTokens(user, request.getNewPassword());
        securityAuditService.record(user.getId(), "PASSWORD_RESET", request.getPhone(), null, true, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(1003, "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(1015, "New password must differ from current password");
        }
        updatePasswordAndRevokeTokens(user, request.getNewPassword());
        securityAuditService.record(user.getId(), "PASSWORD_CHANGED", user.getUsername(), null, true, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePhone(String userId, ChangePhoneRequest request) {
        User user = requireUser(userId);
        User existing = findByPhone(request.getNewPhone());
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BusinessException(1008, "Phone number is already registered");
        }
        verificationCodeService.consume(request.getNewPhone(), "CHANGE_PHONE", request.getCode());
        user.setPhone(sensitiveDataCrypto.encrypt(request.getNewPhone()));
        user.setPhoneHash(sensitiveDataCrypto.blindIndex(request.getNewPhone()));
        bumpTokenVersion(user);
        userMapper.updateById(user);
        revokeAllRefreshTokens(userId);
        securityAuditService.record(user.getId(), "PHONE_CHANGED", request.getNewPhone(), null, true, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse switchRole(String userId, String role) {
        User user = requireUser(userId);
        if (!hasActiveRole(userId, role)) {
            throw new BusinessException(1004, "Requested role is not active");
        }
        securityAuditService.record(userId, "ROLE_SWITCH", role, null, true, null);
        return issueTokens(user, role);
    }

    @Override
    public UserVO getProfile(String userId) {
        return toVO(requireUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(String userId, ProfileUpdateRequest request) {
        User user = requireUser(userId);
        if (request.getName() != null) {
            user.setName(request.getName().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    public List<CreditRecordVO> listCreditRecords(String userId) {
        requireUser(userId);
        List<CreditRecord> records = creditRecordMapper.selectList(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, userId)
                .orderByDesc(CreditRecord::getCreatedAt));
        List<CreditRecordVO> result = new ArrayList<>();
        for (CreditRecord record : records) {
            if (!userId.equals(record.getUserId())) {
                continue;
            }
            result.add(toCreditRecordVO(record));
        }
        return result;
    }

    @Override
    public AccountPreferenceVO getPreferences(String userId) {
        requireUser(userId);
        AccountPreference preference = accountPreferenceMapper.selectById(userId);
        if (preference == null) {
            preference = defaultPreference(userId);
        }
        return toPreferenceVO(preference);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountPreferenceVO updatePreferences(String userId, AccountPreferenceRequest request) {
        requireUser(userId);
        AccountPreference preference = accountPreferenceMapper.selectById(userId);
        boolean isNew = preference == null;
        if (preference == null) {
            preference = defaultPreference(userId);
        }
        if (request.getDarkMode() != null) {
            preference.setDarkMode(Boolean.TRUE.equals(request.getDarkMode()) ? 1 : 0);
        }
        if (request.getFontScale() != null && !request.getFontScale().isBlank()) {
            preference.setFontScale(request.getFontScale());
        }
        if (request.getLocale() != null && !request.getLocale().isBlank()) {
            preference.setLocale(request.getLocale());
        }
        if (request.getProtocolVersion() != null && !request.getProtocolVersion().isBlank()) {
            preference.setProtocolVersion(request.getProtocolVersion().trim());
        }
        preference.setUpdatedAt(TimeFormatter.nowDateTime());
        if (isNew) {
            accountPreferenceMapper.insert(preference);
        } else {
            accountPreferenceMapper.updateById(preference);
        }
        securityAuditService.record(userId, "ACCOUNT_PREFERENCE_UPDATE", userId, null, true,
                "locale=" + preference.getLocale());
        return toPreferenceVO(preference);
    }

    @Override
    public List<AccountBindingVO> listBindings(String userId) {
        requireUser(userId);
        return accountBindingMapper.selectList(new LambdaQueryWrapper<AccountBinding>()
                        .eq(AccountBinding::getUserId, userId)
                        .eq(AccountBinding::getStatus, "ACTIVE")
                        .orderByDesc(AccountBinding::getCreatedAt))
                .stream()
                .map(this::toBindingVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountBindingVO createBinding(String userId, AccountBindingRequest request) {
        requireUser(userId);
        String normalized = normalizeBindingValue(request.getAccountValue());
        String hashed = Hashing.sha256(request.getBindingType() + ":" + normalized);
        AccountBinding existing = accountBindingMapper.selectOne(new LambdaQueryWrapper<AccountBinding>()
                .eq(AccountBinding::getUserId, userId)
                .eq(AccountBinding::getBindingType, request.getBindingType())
                .eq(AccountBinding::getAccountValue, hashed)
                .last("limit 1"));
        if (existing != null) {
            if (!"ACTIVE".equals(existing.getStatus())) {
                existing.setStatus("ACTIVE");
                existing.setUpdatedAt(TimeFormatter.nowDateTime());
                accountBindingMapper.updateById(existing);
            }
            return toBindingVO(existing);
        }

        LocalDateTime now = TimeFormatter.nowDateTime();
        AccountBinding binding = new AccountBinding();
        binding.setId(IdGenerator.next("bind"));
        binding.setUserId(userId);
        binding.setBindingType(request.getBindingType());
        binding.setProvider(normalizeNullable(request.getProvider()));
        binding.setAccountLabel(normalizeNullable(request.getAccountLabel()));
        binding.setAccountValue(hashed);
        binding.setMaskedValue(maskBindingValue(request.getBindingType(), normalized));
        binding.setStatus("ACTIVE");
        binding.setCreatedAt(now);
        binding.setUpdatedAt(now);
        accountBindingMapper.insert(binding);
        securityAuditService.record(userId, "ACCOUNT_BINDING_CREATE", userId, null, true, request.getBindingType());
        return toBindingVO(binding);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindAccount(String userId, String bindingId) {
        requireUser(userId);
        AccountBinding binding = accountBindingMapper.selectOne(new LambdaQueryWrapper<AccountBinding>()
                .eq(AccountBinding::getId, bindingId)
                .eq(AccountBinding::getUserId, userId)
                .last("limit 1"));
        if (binding == null) {
            throw new BusinessException(1404, "Account binding does not exist");
        }
        binding.setStatus("UNBOUND");
        binding.setUpdatedAt(TimeFormatter.nowDateTime());
        accountBindingMapper.updateById(binding);
        securityAuditService.record(userId, "ACCOUNT_BINDING_UNBOUND", bindingId, null, true, binding.getBindingType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse refresh(RefreshTokenRequest request) {
        AuthRefreshToken stored = findRefreshToken(request.getRefreshToken());
        LocalDateTime now = TimeFormatter.nowDateTime();
        if (stored == null || stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(now)) {
            throw new BusinessException(1001, "Refresh token is invalid or expired");
        }
        User user = userMapper.selectById(stored.getUserId());
        ensureUsable(user);
        stored.setRevokedAt(now);
        refreshTokenMapper.updateById(stored);
        securityAuditService.record(user.getId(), "TOKEN_REFRESH", user.getUsername(), null, true, null);
        String activeRole = stored.getActiveRole() == null ? user.getRole() : stored.getActiveRole();
        if (!hasActiveRole(user.getId(), activeRole)) {
            throw new BusinessException(1004, "Refresh token role is no longer active");
        }
        return issueTokens(user, activeRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(RefreshTokenRequest request) {
        AuthRefreshToken stored = findRefreshToken(request.getRefreshToken());
        if (stored != null && stored.getRevokedAt() == null) {
            stored.setRevokedAt(TimeFormatter.nowDateTime());
            refreshTokenMapper.updateById(stored);
            securityAuditService.record(stored.getUserId(), "LOGOUT", null, null, true, null);
        }
    }

    @Override
    public UserVO getPassengerProfile(String passengerId) {
        return loadAsVO(passengerId, "passenger");
    }

    @Override
    public UserVO getDriverProfile(String driverId) {
        return loadAsVO(driverId, "driver");
    }

    private void ensureNotTemporarilyLocked(String username) {
        LocalDateTime cutoff = TimeFormatter.nowDateTime().minusMinutes(loginLockMinutes);
        Long failures = loginFailureMapper.selectCount(new LambdaQueryWrapper<LoginFailure>()
                .eq(LoginFailure::getUsername, username)
                .ge(LoginFailure::getCreatedAt, cutoff));
        if (failures >= maxLoginFailures) {
            throw new BusinessException(1011, "Account is temporarily locked");
        }
    }

    private void recordLoginFailure(String username, String ipAddress, String reason) {
        LoginFailure failure = new LoginFailure();
        failure.setId(IdGenerator.next("lf"));
        failure.setUsername(username);
        failure.setIpAddress(ipAddress);
        failure.setReason(reason);
        failure.setCreatedAt(TimeFormatter.nowDateTime());
        loginFailureMapper.insert(failure);
    }

    private void clearLoginFailures(String username) {
        loginFailureMapper.delete(new LambdaQueryWrapper<LoginFailure>()
                .eq(LoginFailure::getUsername, username));
    }

    private void updatePasswordAndRevokeTokens(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
        bumpTokenVersion(user);
        userMapper.updateById(user);
        revokeAllRefreshTokens(user.getId());
        clearLoginFailures(user.getUsername());
    }

    private void bumpTokenVersion(User user) {
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
    }

    private void revokeAllRefreshTokens(String userId) {
        refreshTokenMapper.update(null, new LambdaUpdateWrapper<AuthRefreshToken>()
                .eq(AuthRefreshToken::getUserId, userId)
                .isNull(AuthRefreshToken::getRevokedAt)
                .set(AuthRefreshToken::getRevokedAt, TimeFormatter.nowDateTime()));
    }

    private LoginResponse issueTokens(User user, String activeRole) {
        String rawRefreshToken = randomToken();
        AuthRefreshToken stored = new AuthRefreshToken();
        stored.setId(IdGenerator.next("rt"));
        stored.setUserId(user.getId());
        stored.setTokenHash(Hashing.sha256(rawRefreshToken));
        stored.setExpiresAt(TimeFormatter.nowDateTime().plusDays(refreshTtlDays));
        stored.setActiveRole(activeRole);
        refreshTokenMapper.insert(stored);
        LoginResponse response = new LoginResponse();
        response.setToken(jwtService.createAccessToken(user, activeRole));
        response.setRefreshToken(rawRefreshToken);
        response.setExpiresIn(jwtService.expiresInSeconds());
        UserVO vo = toVO(user);
        vo.setRole(activeRole);
        response.setUser(vo);
        return response;
    }

    private AuthRefreshToken findRefreshToken(String rawToken) {
        return refreshTokenMapper.selectOne(new LambdaQueryWrapper<AuthRefreshToken>()
                .eq(AuthRefreshToken::getTokenHash, Hashing.sha256(rawToken)));
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private User findByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhoneHash, sensitiveDataCrypto.blindIndex(phone)));
    }

    private User requireUser(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(1404, "User does not exist");
        }
        return user;
    }

    private AccountPreference defaultPreference(String userId) {
        AccountPreference preference = new AccountPreference();
        preference.setUserId(userId);
        preference.setDarkMode(0);
        preference.setFontScale("standard");
        preference.setLocale("zh-CN");
        preference.setProtocolVersion("2026-06");
        preference.setUpdatedAt(TimeFormatter.nowDateTime());
        return preference;
    }

    private AccountPreferenceVO toPreferenceVO(AccountPreference preference) {
        AccountPreferenceVO vo = new AccountPreferenceVO();
        vo.setDarkMode(preference.getDarkMode() != null && preference.getDarkMode() == 1);
        vo.setFontScale(preference.getFontScale());
        vo.setLocale(preference.getLocale());
        vo.setProtocolVersion(preference.getProtocolVersion());
        vo.setUpdatedAt(TimeFormatter.format(preference.getUpdatedAt()));
        return vo;
    }

    private AccountBindingVO toBindingVO(AccountBinding binding) {
        AccountBindingVO vo = new AccountBindingVO();
        vo.setId(binding.getId());
        vo.setBindingType(binding.getBindingType());
        vo.setProvider(binding.getProvider());
        vo.setAccountLabel(binding.getAccountLabel());
        vo.setMaskedValue(binding.getMaskedValue());
        vo.setStatus(binding.getStatus());
        vo.setCreatedAt(TimeFormatter.format(binding.getCreatedAt()));
        vo.setUpdatedAt(TimeFormatter.format(binding.getUpdatedAt()));
        return vo;
    }

    private String normalizeBindingValue(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(1002, "Binding value cannot be blank");
        }
        return normalized;
    }

    private String maskBindingValue(String type, String value) {
        if ("PHONE".equals(type)) {
            return Masking.phone(value);
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, Math.min(2, value.length())) + "****" + value.substring(value.length() - 2);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean hasActiveRole(String userId, String role) {
        return userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRole, role)
                .eq(UserRole::getStatus, "ACTIVE")) > 0;
    }

    private void ensureUsable(User user) {
        if (user == null || UserStatus.BANNED.getValue().equals(user.getStatus())) {
            throw new BusinessException(1005, "Account is unavailable");
        }
    }

    private UserVO loadAsVO(String userId, String expectedRole) {
        User user = requireUser(userId);
        if (expectedRole != null && !hasActiveRole(userId, expectedRole)) {
            throw new BusinessException(1004, "User role does not match endpoint");
        }
        return toVO(user);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setPhone(sensitiveDataCrypto.decrypt(user.getPhone()));
        return vo;
    }

    private CreditRecordVO toCreditRecordVO(CreditRecord record) {
        CreditRecordVO vo = new CreditRecordVO();
        vo.setId(record.getId());
        vo.setDelta(record.getDelta());
        vo.setReason(record.getReason());
        vo.setOperatorId(record.getOperatorId());
        vo.setCreatedAt(TimeFormatter.format(record.getCreatedAt()));
        return vo;
    }
}
