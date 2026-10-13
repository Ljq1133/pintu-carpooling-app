package com.pintu.service;

import com.pintu.dto.UserAddressSaveRequest;
import com.pintu.entity.UserAddress;

import java.math.BigDecimal;
import java.util.List;

public interface AddressService {
    List<UserAddress> list(String userId, String type);

    UserAddress saveFavorite(String userId, UserAddressSaveRequest request);

    void recordHistory(String userId, String name, String address, BigDecimal lng, BigDecimal lat);

    void deleteFavorite(String userId, String addressId);
}
