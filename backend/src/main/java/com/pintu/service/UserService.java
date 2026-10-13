package com.pintu.service;

import com.pintu.dto.LoginRequest;
import com.pintu.dto.LoginResponse;
import com.pintu.dto.RefreshTokenRequest;
import com.pintu.dto.RegisterRequest;
import com.pintu.dto.UserVO;
import com.pintu.dto.ChangePasswordRequest;
import com.pintu.dto.ChangePhoneRequest;
import com.pintu.dto.ResetPasswordRequest;
import com.pintu.dto.VerificationCodeLoginRequest;
import com.pintu.dto.ProfileUpdateRequest;
import com.pintu.dto.CreditRecordVO;
import com.pintu.dto.AccountPreferenceRequest;
import com.pintu.dto.AccountPreferenceVO;
import com.pintu.dto.AccountBindingRequest;
import com.pintu.dto.AccountBindingVO;

import java.util.List;

/**
 * 用户与登录服务。
 */
public interface UserService {

    /**
     * 演示登录。MVP 阶段使用固定演示账号校验。
     */
    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    LoginResponse loginWithCode(VerificationCodeLoginRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(String userId, ChangePasswordRequest request);

    void changePhone(String userId, ChangePhoneRequest request);

    LoginResponse switchRole(String userId, String role);

    UserVO getProfile(String userId);

    UserVO updateProfile(String userId, ProfileUpdateRequest request);

    List<CreditRecordVO> listCreditRecords(String userId);

    AccountPreferenceVO getPreferences(String userId);

    AccountPreferenceVO updatePreferences(String userId, AccountPreferenceRequest request);

    List<AccountBindingVO> listBindings(String userId);

    AccountBindingVO createBinding(String userId, AccountBindingRequest request);

    void unbindAccount(String userId, String bindingId);

    LoginResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    /**
     * 获取乘客资料（包含统计指标）。
     */
    UserVO getPassengerProfile(String passengerId);

    /**
     * 获取司机资料。
     */
    UserVO getDriverProfile(String driverId);
}
