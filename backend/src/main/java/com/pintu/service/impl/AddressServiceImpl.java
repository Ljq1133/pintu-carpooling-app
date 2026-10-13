package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.dto.UserAddressSaveRequest;
import com.pintu.entity.UserAddress;
import com.pintu.mapper.UserAddressMapper;
import com.pintu.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private static final String HISTORY = "HISTORY";
    private static final String FAVORITE = "FAVORITE";
    private final UserAddressMapper mapper;

    @Override
    public List<UserAddress> list(String userId, String type) {
        String normalized = normalizeType(type);
        return mapper.selectList(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getAddressType, normalized)
                .orderByDesc(UserAddress::getLastUsedAt)
                .last("limit 20"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddress saveFavorite(String userId, UserAddressSaveRequest request) {
        UserAddress existing = find(userId, FAVORITE, request.getLng(), request.getLat());
        UserAddress address = existing == null ? new UserAddress() : existing;
        if (existing == null) {
            address.setId(IdGenerator.next("addr"));
            address.setUserId(userId);
            address.setAddressType(FAVORITE);
        }
        address.setLabel(request.getLabel());
        address.setName(request.getName().trim());
        address.setAddress(request.getAddress().trim());
        address.setLng(request.getLng());
        address.setLat(request.getLat());
        address.setLastUsedAt(LocalDateTime.now());
        if (existing == null) mapper.insert(address);
        else mapper.updateById(address);
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordHistory(String userId, String name, String address,
                              BigDecimal lng, BigDecimal lat) {
        UserAddress existing = find(userId, HISTORY, lng, lat);
        UserAddress value = existing == null ? new UserAddress() : existing;
        if (existing == null) {
            value.setId(IdGenerator.next("addr"));
            value.setUserId(userId);
            value.setAddressType(HISTORY);
        }
        value.setName(name);
        value.setAddress(address);
        value.setLng(lng);
        value.setLat(lat);
        value.setLastUsedAt(LocalDateTime.now());
        if (existing == null) mapper.insert(value);
        else mapper.updateById(value);
    }

    @Override
    public void deleteFavorite(String userId, String addressId) {
        UserAddress address = mapper.selectById(addressId);
        if (address == null || !userId.equals(address.getUserId())
                || !FAVORITE.equals(address.getAddressType())) {
            throw new BusinessException(1404, "收藏地址不存在");
        }
        mapper.deleteById(addressId);
    }

    private UserAddress find(String userId, String type, BigDecimal lng, BigDecimal lat) {
        return mapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getAddressType, type)
                .eq(UserAddress::getLng, lng)
                .eq(UserAddress::getLat, lat)
                .last("limit 1"));
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) return HISTORY;
        String normalized = type.trim().toUpperCase();
        if (!HISTORY.equals(normalized) && !FAVORITE.equals(normalized)) {
            throw new BusinessException(1002, "地址类型只支持 HISTORY 或 FAVORITE");
        }
        return normalized;
    }
}
