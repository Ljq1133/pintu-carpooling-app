package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.dto.BlacklistCreateRequest;
import com.pintu.dto.BlacklistVO;
import com.pintu.entity.Blacklist;
import com.pintu.entity.User;
import com.pintu.mapper.BlacklistMapper;
import com.pintu.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class PrivacyServiceImplTest {

    private BlacklistMapper blacklistMapper;
    private UserMapper userMapper;
    private PrivacyServiceImpl service;

    @BeforeEach
    void setUp() {
        blacklistMapper = mock(BlacklistMapper.class);
        userMapper = mock(UserMapper.class);
        service = new PrivacyServiceImpl(blacklistMapper, userMapper);
    }

    @Test
    void rejectsBlockingSelf() {
        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.blockUser("passenger-001", request("passenger-001")));

        assertEquals(1002, error.getCode());
        verify(blacklistMapper, never()).insert(any(Blacklist.class));
    }

    @Test
    void rejectsMissingTargetUser() {
        when(userMapper.selectById("missing-user")).thenReturn(null);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.blockUser("passenger-001", request("missing-user")));

        assertEquals(1404, error.getCode());
    }

    @Test
    void duplicateBlockReturnsExistingRecordWithoutInsert() {
        User driver = user("driver-001", "赵司机", "driver");
        Blacklist existing = record("blacklist-1", "passenger-001", "driver-001");
        when(userMapper.selectById("driver-001")).thenReturn(driver);
        when(blacklistMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        BlacklistVO result = service.blockUser("passenger-001", request("driver-001"));

        assertEquals("driver-001", result.getBlockedUserId());
        assertEquals("赵**", result.getBlockedName());
        verify(blacklistMapper, never()).insert(any(Blacklist.class));
    }

    @Test
    void createsBlockWithDefaultReason() {
        when(userMapper.selectById("driver-001")).thenReturn(user("driver-001", "赵司机", "driver"));
        when(blacklistMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        BlacklistVO result = service.blockUser("passenger-001", request("driver-001"));

        ArgumentCaptor<Blacklist> captor = ArgumentCaptor.forClass(Blacklist.class);
        verify(blacklistMapper).insert(captor.capture());
        assertEquals("passenger-001", captor.getValue().getOwnerId());
        assertEquals("driver-001", captor.getValue().getBlockedId());
        assertEquals("用户主动拉黑", captor.getValue().getReason());
        assertEquals("赵**", result.getBlockedName());
    }

    @Test
    void listReturnsOnlyMapperResultsAndMasksNames() {
        Blacklist currentUsersRecord = record("blacklist-1", "passenger-001", "driver-001");
        when(blacklistMapper.selectList(any(Wrapper.class))).thenReturn(List.of(currentUsersRecord));
        when(userMapper.selectById("driver-001")).thenReturn(user("driver-001", "赵司机", "driver"));

        List<BlacklistVO> result = service.listBlockedUsers("passenger-001");

        assertEquals(1, result.size());
        assertEquals("赵**", result.get(0).getBlockedName());
        assertFalse(result.get(0).getBlockedName().contains("司机"));
    }

    @Test
    void unblockIsIdempotentAndOnlyDeletesOwnersRecord() {
        when(blacklistMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        service.unblockUser("passenger-001", "driver-001");
        verify(blacklistMapper, never()).deleteById(any(String.class));

        Blacklist existing = record("blacklist-1", "passenger-001", "driver-001");
        when(blacklistMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        service.unblockUser("passenger-001", "driver-001");
        verify(blacklistMapper).deleteById("blacklist-1");
    }

    private BlacklistCreateRequest request(String blockedUserId) {
        BlacklistCreateRequest request = new BlacklistCreateRequest();
        request.setBlockedUserId(blockedUserId);
        return request;
    }

    private Blacklist record(String id, String ownerId, String blockedId) {
        Blacklist record = new Blacklist();
        record.setId(id);
        record.setOwnerId(ownerId);
        record.setBlockedId(blockedId);
        record.setReason("用户主动拉黑");
        record.setCreatedAt(LocalDateTime.of(2026, 6, 14, 20, 0));
        return record;
    }

    private User user(String id, String name, String role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setRole(role);
        return user;
    }
}
