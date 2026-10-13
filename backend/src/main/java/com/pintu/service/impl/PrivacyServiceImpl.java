package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.Masking;
import com.pintu.common.util.TimeFormatter;
import com.pintu.dto.BlacklistCreateRequest;
import com.pintu.dto.BlacklistVO;
import com.pintu.entity.Blacklist;
import com.pintu.entity.User;
import com.pintu.mapper.BlacklistMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.service.PrivacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivacyServiceImpl implements PrivacyService {

    private static final String DEFAULT_REASON = "用户主动拉黑";

    private final BlacklistMapper blacklistMapper;
    private final UserMapper userMapper;

    @Override
    public List<BlacklistVO> listBlockedUsers(String ownerId) {
        return blacklistMapper.selectList(new LambdaQueryWrapper<Blacklist>()
                        .eq(Blacklist::getOwnerId, ownerId)
                        .orderByDesc(Blacklist::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlacklistVO blockUser(String ownerId, BlacklistCreateRequest request) {
        String blockedUserId = request.getBlockedUserId().trim();
        if (ownerId.equals(blockedUserId)) {
            throw new BusinessException(1002, "不能拉黑自己");
        }
        if (userMapper.selectById(blockedUserId) == null) {
            throw new BusinessException(1404, "目标用户不存在");
        }

        Blacklist existing = find(ownerId, blockedUserId);
        if (existing != null) {
            return toVO(existing);
        }

        Blacklist record = new Blacklist();
        record.setId(IdGenerator.next("blacklist"));
        record.setOwnerId(ownerId);
        record.setBlockedId(blockedUserId);
        record.setReason(normalizeReason(request.getReason()));
        record.setCreatedAt(LocalDateTime.now());
        blacklistMapper.insert(record);
        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unblockUser(String ownerId, String blockedUserId) {
        Blacklist existing = find(ownerId, blockedUserId);
        if (existing != null) {
            blacklistMapper.deleteById(existing.getId());
        }
    }

    private Blacklist find(String ownerId, String blockedUserId) {
        return blacklistMapper.selectOne(new LambdaQueryWrapper<Blacklist>()
                .eq(Blacklist::getOwnerId, ownerId)
                .eq(Blacklist::getBlockedId, blockedUserId)
                .last("limit 1"));
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? DEFAULT_REASON : reason.trim();
    }

    private BlacklistVO toVO(Blacklist record) {
        User blocked = userMapper.selectById(record.getBlockedId());
        BlacklistVO vo = new BlacklistVO();
        vo.setBlockedUserId(record.getBlockedId());
        vo.setBlockedName(Masking.name(blocked == null ? null : blocked.getName()));
        vo.setRole(blocked == null ? null : blocked.getRole());
        vo.setReason(record.getReason());
        vo.setCreatedAt(TimeFormatter.format(record.getCreatedAt()));
        return vo;
    }
}
