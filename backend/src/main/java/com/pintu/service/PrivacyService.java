package com.pintu.service;

import com.pintu.dto.BlacklistCreateRequest;
import com.pintu.dto.BlacklistVO;

import java.util.List;

public interface PrivacyService {

    List<BlacklistVO> listBlockedUsers(String ownerId);

    BlacklistVO blockUser(String ownerId, BlacklistCreateRequest request);

    void unblockUser(String ownerId, String blockedUserId);
}
