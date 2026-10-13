package com.pintu.service.impl;

import com.pintu.dto.AccountBindingRequest;
import com.pintu.dto.AccountBindingVO;
import com.pintu.dto.AccountPreferenceRequest;
import com.pintu.dto.AccountPreferenceVO;
import com.pintu.dto.CreditRecordVO;
import com.pintu.entity.AccountBinding;
import com.pintu.entity.CreditRecord;
import com.pintu.entity.User;
import com.pintu.mapper.AuthRefreshTokenMapper;
import com.pintu.mapper.AccountBindingMapper;
import com.pintu.mapper.AccountPreferenceMapper;
import com.pintu.mapper.CreditRecordMapper;
import com.pintu.mapper.LoginFailureMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.UserRoleMapper;
import com.pintu.security.JwtService;
import com.pintu.security.SensitiveDataCrypto;
import com.pintu.service.SecurityAuditService;
import com.pintu.service.VerificationCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private UserMapper userMapper;
    private CreditRecordMapper creditRecordMapper;
    private AccountPreferenceMapper accountPreferenceMapper;
    private AccountBindingMapper accountBindingMapper;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        creditRecordMapper = mock(CreditRecordMapper.class);
        accountPreferenceMapper = mock(AccountPreferenceMapper.class);
        accountBindingMapper = mock(AccountBindingMapper.class);
        service = new UserServiceImpl(
                userMapper,
                mock(AuthRefreshTokenMapper.class),
                mock(LoginFailureMapper.class),
                creditRecordMapper,
                accountPreferenceMapper,
                accountBindingMapper,
                mock(VerificationCodeService.class),
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                mock(SensitiveDataCrypto.class),
                mock(SecurityAuditService.class),
                mock(UserRoleMapper.class)
        );
    }

    @Test
    void listCreditRecordsReturnsOnlyCurrentUsersRecords() {
        User user = new User();
        user.setId("passenger-001");
        when(userMapper.selectById("passenger-001")).thenReturn(user);
        when(creditRecordMapper.selectList(any())).thenReturn(List.of(
                record("cr-1", "passenger-001", 5),
                record("cr-2", "driver-001", -10)
        ));

        List<CreditRecordVO> result = service.listCreditRecords("passenger-001");

        assertEquals(1, result.size());
        assertEquals("cr-1", result.get(0).getId());
        assertEquals(5, result.get(0).getDelta());
    }

    @Test
    void preferencesReturnDefaultsAndCanBeUpdated() {
        User user = new User();
        user.setId("passenger-001");
        when(userMapper.selectById("passenger-001")).thenReturn(user);
        when(accountPreferenceMapper.selectById("passenger-001")).thenReturn(null);

        AccountPreferenceVO defaults = service.getPreferences("passenger-001");
        assertEquals("standard", defaults.getFontScale());
        assertEquals("zh-CN", defaults.getLocale());

        AccountPreferenceRequest request = new AccountPreferenceRequest();
        request.setDarkMode(true);
        request.setFontScale("large");
        request.setLocale("en-US");

        AccountPreferenceVO updated = service.updatePreferences("passenger-001", request);
        assertEquals("large", updated.getFontScale());
        assertEquals("en-US", updated.getLocale());
    }

    @Test
    void createBindingStoresHashAndOnlyReturnsMaskedValue() {
        User user = new User();
        user.setId("passenger-001");
        when(userMapper.selectById("passenger-001")).thenReturn(user);
        when(accountBindingMapper.selectOne(any())).thenReturn(null);

        AccountBindingRequest request = new AccountBindingRequest();
        request.setBindingType("PAYMENT");
        request.setProvider("mock-pay");
        request.setAccountValue("alice@example.com");

        AccountBindingVO result = service.createBinding("passenger-001", request);

        ArgumentCaptor<AccountBinding> captor = ArgumentCaptor.forClass(AccountBinding.class);
        verify(accountBindingMapper).insert(captor.capture());
        assertFalse(captor.getValue().getAccountValue().contains("alice@example.com"));
        assertFalse(result.getMaskedValue().contains("alice@example.com"));
        assertEquals("PAYMENT", result.getBindingType());
    }

    private CreditRecord record(String id, String userId, int delta) {
        CreditRecord record = new CreditRecord();
        record.setId(id);
        record.setUserId(userId);
        record.setDelta(delta);
        record.setReason("课程测试信用流水");
        record.setOperatorId("admin-001");
        record.setCreatedAt(LocalDateTime.of(2026, 6, 14, 20, 0));
        return record;
    }
}
