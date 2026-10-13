package com.pintu.service;

import com.pintu.dto.AdminOrderQuery;
import com.pintu.dto.AdminOrderDetailVO;
import com.pintu.dto.AdminOpsStatisticsVO;
import com.pintu.dto.AdminSafetyOrderVO;
import com.pintu.dto.DataRetentionPolicyRequest;
import com.pintu.dto.DataRetentionPolicyVO;
import com.pintu.dto.LocationPointVO;
import com.pintu.dto.RetentionCleanupRequest;
import com.pintu.dto.RetentionCleanupVO;
import com.pintu.dto.RuleConfigRequest;
import com.pintu.dto.RuleConfigVO;
import com.pintu.dto.RuleRollbackRequest;
import com.pintu.dto.SafetyAlertVO;
import com.pintu.dto.UserRestrictionRequest;
import com.pintu.dto.UserRestrictionVO;

import java.util.List;

public interface AdminGovernanceService {

    List<AdminSafetyOrderVO> listSafetyOrders(String adminId);

    List<LocationPointVO> listSafetyTrajectory(String adminId, String orderId);

    List<SafetyAlertVO> listSafetyAlerts(String adminId);

    String exportOrdersCsv(String adminId, AdminOrderQuery query);

    AdminOrderDetailVO getOrderDetail(String adminId, String orderId);

    AdminOpsStatisticsVO getOpsStatistics(String adminId);

    List<RuleConfigVO> listRules(String adminId);

    RuleConfigVO createRule(String adminId, RuleConfigRequest request);

    RuleConfigVO updateRule(String adminId, String ruleId, RuleConfigRequest request);

    RuleConfigVO publishRule(String adminId, String ruleId);

    RuleConfigVO rollbackRule(String adminId, String ruleId, RuleRollbackRequest request);

    List<DataRetentionPolicyVO> listRetentionPolicies(String adminId);

    DataRetentionPolicyVO saveRetentionPolicy(String adminId, DataRetentionPolicyRequest request);

    RetentionCleanupVO cleanupRetention(String adminId, RetentionCleanupRequest request);

    UserRestrictionVO restrictUser(String adminId, String userId, UserRestrictionRequest request);

    void releaseRestriction(String adminId, String userId, String type);
}
