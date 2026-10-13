package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.GeoDistance;
import com.pintu.common.util.GeoPrivacy;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.Masking;
import com.pintu.common.util.TimeFormatter;
import com.pintu.dto.EmergencyContactSaveRequest;
import com.pintu.dto.EmergencyContactVO;
import com.pintu.dto.DataRetentionPolicyVO;
import com.pintu.dto.LocationBatchReportRequest;
import com.pintu.dto.LocationPointVO;
import com.pintu.dto.LocationReportRequest;
import com.pintu.dto.SafetyAlertCreateRequest;
import com.pintu.dto.SafetyAlertVO;
import com.pintu.dto.TripShareCreateRequest;
import com.pintu.dto.TripShareLinkVO;
import com.pintu.dto.TripShareViewVO;
import com.pintu.dto.VirtualPhoneBindingVO;
import com.pintu.entity.EmergencyContact;
import com.pintu.entity.DataRetentionPolicy;
import com.pintu.entity.LocationReport;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.SafetyAlert;
import com.pintu.entity.TrajectoryPoint;
import com.pintu.entity.TripShareToken;
import com.pintu.entity.User;
import com.pintu.entity.VirtualPhoneBinding;
import com.pintu.enums.OrderStatus;
import com.pintu.mapper.EmergencyContactMapper;
import com.pintu.mapper.DataRetentionPolicyMapper;
import com.pintu.mapper.LocationReportMapper;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.SafetyAlertMapper;
import com.pintu.mapper.TrajectoryPointMapper;
import com.pintu.mapper.TripShareTokenMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.VirtualPhoneBindingMapper;
import com.pintu.security.SensitiveDataCrypto;
import com.pintu.service.SafetyService;
import com.pintu.service.SecurityAuditService;
import com.pintu.service.phone.VirtualPhoneProvider;
import com.pintu.service.phone.VirtualPhoneProvision;
import com.pintu.service.police.PoliceGateway;
import com.pintu.service.police.PoliceReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SafetyServiceImpl implements SafetyService {

    private static final String ALERT_PENDING = "\u5f85\u5904\u7406";
    private static final String ALERT_REPORTED = "\u5df2\u4e0a\u62a5";
    private static final String BINDING_ACTIVE = "ACTIVE";
    private static final String BINDING_UNBOUND = "UNBOUND";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmergencyContactMapper emergencyContactMapper;
    private final DataRetentionPolicyMapper dataRetentionPolicyMapper;
    private final SafetyAlertMapper safetyAlertMapper;
    private final OrderRecordMapper orderRecordMapper;
    private final VirtualPhoneBindingMapper virtualPhoneBindingMapper;
    private final TripShareTokenMapper tripShareTokenMapper;
    private final LocationReportMapper locationReportMapper;
    private final TrajectoryPointMapper trajectoryPointMapper;
    private final UserMapper userMapper;
    private final SensitiveDataCrypto sensitiveDataCrypto;
    private final PoliceGateway policeGateway;
    private final VirtualPhoneProvider virtualPhoneProvider;
    private final SecurityAuditService securityAuditService;

    @Override
    public List<EmergencyContactVO> listEmergencyContacts(String userId) {
        return emergencyContactMapper.selectList(new QueryWrapper<EmergencyContact>()
                        .eq("user_id", userId)
                        .orderByDesc("is_primary")
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toContactVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmergencyContactVO createEmergencyContact(String userId, EmergencyContactSaveRequest request) {
        String phone = requirePhone(request.getPhone());
        EmergencyContact contact = new EmergencyContact();
        contact.setId(IdGenerator.next("contact"));
        contact.setUserId(userId);
        applyContact(contact, request, phone);
        LocalDateTime now = TimeFormatter.nowDateTime();
        contact.setCreatedAt(now);
        contact.setUpdatedAt(now);
        if (contact.getIsPrimary() == 1) {
            clearPrimaryContact(userId, null);
        }
        emergencyContactMapper.insert(contact);
        return toContactVO(contact);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmergencyContactVO updateEmergencyContact(
            String userId,
            String contactId,
            EmergencyContactSaveRequest request) {
        EmergencyContact contact = findOwnedContact(userId, contactId);
        String phone = request.getPhone() == null || request.getPhone().isBlank()
                ? null
                : requirePhone(request.getPhone());
        applyContact(contact, request, phone);
        contact.setUpdatedAt(TimeFormatter.nowDateTime());
        if (contact.getIsPrimary() == 1) {
            clearPrimaryContact(userId, contactId);
        }
        emergencyContactMapper.updateById(contact);
        return toContactVO(contact);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmergencyContact(String userId, String contactId) {
        EmergencyContact contact = findOwnedContact(userId, contactId);
        emergencyContactMapper.deleteById(contact.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SafetyAlertVO createOrderAlert(String userId, String orderId, SafetyAlertCreateRequest request) {
        OrderRecord order = requireOrder(orderId);
        requireParticipant(order, userId);
        if (!OrderStatus.ONGOING.getValue().equals(order.getStatus())) {
            throw new BusinessException(1014, "Only ongoing orders can trigger safety alerts");
        }

        SafetyAlert alert = new SafetyAlert();
        alert.setId(IdGenerator.next("alert"));
        alert.setOrderId(orderId);
        alert.setUserId(userId);
        alert.setAlertType(request.getAlertType().trim());
        alert.setLongitude(request.getLongitude());
        alert.setLatitude(request.getLatitude());
        alert.setDetails(normalize(request.getDetails()));
        alert.setStatus(ALERT_PENDING);
        alert.setCreatedAt(TimeFormatter.nowDateTime());
        safetyAlertMapper.insert(alert);

        PoliceReport report = policeGateway.report(alert);
        alert.setStatus(ALERT_REPORTED);
        safetyAlertMapper.updateById(alert);
        return toAlertVO(alert, report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VirtualPhoneBindingVO ensureVirtualPhoneBound(String orderId, String userId) {
        OrderRecord order = requireOrder(orderId);
        requireParticipant(order, userId);
        if (!OrderStatus.ONGOING.getValue().equals(order.getStatus())) {
            throw new BusinessException(1014, "Virtual phone is only available for ongoing orders");
        }
        VirtualPhoneBinding existing = activeVirtualPhone(orderId);
        if (existing != null) {
            return toVirtualPhoneVO(existing);
        }

        VirtualPhoneProvision provision = virtualPhoneProvider.bind(order);
        LocalDateTime now = TimeFormatter.nowDateTime();
        VirtualPhoneBinding binding = new VirtualPhoneBinding();
        binding.setId(IdGenerator.next("vpb"));
        binding.setOrderId(order.getId());
        binding.setPassengerId(order.getPassengerId());
        binding.setDriverId(order.getDriverId());
        binding.setVirtualNumber(provision.virtualNumber());
        binding.setProviderBindingId(provision.providerBindingId());
        binding.setStatus(BINDING_ACTIVE);
        binding.setBoundAt(now);
        binding.setCreatedAt(now);
        binding.setUpdatedAt(now);
        virtualPhoneBindingMapper.insert(binding);
        securityAuditService.record(userId, "VIRTUAL_PHONE_BIND", orderId, null, true, "mock_provider");
        return toVirtualPhoneVO(binding);
    }

    @Override
    public VirtualPhoneBindingVO getVirtualPhone(String orderId, String userId) {
        OrderRecord order = requireOrder(orderId);
        requireParticipant(order, userId);
        VirtualPhoneBinding binding = activeVirtualPhone(orderId);
        return binding == null ? null : toVirtualPhoneVO(binding);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindOrderVirtualPhone(String orderId, String reason) {
        VirtualPhoneBinding binding = activeVirtualPhone(orderId);
        if (binding == null) {
            return;
        }
        virtualPhoneProvider.unbind(binding.getProviderBindingId());
        binding.setStatus(BINDING_UNBOUND);
        binding.setUnboundAt(TimeFormatter.nowDateTime());
        binding.setUnbindReason(reason);
        binding.setUpdatedAt(TimeFormatter.nowDateTime());
        virtualPhoneBindingMapper.updateById(binding);
        securityAuditService.record(null, "VIRTUAL_PHONE_UNBIND", orderId, null, true, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeOrderSafetyResources(String orderId, String reason) {
        unbindOrderVirtualPhone(orderId, reason);
        LocalDateTime now = TimeFormatter.nowDateTime();
        List<TripShareToken> tokens = tripShareTokenMapper.selectList(new QueryWrapper<TripShareToken>()
                .eq("order_id", orderId)
                .isNull("revoked_at"));
        for (TripShareToken token : tokens) {
            token.setRevokedAt(now);
            tripShareTokenMapper.updateById(token);
        }
        if (!tokens.isEmpty()) {
            securityAuditService.record(null, "TRIP_SHARE_REVOKE", orderId, null, true,
                    reason + ", count=" + tokens.size());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TripShareLinkVO createShareLink(String userId, String orderId, TripShareCreateRequest request) {
        OrderRecord order = requireOrder(orderId);
        requireParticipant(order, userId);
        if (!OrderStatus.ONGOING.getValue().equals(order.getStatus())) {
            throw new BusinessException(1014, "Trip sharing is only available for ongoing orders");
        }
        int expiresMinutes = request == null || request.getExpiresMinutes() == null
                ? 120
                : request.getExpiresMinutes();
        LocalDateTime now = TimeFormatter.nowDateTime();
        TripShareToken shareToken = new TripShareToken();
        shareToken.setId(IdGenerator.next("share"));
        shareToken.setToken(generateToken());
        shareToken.setOrderId(orderId);
        shareToken.setCreatedBy(userId);
        shareToken.setExpiresAt(now.plusMinutes(expiresMinutes));
        shareToken.setCreatedAt(now);
        tripShareTokenMapper.insert(shareToken);
        securityAuditService.record(userId, "TRIP_SHARE_CREATE", orderId, null, true,
                "expiresMinutes=" + expiresMinutes);
        return toShareLinkVO(shareToken);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TripShareViewVO getShareView(String token) {
        TripShareToken shareToken = tripShareTokenMapper.selectOne(new QueryWrapper<TripShareToken>()
                .eq("token", token)
                .isNull("revoked_at")
                .gt("expires_at", TimeFormatter.nowDateTime())
                .last("limit 1"));
        if (shareToken == null) {
            throw new BusinessException(1404, "Share link is invalid or expired");
        }
        OrderRecord order = requireOrder(shareToken.getOrderId());
        if (!OrderStatus.ONGOING.getValue().equals(order.getStatus())) {
            shareToken.setRevokedAt(TimeFormatter.nowDateTime());
            tripShareTokenMapper.updateById(shareToken);
            throw new BusinessException(1014, "Share link has been revoked because the trip is no longer ongoing");
        }
        User passenger = userMapper.selectById(order.getPassengerId());
        User driver = userMapper.selectById(order.getDriverId());

        TripShareViewVO vo = new TripShareViewVO();
        vo.setOrderId(order.getId());
        vo.setRoute(maskLocation(order.getStartLocation()) + " -> " + maskLocation(order.getEndLocation()));
        vo.setStatus(order.getStatus());
        vo.setPassenger(passenger == null ? null : Masking.name(passenger.getName()));
        vo.setDriver(driver == null ? null : Masking.name(driver.getName()));
        vo.setStartLng(GeoPrivacy.blur(order.getStartLng()));
        vo.setStartLat(GeoPrivacy.blur(order.getStartLat()));
        vo.setEndLng(GeoPrivacy.blur(order.getEndLng()));
        vo.setEndLat(GeoPrivacy.blur(order.getEndLat()));
        vo.setDepartureTime(TimeFormatter.format(order.getDepartureTime()));
        vo.setExpiresAt(TimeFormatter.format(shareToken.getExpiresAt()));
        vo.setTrajectory(trajectoryPointMapper.selectList(new QueryWrapper<TrajectoryPoint>()
                        .eq("order_id", order.getId())
                        .orderByAsc("sequence_no"))
                .stream()
                .map(this::toPublicLocationPointVO)
                .toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LocationPointVO reportDriverLocation(String driverId, String orderId, LocationReportRequest request) {
        return reportDriverLocationInternal(driverId, orderId, request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<LocationPointVO> reportDriverLocationBatch(
            String driverId,
            String orderId,
            LocationBatchReportRequest request) {
        return request.getPoints().stream()
                .map(point -> reportDriverLocationInternal(driverId, orderId, point, request.getBatchId()))
                .toList();
    }

    private LocationPointVO reportDriverLocationInternal(
            String driverId,
            String orderId,
            LocationReportRequest request,
            String batchId) {
        OrderRecord order = requireOrder(orderId);
        if (driverId != null && !driverId.equals(order.getDriverId())) {
            throw new BusinessException(1004, "Only the order driver can report location");
        }
        if (!OrderStatus.ONGOING.getValue().equals(order.getStatus())) {
            throw new BusinessException(1014, "Precise location reporting stops when the trip is not ongoing");
        }
        validateLocationReportMeta(request);

        LocalDateTime reportedAt = request.getReportedAt() == null ? TimeFormatter.nowDateTime() : request.getReportedAt();
        if (request.getClientReportId() != null && !request.getClientReportId().isBlank()) {
            LocationReport existing = locationReportMapper.selectOne(new QueryWrapper<LocationReport>()
                    .eq("order_id", orderId)
                    .eq("user_id", driverId)
                    .eq("client_report_id", request.getClientReportId().trim())
                    .last("limit 1"));
            if (existing != null) {
                return toLocationPointVO(existing, null);
            }
        }
        LocationReport previous = latestLocation(orderId);

        LocationReport report = new LocationReport();
        report.setId(IdGenerator.next("loc"));
        report.setOrderId(orderId);
        report.setUserId(driverId);
        report.setLongitude(request.getLongitude());
        report.setLatitude(request.getLatitude());
        report.setAccuracy(request.getAccuracy());
        report.setSpeed(request.getSpeed());
        report.setHeading(request.getHeading());
        report.setClientReportId(normalize(request.getClientReportId()));
        report.setBatchId(normalize(batchId));
        report.setReportIntervalSeconds(request.getReportIntervalSeconds());
        report.setRetryCount(request.getRetryCount() == null ? 0 : request.getRetryCount());
        report.setReportedAt(reportedAt);
        report.setCreatedAt(TimeFormatter.nowDateTime());
        locationReportMapper.insert(report);

        int nextSequence = Math.toIntExact(trajectoryPointMapper.selectCount(new QueryWrapper<TrajectoryPoint>()
                .eq("order_id", orderId)) + 1);
        TrajectoryPoint point = new TrajectoryPoint();
        point.setId(IdGenerator.next("traj"));
        point.setOrderId(orderId);
        point.setSequenceNo(nextSequence);
        point.setLongitude(request.getLongitude());
        point.setLatitude(request.getLatitude());
        point.setRecordedAt(reportedAt);
        trajectoryPointMapper.insert(point);

        createLocationWarnings(order, previous, report);
        return toLocationPointVO(report, nextSequence);
    }

    private void validateLocationReportMeta(LocationReportRequest request) {
        if (request.getReportIntervalSeconds() != null
                && (request.getReportIntervalSeconds() < 1 || request.getReportIntervalSeconds() > 10)) {
            throw new BusinessException(1002, "Location report interval must be between 1 and 10 seconds");
        }
        if (request.getRetryCount() != null && request.getRetryCount() < 0) {
            throw new BusinessException(1002, "Location retry count cannot be negative");
        }
    }

    @Override
    public List<LocationPointVO> listTrajectory(String userId, String orderId) {
        OrderRecord order = requireOrder(orderId);
        requireParticipant(order, userId);
        if (!OrderStatus.ONGOING.getValue().equals(order.getStatus())) {
            return List.of();
        }
        return trajectoryPointMapper.selectList(new QueryWrapper<TrajectoryPoint>()
                        .eq("order_id", orderId)
                        .orderByAsc("sequence_no"))
                .stream()
                .map(this::toLocationPointVO)
                .toList();
    }

    @Override
    public List<DataRetentionPolicyVO> listRetentionPolicies() {
        return dataRetentionPolicyMapper.selectList(new QueryWrapper<DataRetentionPolicy>()
                        .orderByAsc("data_type"))
                .stream()
                .map(this::toRetentionPolicyVO)
                .toList();
    }

    private void applyContact(EmergencyContact contact, EmergencyContactSaveRequest request, String phone) {
        contact.setName(request.getName().trim());
        contact.setRelationship(normalize(request.getRelationship()));
        contact.setIsPrimary(Boolean.TRUE.equals(request.getIsPrimary()) ? 1 : 0);
        if (phone != null) {
            contact.setPhoneCipher(sensitiveDataCrypto.encrypt(phone));
            contact.setPhoneMasked(Masking.phone(phone));
        }
    }

    private String requirePhone(String phone) {
        String normalized = phone == null ? "" : phone.trim();
        if (!normalized.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(1002, "Please enter a valid mainland China mobile phone number");
        }
        return normalized;
    }

    private void clearPrimaryContact(String userId, String excludedId) {
        emergencyContactMapper.selectList(new QueryWrapper<EmergencyContact>()
                        .eq("user_id", userId)
                        .eq("is_primary", 1))
                .stream()
                .filter(contact -> excludedId == null || !excludedId.equals(contact.getId()))
                .forEach(contact -> {
                    contact.setIsPrimary(0);
                    contact.setUpdatedAt(TimeFormatter.nowDateTime());
                    emergencyContactMapper.updateById(contact);
                });
    }

    private EmergencyContact findOwnedContact(String userId, String contactId) {
        EmergencyContact contact = emergencyContactMapper.selectOne(new QueryWrapper<EmergencyContact>()
                .eq("id", contactId)
                .eq("user_id", userId)
                .last("limit 1"));
        if (contact == null) {
            throw new BusinessException(1404, "Emergency contact does not exist");
        }
        return contact;
    }

    private EmergencyContactVO toContactVO(EmergencyContact contact) {
        EmergencyContactVO vo = new EmergencyContactVO();
        vo.setId(contact.getId());
        vo.setName(contact.getName());
        vo.setPhoneMasked(contact.getPhoneMasked());
        vo.setRelationship(contact.getRelationship());
        vo.setIsPrimary(contact.getIsPrimary() != null && contact.getIsPrimary() == 1);
        vo.setCreatedAt(TimeFormatter.format(contact.getCreatedAt()));
        vo.setUpdatedAt(TimeFormatter.format(contact.getUpdatedAt()));
        return vo;
    }

    private SafetyAlertVO toAlertVO(SafetyAlert alert, PoliceReport report) {
        SafetyAlertVO vo = new SafetyAlertVO();
        vo.setId(alert.getId());
        vo.setOrderId(alert.getOrderId());
        vo.setAlertType(alert.getAlertType());
        vo.setLongitude(alert.getLongitude());
        vo.setLatitude(alert.getLatitude());
        vo.setDetails(alert.getDetails());
        vo.setStatus(alert.getStatus());
        vo.setCreatedAt(TimeFormatter.format(alert.getCreatedAt()));
        vo.setPoliceReportId(report.reportId());
        vo.setPoliceMessage(report.message());
        return vo;
    }

    private OrderRecord requireOrder(String orderId) {
        OrderRecord order = orderRecordMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(1404, "Order does not exist: " + orderId);
        }
        return order;
    }

    private void requireParticipant(OrderRecord order, String userId) {
        if (userId != null && !userId.equals(order.getPassengerId()) && !userId.equals(order.getDriverId())) {
            throw new BusinessException(1004, "Only order participants can access this safety resource");
        }
    }

    private VirtualPhoneBinding activeVirtualPhone(String orderId) {
        return virtualPhoneBindingMapper.selectOne(new QueryWrapper<VirtualPhoneBinding>()
                .eq("order_id", orderId)
                .eq("status", BINDING_ACTIVE)
                .last("limit 1"));
    }

    private VirtualPhoneBindingVO toVirtualPhoneVO(VirtualPhoneBinding binding) {
        VirtualPhoneBindingVO vo = new VirtualPhoneBindingVO();
        vo.setId(binding.getId());
        vo.setOrderId(binding.getOrderId());
        vo.setVirtualNumber(binding.getVirtualNumber());
        vo.setStatus(binding.getStatus());
        vo.setBoundAt(TimeFormatter.format(binding.getBoundAt()));
        vo.setUnboundAt(TimeFormatter.format(binding.getUnboundAt()));
        return vo;
    }

    private TripShareLinkVO toShareLinkVO(TripShareToken shareToken) {
        TripShareLinkVO vo = new TripShareLinkVO();
        vo.setToken(shareToken.getToken());
        vo.setPublicPath("/share/" + shareToken.getToken());
        vo.setOrderId(shareToken.getOrderId());
        vo.setExpiresAt(TimeFormatter.format(shareToken.getExpiresAt()));
        return vo;
    }

    private LocationReport latestLocation(String orderId) {
        return locationReportMapper.selectOne(new QueryWrapper<LocationReport>()
                .eq("order_id", orderId)
                .orderByDesc("reported_at")
                .last("limit 1"));
    }

    private void createLocationWarnings(OrderRecord order, LocationReport previous, LocationReport current) {
        LocalDateTime now = TimeFormatter.nowDateTime();
        if (current.getReportedAt() != null && current.getReportedAt().isBefore(now.minusSeconds(90))) {
            createSystemAlert(order.getId(), current.getUserId(), "LOCATION_STALE", current, "stale location report");
        }
        double toStart = GeoDistance.km(order.getStartLng(), order.getStartLat(), current.getLongitude(), current.getLatitude());
        double toEnd = GeoDistance.km(order.getEndLng(), order.getEndLat(), current.getLongitude(), current.getLatitude());
        double route = GeoDistance.km(order.getStartLng(), order.getStartLat(), order.getEndLng(), order.getEndLat());
        if (route < Double.MAX_VALUE && toStart > route + 5 && toEnd > route + 5) {
            createSystemAlert(order.getId(), current.getUserId(), "ROUTE_DEVIATION", current, "location is outside route corridor");
        }
        if (previous != null
                && current.getReportedAt() != null
                && previous.getReportedAt() != null
                && Duration.between(previous.getReportedAt(), current.getReportedAt()).toMinutes() >= 5
                && GeoDistance.km(previous.getLongitude(), previous.getLatitude(),
                    current.getLongitude(), current.getLatitude()) < 0.1) {
            createSystemAlert(order.getId(), current.getUserId(), "ABNORMAL_STOP", current, "vehicle stopped for more than 5 minutes");
        }
    }

    private void createSystemAlert(String orderId, String userId, String type, LocationReport report, String details) {
        SafetyAlert alert = new SafetyAlert();
        alert.setId(IdGenerator.next("alert"));
        alert.setOrderId(orderId);
        alert.setUserId(userId);
        alert.setAlertType(type);
        alert.setLongitude(report.getLongitude());
        alert.setLatitude(report.getLatitude());
        alert.setDetails(details);
        alert.setStatus(ALERT_PENDING);
        alert.setCreatedAt(TimeFormatter.nowDateTime());
        safetyAlertMapper.insert(alert);
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

    private DataRetentionPolicyVO toRetentionPolicyVO(DataRetentionPolicy policy) {
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

    private LocationPointVO toPublicLocationPointVO(TrajectoryPoint point) {
        LocationPointVO vo = toLocationPointVO(point);
        vo.setLongitude(GeoPrivacy.blur(vo.getLongitude()));
        vo.setLatitude(GeoPrivacy.blur(vo.getLatitude()));
        vo.setAccuracy(null);
        vo.setSpeed(null);
        vo.setHeading(null);
        return vo;
    }

    private String generateToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String maskLocation(String location) {
        if (location == null || location.isBlank()) {
            return "unknown";
        }
        String trimmed = location.trim();
        return trimmed.length() <= 2 ? trimmed + "***" : trimmed.substring(0, 2) + "***";
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
