package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.Masking;
import com.pintu.common.util.TimeFormatter;
import com.pintu.dto.AdminOpsStatisticsVO;
import com.pintu.dto.AdminOrderDetailVO;
import com.pintu.dto.AdminOrderQuery;
import com.pintu.dto.AdminOrderVO;
import com.pintu.dto.AdminSafetyOrderVO;
import com.pintu.dto.DataRetentionPolicyRequest;
import com.pintu.dto.DataRetentionPolicyVO;
import com.pintu.dto.LocationPointVO;
import com.pintu.dto.RetentionCleanupRequest;
import com.pintu.dto.RetentionCleanupVO;
import com.pintu.dto.RuleConfigRequest;
import com.pintu.dto.RuleConfigVO;
import com.pintu.dto.RuleRollbackRequest;
import com.pintu.dto.RuleVersionVO;
import com.pintu.dto.SafetyAlertVO;
import com.pintu.dto.UserRestrictionRequest;
import com.pintu.dto.UserRestrictionVO;
import com.pintu.entity.AppealEvidence;
import com.pintu.entity.AppealTicket;
import com.pintu.entity.ArbitrationRecord;
import com.pintu.entity.BanRecord;
import com.pintu.entity.Conversation;
import com.pintu.entity.CreditRecord;
import com.pintu.entity.DataRetentionPolicy;
import com.pintu.entity.LocationReport;
import com.pintu.entity.NegotiationProposal;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.OrderStatusHistory;
import com.pintu.entity.Payment;
import com.pintu.entity.Review;
import com.pintu.entity.RuleConfig;
import com.pintu.entity.RuleRollback;
import com.pintu.entity.RuleVersion;
import com.pintu.entity.SafetyAlert;
import com.pintu.entity.TrajectoryPoint;
import com.pintu.entity.TripShareToken;
import com.pintu.entity.User;
import com.pintu.entity.VirtualPhoneBinding;
import com.pintu.enums.AdminPermission;
import com.pintu.enums.OrderStatus;
import com.pintu.mapper.AppealEvidenceMapper;
import com.pintu.mapper.AppealTicketMapper;
import com.pintu.mapper.ArbitrationRecordMapper;
import com.pintu.mapper.BanRecordMapper;
import com.pintu.mapper.ConversationMapper;
import com.pintu.mapper.CreditRecordMapper;
import com.pintu.mapper.DataRetentionPolicyMapper;
import com.pintu.mapper.LocationReportMapper;
import com.pintu.mapper.NegotiationProposalMapper;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.OrderStatusHistoryMapper;
import com.pintu.mapper.PaymentMapper;
import com.pintu.mapper.ReviewMapper;
import com.pintu.mapper.RuleConfigMapper;
import com.pintu.mapper.RuleRollbackMapper;
import com.pintu.mapper.RuleVersionMapper;
import com.pintu.mapper.SafetyAlertMapper;
import com.pintu.mapper.TrajectoryPointMapper;
import com.pintu.mapper.TripShareTokenMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.VirtualPhoneBindingMapper;
import com.pintu.service.AdminGovernanceService;
import com.pintu.service.AdminService;
import com.pintu.service.OrderService;
import com.pintu.service.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminGovernanceServiceImpl implements AdminGovernanceService {

    private static final String ALERT_PENDING = "\u5f85\u5904\u7406";
    private static final String NO_LOCATION_UPDATE = "NO_LOCATION_UPDATE";
    private static final long NO_LOCATION_THRESHOLD_SECONDS = 90;

    private final AdminService adminService;
    private final OrderService orderService;
    private final SecurityAuditService securityAuditService;
    private final OrderRecordMapper orderRecordMapper;
    private final UserMapper userMapper;
    private final LocationReportMapper locationReportMapper;
    private final TrajectoryPointMapper trajectoryPointMapper;
    private final VirtualPhoneBindingMapper virtualPhoneBindingMapper;
    private final TripShareTokenMapper tripShareTokenMapper;
    private final SafetyAlertMapper safetyAlertMapper;
    private final OrderStatusHistoryMapper orderStatusHistoryMapper;
    private final PaymentMapper paymentMapper;
    private final ReviewMapper reviewMapper;
    private final CreditRecordMapper creditRecordMapper;
    private final AppealTicketMapper appealTicketMapper;
    private final AppealEvidenceMapper appealEvidenceMapper;
    private final ArbitrationRecordMapper arbitrationRecordMapper;
    private final ConversationMapper conversationMapper;
    private final NegotiationProposalMapper negotiationProposalMapper;
    private final RuleConfigMapper ruleConfigMapper;
    private final RuleVersionMapper ruleVersionMapper;
    private final RuleRollbackMapper ruleRollbackMapper;
    private final DataRetentionPolicyMapper dataRetentionPolicyMapper;
    private final BanRecordMapper banRecordMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<AdminSafetyOrderVO> listSafetyOrders(String adminId) {
        require(adminId, AdminPermission.SAFETY_MONITOR);
        return orderRecordMapper.selectList(new QueryWrapper<OrderRecord>()
                        .eq("status", OrderStatus.ONGOING.getValue())
                        .orderByDesc("updated_at"))
                .stream()
                .map(this::toSafetyOrderVO)
                .toList();
    }

    @Override
    public List<LocationPointVO> listSafetyTrajectory(String adminId, String orderId) {
        require(adminId, AdminPermission.SAFETY_MONITOR);
        OrderRecord order = requireOrder(orderId);
        ensureNoLocationUpdateAlert(order, latestLocation(orderId));
        return trajectoryPointMapper.selectList(new QueryWrapper<TrajectoryPoint>()
                        .eq("order_id", orderId)
                        .orderByAsc("sequence_no"))
                .stream()
                .map(this::toLocationPointVO)
                .toList();
    }

    @Override
    public List<SafetyAlertVO> listSafetyAlerts(String adminId) {
        require(adminId, AdminPermission.SAFETY_MONITOR);
        return safetyAlertMapper.selectList(new QueryWrapper<SafetyAlert>()
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toAlertVO)
                .toList();
    }

    @Override
    public String exportOrdersCsv(String adminId, AdminOrderQuery query) {
        require(adminId, AdminPermission.ORDER_VIEW);
        List<AdminOrderVO> orders = orderService.listForAdmin(query);
        StringBuilder csv = new StringBuilder("\uFEFFid,route,passenger,driver,status,paymentStatus,price,risk,createdAt\n");
        for (AdminOrderVO order : orders) {
            csv.append(cell(order.getId())).append(',')
                    .append(cell(order.getRoute())).append(',')
                    .append(cell(order.getPassenger())).append(',')
                    .append(cell(order.getDriver())).append(',')
                    .append(cell(order.getStatus())).append(',')
                    .append(cell(order.getPaymentStatus())).append(',')
                    .append(cell(order.getPrice() == null ? "" : order.getPrice().toPlainString())).append(',')
                    .append(cell(order.getRisk())).append(',')
                    .append(cell(order.getCreatedAt()))
                    .append('\n');
        }
        securityAuditService.record(adminId, "ADMIN_ORDER_EXPORT", "orders", null, true, "count=" + orders.size());
        return csv.toString();
    }

    @Override
    public AdminOrderDetailVO getOrderDetail(String adminId, String orderId) {
        require(adminId, AdminPermission.ORDER_VIEW);
        return toOrderDetailVO(requireOrder(orderId));
    }

    @Override
    public AdminOpsStatisticsVO getOpsStatistics(String adminId) {
        require(adminId, AdminPermission.ORDER_VIEW);
        List<OrderRecord> orders = orderRecordMapper.selectList(new QueryWrapper<OrderRecord>()
                .orderByDesc("created_at"));
        long total = orders.size();
        long completed = orders.stream()
                .filter(order -> OrderStatus.COMPLETED.getValue().equals(order.getStatus())
                        || OrderStatus.ARCHIVED.getValue().equals(order.getStatus()))
                .count();
        long cancelled = orders.stream()
                .filter(order -> OrderStatus.CANCELLED.getValue().equals(order.getStatus()))
                .count();
        long active = orders.stream()
                .filter(order -> OrderStatus.CONFIRMED.getValue().equals(order.getStatus())
                        || OrderStatus.ONGOING.getValue().equals(order.getStatus())
                        || OrderStatus.PENDING_PAYMENT.getValue().equals(order.getStatus()))
                .count();
        long alerts = safetyAlertMapper.selectCount(new QueryWrapper<>());
        long openAppeals = appealTicketMapper.selectList(new QueryWrapper<AppealTicket>())
                .stream()
                .filter(ticket -> ticket.getClosedAt() == null)
                .count();
        AdminOpsStatisticsVO vo = new AdminOpsStatisticsVO();
        vo.setTotalOrders(total);
        vo.setCompletedOrders(completed);
        vo.setCancelledOrders(cancelled);
        vo.setActiveOrders(active);
        vo.setSafetyAlertCount(alerts);
        vo.setOpenAppealCount(openAppeals);
        vo.setConversionRate(percent(active + completed, total));
        vo.setCompletionRate(percent(completed, total));
        vo.setCancellationRate(percent(cancelled, total));
        vo.setAbnormalRate(percent(alerts, total));
        vo.setAverageMatchMinutes(averageMatchMinutes(orders));
        return vo;
    }

    @Override
    public List<RuleConfigVO> listRules(String adminId) {
        require(adminId, AdminPermission.RULE_MANAGE);
        return ruleConfigMapper.selectList(new QueryWrapper<RuleConfig>()
                        .orderByAsc("rule_type")
                        .orderByAsc("code"))
                .stream()
                .map(this::toRuleVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RuleConfigVO createRule(String adminId, RuleConfigRequest request) {
        require(adminId, AdminPermission.RULE_MANAGE);
        validateRule(request);
        RuleConfig existing = ruleConfigMapper.selectOne(new QueryWrapper<RuleConfig>()
                .eq("rule_type", request.getRuleType().trim())
                .eq("code", request.getCode().trim())
                .last("limit 1"));
        if (existing != null) {
            throw new BusinessException(1008, "Rule code already exists");
        }
        LocalDateTime now = TimeFormatter.nowDateTime();
        RuleConfig config = new RuleConfig();
        config.setId(IdGenerator.next("rule"));
        config.setRuleType(request.getRuleType().trim());
        config.setCode(request.getCode().trim());
        config.setName(request.getName().trim());
        config.setStatus("DRAFT");
        config.setGrayPercent(request.getGrayPercent() == null ? 100 : request.getGrayPercent());
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        ruleConfigMapper.insert(config);
        createRuleVersion(config, adminId, request.getContent(), 1);
        securityAuditService.record(adminId, "RULE_CREATE", config.getId(), null, true, config.getCode());
        return toRuleVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RuleConfigVO updateRule(String adminId, String ruleId, RuleConfigRequest request) {
        require(adminId, AdminPermission.RULE_MANAGE);
        validateRule(request);
        RuleConfig config = requireRule(ruleId);
        config.setRuleType(request.getRuleType().trim());
        config.setCode(request.getCode().trim());
        config.setName(request.getName().trim());
        config.setGrayPercent(request.getGrayPercent() == null ? config.getGrayPercent() : request.getGrayPercent());
        config.setStatus("DRAFT");
        config.setUpdatedAt(TimeFormatter.nowDateTime());
        ruleConfigMapper.updateById(config);
        int nextVersion = versions(ruleId).stream().map(RuleVersion::getVersion).max(Comparator.naturalOrder()).orElse(0) + 1;
        createRuleVersion(config, adminId, request.getContent(), nextVersion);
        securityAuditService.record(adminId, "RULE_UPDATE", config.getId(), null, true, "version=" + nextVersion);
        return toRuleVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RuleConfigVO publishRule(String adminId, String ruleId) {
        require(adminId, AdminPermission.RULE_MANAGE);
        RuleConfig config = requireRule(ruleId);
        RuleVersion latest = latestVersion(ruleId);
        if (latest == null) {
            throw new BusinessException(1404, "Rule version does not exist");
        }
        latest.setStatus("PUBLISHED");
        latest.setPublishedAt(TimeFormatter.nowDateTime());
        ruleVersionMapper.updateById(latest);
        config.setActiveVersion(latest.getVersion());
        config.setStatus("ACTIVE");
        config.setUpdatedAt(TimeFormatter.nowDateTime());
        ruleConfigMapper.updateById(config);
        securityAuditService.record(adminId, "RULE_PUBLISH", ruleId, null, true, "version=" + latest.getVersion());
        return toRuleVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RuleConfigVO rollbackRule(String adminId, String ruleId, RuleRollbackRequest request) {
        require(adminId, AdminPermission.RULE_MANAGE);
        RuleConfig config = requireRule(ruleId);
        RuleVersion target = ruleVersionMapper.selectOne(new QueryWrapper<RuleVersion>()
                .eq("rule_id", ruleId)
                .eq("version", request.getToVersion())
                .last("limit 1"));
        if (target == null) {
            throw new BusinessException(1404, "Rollback target version does not exist");
        }
        RuleRollback rollback = new RuleRollback();
        rollback.setId(IdGenerator.next("rollback"));
        rollback.setRuleId(ruleId);
        rollback.setFromVersion(config.getActiveVersion());
        rollback.setToVersion(request.getToVersion());
        rollback.setOperatorId(adminId);
        rollback.setReason(request.getReason());
        rollback.setCreatedAt(TimeFormatter.nowDateTime());
        ruleRollbackMapper.insert(rollback);
        target.setStatus("PUBLISHED");
        target.setPublishedAt(TimeFormatter.nowDateTime());
        ruleVersionMapper.updateById(target);
        config.setActiveVersion(request.getToVersion());
        config.setStatus("ACTIVE");
        config.setUpdatedAt(TimeFormatter.nowDateTime());
        ruleConfigMapper.updateById(config);
        securityAuditService.record(adminId, "RULE_ROLLBACK", ruleId, null, true,
                config.getActiveVersion() + "->" + request.getToVersion());
        return toRuleVO(config);
    }

    @Override
    public List<DataRetentionPolicyVO> listRetentionPolicies(String adminId) {
        require(adminId, AdminPermission.SAFETY_MONITOR);
        return dataRetentionPolicyMapper.selectList(new QueryWrapper<DataRetentionPolicy>()
                        .orderByAsc("data_type"))
                .stream()
                .map(this::toRetentionVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataRetentionPolicyVO saveRetentionPolicy(String adminId, DataRetentionPolicyRequest request) {
        require(adminId, AdminPermission.SAFETY_MONITOR);
        DataRetentionPolicy policy = dataRetentionPolicyMapper.selectOne(new QueryWrapper<DataRetentionPolicy>()
                .eq("data_type", request.getDataType().trim())
                .last("limit 1"));
        boolean isNew = policy == null;
        if (policy == null) {
            policy = new DataRetentionPolicy();
            policy.setId(IdGenerator.next("ret"));
            policy.setDataType(request.getDataType().trim());
        }
        policy.setRetentionDays(request.getRetentionDays());
        policy.setDeleteMode(request.getDeleteMode() == null ? "ARCHIVE_THEN_DELETE" : request.getDeleteMode());
        policy.setDescription(request.getDescription());
        policy.setUpdatedBy(adminId);
        policy.setUpdatedAt(TimeFormatter.nowDateTime());
        if (isNew) {
            dataRetentionPolicyMapper.insert(policy);
        } else {
            dataRetentionPolicyMapper.updateById(policy);
        }
        securityAuditService.record(adminId, "RETENTION_POLICY_SAVE", policy.getDataType(), null, true,
                "days=" + policy.getRetentionDays());
        return toRetentionVO(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetentionCleanupVO cleanupRetention(String adminId, RetentionCleanupRequest request) {
        require(adminId, AdminPermission.SAFETY_MONITOR);
        boolean dryRun = request == null || !Boolean.FALSE.equals(request.getDryRun());
        List<DataRetentionPolicy> policies = dataRetentionPolicyMapper.selectList(new QueryWrapper<DataRetentionPolicy>()
                .orderByAsc("data_type"));
        DataRetentionPolicy locationPolicy = policies.stream()
                .filter(policy -> "LOCATION".equalsIgnoreCase(policy.getDataType()))
                .findFirst()
                .orElse(null);
        int retentionDays = locationPolicy == null ? 30 : locationPolicy.getRetentionDays();
        LocalDateTime cutoff = TimeFormatter.nowDateTime().minusDays(retentionDays);
        Long locationReports = locationReportMapper.selectCount(new QueryWrapper<LocationReport>()
                .lt("reported_at", cutoff));
        Long trajectoryPoints = trajectoryPointMapper.selectCount(new QueryWrapper<TrajectoryPoint>()
                .lt("recorded_at", cutoff));
        if (!dryRun) {
            locationReports = (long) locationReportMapper.delete(new QueryWrapper<LocationReport>()
                    .lt("reported_at", cutoff));
            trajectoryPoints = (long) trajectoryPointMapper.delete(new QueryWrapper<TrajectoryPoint>()
                    .lt("recorded_at", cutoff));
        }
        RetentionCleanupVO vo = new RetentionCleanupVO();
        vo.setDryRun(dryRun);
        vo.setLocationCutoffAt(TimeFormatter.format(cutoff));
        vo.setLocationReports(locationReports);
        vo.setTrajectoryPoints(trajectoryPoints);
        vo.setPolicyCount(policies.size());
        vo.setPolicies(policies.stream().map(this::toRetentionVO).toList());
        vo.getNotes().add("LOCATION cleanup covers location_report and trajectory_point rows before the cutoff.");
        vo.getNotes().add("CHAT, ATTACHMENT and AUDIT policies are shown for course traceability; external object deletion is not connected.");
        securityAuditService.record(adminId, "RETENTION_CLEANUP", "LOCATION", null, true,
                "dryRun=" + dryRun + ", locationReports=" + locationReports + ", trajectoryPoints=" + trajectoryPoints);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRestrictionVO restrictUser(String adminId, String userId, UserRestrictionRequest request) {
        require(adminId, AdminPermission.USER_RESTRICT);
        User user = requireUser(userId);
        String type = request.getType();
        BanRecord existing = activeBan(userId, type);
        if (existing != null) {
            return toRestrictionVO(existing);
        }
        BanRecord record = new BanRecord();
        record.setId(IdGenerator.next("ban"));
        record.setUserId(userId);
        record.setBanType(type);
        record.setReason(request.getReason());
        record.setOperatorId(adminId);
        record.setStartsAt(TimeFormatter.nowDateTime());
        banRecordMapper.insert(record);
        applyUserRestrictionStatus(user, type);
        userMapper.updateById(user);
        securityAuditService.record(adminId, "USER_RESTRICT", userId, null, true, type);
        return toRestrictionVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseRestriction(String adminId, String userId, String type) {
        require(adminId, AdminPermission.USER_RESTRICT);
        User user = requireUser(userId);
        BanRecord record = activeBan(userId, type);
        if (record != null) {
            record.setReleasedAt(TimeFormatter.nowDateTime());
            banRecordMapper.updateById(record);
        }
        restoreUserRestrictionStatus(user);
        userMapper.updateById(user);
        securityAuditService.record(adminId, "USER_RESTRICTION_RELEASE", userId, null, true, type);
    }

    private void require(String adminId, AdminPermission permission) {
        adminService.requirePermission(adminId, permission.getCode());
    }

    private OrderRecord requireOrder(String orderId) {
        OrderRecord order = orderRecordMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(1404, "Order does not exist: " + orderId);
        }
        return order;
    }

    private User requireUser(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(1404, "User does not exist");
        }
        return user;
    }

    private RuleConfig requireRule(String ruleId) {
        RuleConfig config = ruleConfigMapper.selectById(ruleId);
        if (config == null) {
            throw new BusinessException(1404, "Rule does not exist");
        }
        return config;
    }

    private void validateRule(RuleConfigRequest request) {
        if (request.getRuleType() == null
                || (!"MATCHING".equals(request.getRuleType().trim())
                && !"PRICING".equals(request.getRuleType().trim())
                && !"CREDIT".equals(request.getRuleType().trim()))) {
            throw new BusinessException(1002, "Rule type must be MATCHING, PRICING or CREDIT");
        }
        validateRuleContent(request.getContent());
    }

    private void validateRuleContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(1002, "Rule content cannot be blank");
        }
        try {
            JsonNode node = objectMapper.readTree(content);
            if (!node.isObject()) {
                throw new BusinessException(1002, "Rule content must be a JSON object");
            }
            if (node.size() == 0) {
                throw new BusinessException(1002, "Rule content cannot be an empty JSON object");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(1002, "Rule content must be valid JSON");
        }
    }

    private RuleVersion createRuleVersion(RuleConfig config, String adminId, String content, int version) {
        RuleVersion ruleVersion = new RuleVersion();
        ruleVersion.setId(IdGenerator.next("rv"));
        ruleVersion.setRuleId(config.getId());
        ruleVersion.setVersion(version);
        ruleVersion.setContent(content.trim());
        ruleVersion.setStatus("DRAFT");
        ruleVersion.setCreatedBy(adminId);
        ruleVersion.setCreatedAt(TimeFormatter.nowDateTime());
        ruleVersionMapper.insert(ruleVersion);
        return ruleVersion;
    }

    private RuleVersion latestVersion(String ruleId) {
        return ruleVersionMapper.selectOne(new QueryWrapper<RuleVersion>()
                .eq("rule_id", ruleId)
                .orderByDesc("version")
                .last("limit 1"));
    }

    private List<RuleVersion> versions(String ruleId) {
        return ruleVersionMapper.selectList(new QueryWrapper<RuleVersion>()
                .eq("rule_id", ruleId)
                .orderByDesc("version"));
    }

    private BanRecord activeBan(String userId, String type) {
        return banRecordMapper.selectOne(new QueryWrapper<BanRecord>()
                .eq("user_id", userId)
                .eq("ban_type", type)
                .isNull("released_at")
                .last("limit 1"));
    }

    private List<BanRecord> activeBans(String userId) {
        return banRecordMapper.selectList(new QueryWrapper<BanRecord>()
                .eq("user_id", userId)
                .isNull("released_at"));
    }

    private void applyUserRestrictionStatus(User user, String type) {
        if ("FULL_BAN".equals(type)) {
            user.setStatus("已封禁");
        } else if ("LIMIT_COMMUNICATION".equals(type)) {
            user.setStatus("限制通信");
        } else if ("LIMIT_ACCEPT".equals(type)) {
            user.setStatus("限制接单");
        } else if ("LIMIT_POST".equals(type)) {
            user.setStatus("限制发单");
        }
    }

    private void restoreUserRestrictionStatus(User user) {
        List<String> types = activeBans(user.getId()).stream().map(BanRecord::getBanType).toList();
        if (types.contains("FULL_BAN")) {
            user.setStatus("已封禁");
        } else if (types.contains("LIMIT_COMMUNICATION")) {
            user.setStatus("限制通信");
        } else if (types.contains("LIMIT_ACCEPT")) {
            user.setStatus("限制接单");
        } else if (types.contains("LIMIT_POST")) {
            user.setStatus("限制发单");
        } else if (user.getCredit() != null && user.getCredit() < 60) {
            user.setStatus("driver".equals(user.getRole()) ? "限制接单" : "限制发单");
        } else if (user.getCredit() != null && user.getCredit() < 80) {
            user.setStatus("观察");
        } else {
            user.setStatus("正常");
        }
    }

    private AdminOrderDetailVO toOrderDetailVO(OrderRecord order) {
        User passenger = userMapper.selectById(order.getPassengerId());
        User driver = userMapper.selectById(order.getDriverId());
        AdminOrderDetailVO vo = new AdminOrderDetailVO();
        vo.setId(order.getId());
        vo.setPassengerOrderId(order.getPassengerOrderId());
        vo.setDriverTripId(order.getDriverTripId());
        vo.setMatchRequestId(order.getMatchRequestId());
        vo.setPassengerId(order.getPassengerId());
        vo.setDriverId(order.getDriverId());
        vo.setPassenger(passenger == null ? null : Masking.name(passenger.getName()));
        vo.setDriver(driver == null ? null : Masking.name(driver.getName()));
        vo.setRoute(order.getStartLocation() + " -> " + order.getEndLocation());
        vo.setStartLocation(order.getStartLocation());
        vo.setEndLocation(order.getEndLocation());
        vo.setStartLng(order.getStartLng());
        vo.setStartLat(order.getStartLat());
        vo.setEndLng(order.getEndLng());
        vo.setEndLat(order.getEndLat());
        vo.setDepartureTime(TimeFormatter.format(order.getDepartureTime()));
        vo.setActualDeparture(TimeFormatter.format(order.getActualDeparture()));
        vo.setActualArrival(TimeFormatter.format(order.getActualArrival()));
        vo.setPassengerCount(order.getPassengerCount());
        vo.setPrice(order.getPrice());
        vo.setStatus(order.getStatus());
        vo.setPaymentStatus(order.getPaymentStatus());
        vo.setReviewStatus(order.getReviewStatus());
        vo.setCancelReason(order.getCancelReason());
        vo.setAdminNote(order.getAdminNote());
        vo.setCreatedAt(TimeFormatter.format(order.getCreatedAt()));
        vo.setUpdatedAt(TimeFormatter.format(order.getUpdatedAt()));
        LocationReport latest = latestLocation(order.getId());
        vo.setLatestLocation(latest == null ? null : toLocationPointVO(latest, null));
        vo.setTrajectory(trajectoryPointMapper.selectList(new QueryWrapper<TrajectoryPoint>()
                        .eq("order_id", order.getId())
                        .orderByAsc("sequence_no"))
                .stream()
                .map(this::toLocationPointVO)
                .toList());
        vo.setAlerts(safetyAlertMapper.selectList(new QueryWrapper<SafetyAlert>()
                        .eq("order_id", order.getId())
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toAlertVO)
                .toList());
        vo.setTimeline(orderStatusHistoryMapper.selectList(new QueryWrapper<OrderStatusHistory>()
                        .eq("order_id", order.getId())
                        .orderByAsc("created_at"))
                .stream()
                .map(this::toTimelineEvent)
                .toList());
        vo.setPayments(paymentMapper.selectList(new QueryWrapper<Payment>()
                        .eq("order_id", order.getId())
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toPaymentSnapshot)
                .toList());
        vo.setReviews(reviewMapper.selectList(new QueryWrapper<Review>()
                        .eq("order_id", order.getId())
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toReviewSnapshot)
                .toList());
        List<String> participantIds = participantIds(order);
        if (!participantIds.isEmpty()) {
            vo.setCreditEvents(creditRecordMapper.selectList(new QueryWrapper<CreditRecord>()
                            .in("user_id", participantIds)
                            .orderByDesc("created_at"))
                    .stream()
                    .map(this::toCreditSnapshot)
                    .toList());
        }
        vo.setAppeals(appealTicketMapper.selectList(new QueryWrapper<AppealTicket>()
                        .eq("order_id", order.getId())
                        .orderByDesc("updated_at"))
                .stream()
                .map(this::toAppealSnapshot)
                .toList());
        vo.setNegotiations(loadNegotiations(order).stream()
                .map(this::toNegotiationSnapshot)
                .toList());
        return vo;
    }

    private List<String> participantIds(OrderRecord order) {
        Set<String> ids = new LinkedHashSet<>();
        if (order.getPassengerId() != null) {
            ids.add(order.getPassengerId());
        }
        if (order.getDriverId() != null) {
            ids.add(order.getDriverId());
        }
        return new ArrayList<>(ids);
    }

    private List<NegotiationProposal> loadNegotiations(OrderRecord order) {
        List<Conversation> conversations = conversationMapper.selectList(new QueryWrapper<Conversation>()
                .and(wrapper -> wrapper.eq("order_id", order.getId())
                        .or().eq("match_request_id", order.getMatchRequestId())));
        List<String> conversationIds = conversations.stream().map(Conversation::getId).toList();
        if (conversationIds.isEmpty()) {
            return List.of();
        }
        return negotiationProposalMapper.selectList(new QueryWrapper<NegotiationProposal>()
                .in("conversation_id", conversationIds)
                .orderByAsc("created_at"));
    }

    private AdminOrderDetailVO.TimelineEvent toTimelineEvent(OrderStatusHistory history) {
        AdminOrderDetailVO.TimelineEvent event = new AdminOrderDetailVO.TimelineEvent();
        event.setId(history.getId());
        event.setOperatorId(history.getOperatorId());
        event.setOperatorRole(history.getOperatorRole());
        event.setFromStatus(history.getFromStatus());
        event.setToStatus(history.getToStatus());
        event.setReason(history.getReason());
        event.setCreatedAt(TimeFormatter.format(history.getCreatedAt()));
        return event;
    }

    private AdminOrderDetailVO.PaymentSnapshot toPaymentSnapshot(Payment payment) {
        AdminOrderDetailVO.PaymentSnapshot snapshot = new AdminOrderDetailVO.PaymentSnapshot();
        snapshot.setId(payment.getId());
        snapshot.setAmount(payment.getAmount());
        snapshot.setMethod(payment.getMethod());
        snapshot.setStatus(payment.getStatus());
        snapshot.setPaidAt(TimeFormatter.format(payment.getPaidAt()));
        snapshot.setCreatedAt(TimeFormatter.format(payment.getCreatedAt()));
        return snapshot;
    }

    private AdminOrderDetailVO.ReviewSnapshot toReviewSnapshot(Review review) {
        AdminOrderDetailVO.ReviewSnapshot snapshot = new AdminOrderDetailVO.ReviewSnapshot();
        snapshot.setId(review.getId());
        snapshot.setPassengerId(review.getPassengerId());
        snapshot.setDriverId(review.getDriverId());
        snapshot.setRating(review.getRating());
        snapshot.setComment(review.getComment());
        snapshot.setCreatedAt(TimeFormatter.format(review.getCreatedAt()));
        return snapshot;
    }

    private AdminOrderDetailVO.CreditSnapshot toCreditSnapshot(CreditRecord record) {
        AdminOrderDetailVO.CreditSnapshot snapshot = new AdminOrderDetailVO.CreditSnapshot();
        snapshot.setId(record.getId());
        snapshot.setUserId(record.getUserId());
        snapshot.setDelta(record.getDelta());
        snapshot.setReason(record.getReason());
        snapshot.setOperatorId(record.getOperatorId());
        snapshot.setCreatedAt(TimeFormatter.format(record.getCreatedAt()));
        return snapshot;
    }

    private AdminOrderDetailVO.AppealSnapshot toAppealSnapshot(AppealTicket ticket) {
        AdminOrderDetailVO.AppealSnapshot snapshot = new AdminOrderDetailVO.AppealSnapshot();
        snapshot.setId(ticket.getId());
        snapshot.setApplicantId(ticket.getApplicantId());
        snapshot.setAssigneeId(ticket.getAssigneeId());
        snapshot.setReasonType(ticket.getReasonType());
        snapshot.setDescription(ticket.getDescription());
        snapshot.setStatus(ticket.getStatus());
        snapshot.setResolution(ticket.getResolution());
        snapshot.setCreatedAt(TimeFormatter.format(ticket.getCreatedAt()));
        snapshot.setUpdatedAt(TimeFormatter.format(ticket.getUpdatedAt()));
        snapshot.setClosedAt(TimeFormatter.format(ticket.getClosedAt()));
        snapshot.setEvidence(appealEvidenceMapper.selectList(new QueryWrapper<AppealEvidence>()
                        .eq("appeal_id", ticket.getId())
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toEvidenceSnapshot)
                .toList());
        snapshot.setArbitrations(arbitrationRecordMapper.selectList(new QueryWrapper<ArbitrationRecord>()
                        .eq("appeal_id", ticket.getId())
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toArbitrationSnapshot)
                .toList());
        return snapshot;
    }

    private AdminOrderDetailVO.EvidenceSnapshot toEvidenceSnapshot(AppealEvidence evidence) {
        AdminOrderDetailVO.EvidenceSnapshot snapshot = new AdminOrderDetailVO.EvidenceSnapshot();
        snapshot.setId(evidence.getId());
        snapshot.setUploaderId(evidence.getUploaderId());
        snapshot.setStorageKey(evidence.getStorageKey());
        snapshot.setContentType(evidence.getContentType());
        snapshot.setCreatedAt(TimeFormatter.format(evidence.getCreatedAt()));
        return snapshot;
    }

    private AdminOrderDetailVO.ArbitrationSnapshot toArbitrationSnapshot(ArbitrationRecord record) {
        AdminOrderDetailVO.ArbitrationSnapshot snapshot = new AdminOrderDetailVO.ArbitrationSnapshot();
        snapshot.setId(record.getId());
        snapshot.setAppealId(record.getAppealId());
        snapshot.setAdminId(record.getAdminId());
        snapshot.setActionType(record.getActionType());
        snapshot.setAmount(record.getAmount());
        snapshot.setCreditDelta(record.getCreditDelta());
        snapshot.setRemark(record.getRemark());
        snapshot.setConfirmedBy(record.getConfirmedBy());
        snapshot.setCreatedAt(TimeFormatter.format(record.getCreatedAt()));
        return snapshot;
    }

    private AdminOrderDetailVO.NegotiationSnapshot toNegotiationSnapshot(NegotiationProposal proposal) {
        AdminOrderDetailVO.NegotiationSnapshot snapshot = new AdminOrderDetailVO.NegotiationSnapshot();
        snapshot.setId(proposal.getId());
        snapshot.setConversationId(proposal.getConversationId());
        snapshot.setVersion(proposal.getVersion());
        snapshot.setProposerId(proposal.getProposerId());
        snapshot.setPrice(proposal.getPrice());
        snapshot.setDepartureTime(TimeFormatter.format(proposal.getDepartureTime()));
        snapshot.setPassengerCount(proposal.getPassengerCount());
        snapshot.setPickupLocation(proposal.getPickupLocation());
        snapshot.setNote(proposal.getNote());
        snapshot.setCreatedAt(TimeFormatter.format(proposal.getCreatedAt()));
        return snapshot;
    }

    private BigDecimal percent(long value, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageMatchMinutes(List<OrderRecord> orders) {
        List<Long> minutes = orders.stream()
                .filter(order -> order.getCreatedAt() != null && order.getActualDeparture() != null)
                .map(order -> Duration.between(order.getCreatedAt(), order.getActualDeparture()).toMinutes())
                .filter(value -> value >= 0)
                .toList();
        if (minutes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long sum = minutes.stream().mapToLong(Long::longValue).sum();
        return BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(minutes.size()), 2, RoundingMode.HALF_UP);
    }

    private AdminSafetyOrderVO toSafetyOrderVO(OrderRecord order) {
        User passenger = userMapper.selectById(order.getPassengerId());
        User driver = userMapper.selectById(order.getDriverId());
        LocationReport latest = latestLocation(order.getId());
        boolean noUpdate = ensureNoLocationUpdateAlert(order, latest);
        AdminSafetyOrderVO vo = new AdminSafetyOrderVO();
        vo.setOrderId(order.getId());
        vo.setRoute(order.getStartLocation() + " -> " + order.getEndLocation());
        vo.setPassenger(passenger == null ? null : Masking.name(passenger.getName()));
        vo.setDriver(driver == null ? null : Masking.name(driver.getName()));
        vo.setStatus(order.getStatus());
        vo.setLatestLocation(latest == null ? null : toLocationPointVO(latest, null));
        vo.setLatestLocationAt(latest == null ? null : TimeFormatter.format(latest.getReportedAt()));
        vo.setNoLocationUpdate(noUpdate);
        vo.setSafetyStatus(noUpdate ? NO_LOCATION_UPDATE : "OK");
        vo.setVirtualPhoneStatus(hasActiveVirtualPhone(order.getId()) ? "ACTIVE" : "UNBOUND");
        vo.setShareStatus(hasActiveShareToken(order.getId()) ? "ACTIVE" : "INACTIVE");
        vo.setTrajectoryCount(trajectoryPointMapper.selectCount(new QueryWrapper<TrajectoryPoint>()
                .eq("order_id", order.getId())));
        vo.setAlertCount(safetyAlertMapper.selectCount(new QueryWrapper<SafetyAlert>()
                .eq("order_id", order.getId())));
        return vo;
    }

    private boolean hasActiveVirtualPhone(String orderId) {
        return virtualPhoneBindingMapper.selectCount(new QueryWrapper<VirtualPhoneBinding>()
                .eq("order_id", orderId)
                .eq("status", "ACTIVE")) > 0;
    }

    private boolean hasActiveShareToken(String orderId) {
        return tripShareTokenMapper.selectCount(new QueryWrapper<TripShareToken>()
                .eq("order_id", orderId)
                .isNull("revoked_at")
                .gt("expires_at", TimeFormatter.nowDateTime())) > 0;
    }

    private LocationReport latestLocation(String orderId) {
        return locationReportMapper.selectOne(new QueryWrapper<LocationReport>()
                .eq("order_id", orderId)
                .orderByDesc("reported_at")
                .last("limit 1"));
    }

    private boolean ensureNoLocationUpdateAlert(OrderRecord order, LocationReport latest) {
        if (!OrderStatus.ONGOING.getValue().equals(order.getStatus())) {
            return false;
        }
        LocalDateTime now = TimeFormatter.nowDateTime();
        boolean noUpdate = latest == null || latest.getReportedAt() == null
                || latest.getReportedAt().isBefore(now.minusSeconds(NO_LOCATION_THRESHOLD_SECONDS));
        if (!noUpdate) {
            return false;
        }
        Long existing = safetyAlertMapper.selectCount(new QueryWrapper<SafetyAlert>()
                .eq("order_id", order.getId())
                .eq("alert_type", NO_LOCATION_UPDATE)
                .eq("status", ALERT_PENDING));
        if (existing != null && existing > 0) {
            return true;
        }
        SafetyAlert alert = new SafetyAlert();
        alert.setId(IdGenerator.next("alert"));
        alert.setOrderId(order.getId());
        alert.setUserId(order.getDriverId() == null ? order.getPassengerId() : order.getDriverId());
        alert.setAlertType(NO_LOCATION_UPDATE);
        if (latest != null) {
            alert.setLongitude(latest.getLongitude());
            alert.setLatitude(latest.getLatitude());
        }
        alert.setDetails("No driver location update for more than " + NO_LOCATION_THRESHOLD_SECONDS + " seconds");
        alert.setStatus(ALERT_PENDING);
        alert.setCreatedAt(now);
        safetyAlertMapper.insert(alert);
        return true;
    }

    private LocationPointVO toLocationPointVO(LocationReport report, Integer sequenceNo) {
        LocationPointVO vo = new LocationPointVO();
        vo.setId(report.getId());
        vo.setOrderId(report.getOrderId());
        vo.setLongitude(report.getLongitude());
        vo.setLatitude(report.getLatitude());
        vo.setAccuracy(report.getAccuracy());
        vo.setSpeed(report.getSpeed());
        vo.setHeading(report.getHeading());
        vo.setSequenceNo(sequenceNo);
        vo.setReportedAt(TimeFormatter.format(report.getReportedAt()));
        return vo;
    }

    private LocationPointVO toLocationPointVO(TrajectoryPoint point) {
        LocationPointVO vo = new LocationPointVO();
        vo.setId(point.getId());
        vo.setOrderId(point.getOrderId());
        vo.setLongitude(point.getLongitude());
        vo.setLatitude(point.getLatitude());
        vo.setSequenceNo(point.getSequenceNo());
        vo.setReportedAt(TimeFormatter.format(point.getRecordedAt()));
        return vo;
    }

    private SafetyAlertVO toAlertVO(SafetyAlert alert) {
        SafetyAlertVO vo = new SafetyAlertVO();
        vo.setId(alert.getId());
        vo.setOrderId(alert.getOrderId());
        vo.setAlertType(alert.getAlertType());
        vo.setLongitude(alert.getLongitude());
        vo.setLatitude(alert.getLatitude());
        vo.setDetails(alert.getDetails());
        vo.setStatus(alert.getStatus());
        vo.setCreatedAt(TimeFormatter.format(alert.getCreatedAt()));
        return vo;
    }

    private RuleConfigVO toRuleVO(RuleConfig config) {
        RuleConfigVO vo = new RuleConfigVO();
        vo.setId(config.getId());
        vo.setRuleType(config.getRuleType());
        vo.setCode(config.getCode());
        vo.setName(config.getName());
        vo.setActiveVersion(config.getActiveVersion());
        vo.setStatus(config.getStatus());
        vo.setGrayPercent(config.getGrayPercent());
        vo.setCreatedAt(TimeFormatter.format(config.getCreatedAt()));
        vo.setUpdatedAt(TimeFormatter.format(config.getUpdatedAt()));
        vo.setVersions(versions(config.getId()).stream().map(this::toRuleVersionVO).toList());
        return vo;
    }

    private RuleVersionVO toRuleVersionVO(RuleVersion version) {
        RuleVersionVO vo = new RuleVersionVO();
        vo.setId(version.getId());
        vo.setVersion(version.getVersion());
        vo.setContent(version.getContent());
        vo.setStatus(version.getStatus());
        vo.setCreatedBy(version.getCreatedBy());
        vo.setPublishedAt(TimeFormatter.format(version.getPublishedAt()));
        vo.setCreatedAt(TimeFormatter.format(version.getCreatedAt()));
        return vo;
    }

    private DataRetentionPolicyVO toRetentionVO(DataRetentionPolicy policy) {
        DataRetentionPolicyVO vo = new DataRetentionPolicyVO();
        vo.setId(policy.getId());
        vo.setDataType(policy.getDataType());
        vo.setRetentionDays(policy.getRetentionDays());
        vo.setDeleteMode(policy.getDeleteMode());
        vo.setDescription(policy.getDescription());
        vo.setUpdatedBy(policy.getUpdatedBy());
        vo.setUpdatedAt(TimeFormatter.format(policy.getUpdatedAt()));
        return vo;
    }

    private UserRestrictionVO toRestrictionVO(BanRecord record) {
        UserRestrictionVO vo = new UserRestrictionVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setType(record.getBanType());
        vo.setReason(record.getReason());
        vo.setOperatorId(record.getOperatorId());
        vo.setStatus(record.getReleasedAt() == null ? "ACTIVE" : "RELEASED");
        vo.setStartsAt(TimeFormatter.format(record.getStartsAt()));
        vo.setReleasedAt(TimeFormatter.format(record.getReleasedAt()));
        return vo;
    }

    private String cell(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }
}
