package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.TimeFormatter;
import com.pintu.common.util.Masking;
import com.pintu.dto.MatchRequestCreateRequest;
import com.pintu.dto.MatchRequestVO;
import com.pintu.entity.DriverTrip;
import com.pintu.entity.Conversation;
import com.pintu.entity.MatchRequest;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.PassengerOrder;
import com.pintu.entity.User;
import com.pintu.enums.MatchRequestStatus;
import com.pintu.enums.OrderStatus;
import com.pintu.enums.DriverTripStatus;
import com.pintu.enums.PassengerOrderStatus;
import com.pintu.enums.PaymentStatus;
import com.pintu.enums.ReviewStatus;
import com.pintu.mapper.DriverTripMapper;
import com.pintu.mapper.MatchRequestMapper;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.PassengerOrderMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.ConversationMapper;
import com.pintu.service.MatchRequestService;
import com.pintu.service.matching.MatchEvaluation;
import com.pintu.service.matching.MatchingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchRequestServiceImpl implements MatchRequestService {

    private final MatchRequestMapper matchRequestMapper;
    private final PassengerOrderMapper passengerOrderMapper;
    private final DriverTripMapper driverTripMapper;
    private final OrderRecordMapper orderRecordMapper;
    private final UserMapper userMapper;
    private final MatchingEngine matchingEngine;
    private final ConversationMapper conversationMapper;

    @Value("${matching.request-expiry-minutes:120}")
    private long requestExpiryMinutes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MatchRequestVO create(MatchRequestCreateRequest request, String passengerId) {
        return createInternal(request, passengerId, "PASSENGER");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MatchRequestVO createByDriver(MatchRequestCreateRequest request, String driverId) {
        return createInternal(request, driverId, "DRIVER");
    }

    private MatchRequestVO createInternal(
            MatchRequestCreateRequest request, String actorId, String initiatorRole) {
        PassengerOrder passengerOrder = passengerOrderMapper.selectByIdForUpdate(request.getPassengerOrderId());
        if (passengerOrder == null) {
            throw new BusinessException(1404, "乘客需求不存在：" + request.getPassengerOrderId());
        }
        DriverTrip trip = driverTripMapper.selectByIdForUpdate(request.getDriverTripId());
        if (trip == null) {
            throw new BusinessException(1404, "司机行程不存在：" + request.getDriverTripId());
        }
        if ("PASSENGER".equals(initiatorRole)
                && actorId != null && !actorId.equals(passengerOrder.getPassengerId())) {
            throw new BusinessException(1004, "不能操作其他乘客的拼车需求");
        }
        if ("DRIVER".equals(initiatorRole)
                && actorId != null && !actorId.equals(trip.getDriverId())) {
            throw new BusinessException(1004, "不能使用其他司机的行程发起请求");
        }
        if (!PassengerOrderStatus.MATCHING.getValue().equals(passengerOrder.getStatus())
                && !PassengerOrderStatus.NEGOTIATING.getValue().equals(passengerOrder.getStatus())) {
            throw new BusinessException(1006, "当前需求状态不能发起匹配请求：" + passengerOrder.getStatus());
        }
        if (!DriverTripStatus.PUBLISHED.getValue().equals(trip.getStatus())) {
            throw new BusinessException(1007, "司机行程当前不可接单：" + trip.getStatus());
        }
        if (trip.getAvailableSeats() == null
                || passengerOrder.getPassengerCount() == null
                || trip.getAvailableSeats() < passengerOrder.getPassengerCount()) {
            throw new BusinessException(1007, "司机行程剩余座位不足");
        }
        User passenger = userMapper.selectById(passengerOrder.getPassengerId());
        User driver = userMapper.selectById(trip.getDriverId());
        MatchEvaluation evaluation = matchingEngine.evaluate(passengerOrder, trip, passenger, driver);
        if (!evaluation.eligible()) {
            throw new BusinessException(1018, "当前双方不满足匹配条件：" + evaluation.reason());
        }

        // 同一对乘客需求 + 司机行程仅允许一个有效请求
        QueryWrapper<MatchRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("passenger_order_id", passengerOrder.getId())
                .eq("driver_trip_id", trip.getId())
                .in("status", MatchRequestStatus.PENDING.getValue(), MatchRequestStatus.ACCEPTED.getValue());
        if (matchRequestMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(1008, "已存在有效请求，不能重复发起");
        }

        MatchRequest entity = new MatchRequest();
        entity.setId(IdGenerator.next(IdGenerator.PREFIX_MATCH_REQUEST));
        entity.setPassengerOrderId(passengerOrder.getId());
        entity.setDriverTripId(trip.getId());
        entity.setPassengerId(passengerOrder.getPassengerId());
        entity.setDriverId(trip.getDriverId());
        entity.setMessage(request.getMessage());
        entity.setInitiatorRole(initiatorRole);
        entity.setStatus(MatchRequestStatus.PENDING.getValue());
        entity.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(requestExpiryMinutes));
        matchRequestMapper.insert(entity);

        // 乘客需求状态进入「协商中」
        passengerOrder.setStatus(PassengerOrderStatus.NEGOTIATING.getValue());
        passengerOrderMapper.updateById(passengerOrder);

        return toVO(entity, passengerOrder, trip);
    }

    @Override
    public MatchRequestVO get(String requestId, String actorId) {
        MatchRequest entity = matchRequestMapper.selectById(requestId);
        if (entity == null) {
            throw new BusinessException(1404, "请求不存在：" + requestId);
        }
        requireParticipant(entity, actorId);
        expireIfNeeded(entity);
        PassengerOrder po = passengerOrderMapper.selectByIdForUpdate(entity.getPassengerOrderId());
        DriverTrip trip = driverTripMapper.selectById(entity.getDriverTripId());
        return toVO(entity, po, trip);
    }

    @Override
    public List<MatchRequestVO> listByDriverTrip(String driverTripId, String driverId) {
        DriverTrip ownerTrip = driverTripMapper.selectById(driverTripId);
        if (ownerTrip == null) {
            throw new BusinessException(1404, "司机行程不存在：" + driverTripId);
        }
        if (driverId != null && !driverId.equals(ownerTrip.getDriverId())) {
            throw new BusinessException(1004, "不能查看其他司机的匹配请求");
        }
        QueryWrapper<MatchRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("driver_trip_id", driverTripId).orderByDesc("created_at");
        List<MatchRequest> entities = matchRequestMapper.selectList(wrapper);
        List<MatchRequestVO> result = new ArrayList<>();
        for (MatchRequest entity : entities) {
            PassengerOrder po = passengerOrderMapper.selectById(entity.getPassengerOrderId());
            DriverTrip trip = driverTripMapper.selectById(entity.getDriverTripId());
            result.add(toVO(entity, po, trip));
        }
        return result;
    }

    @Override
    public List<MatchRequestVO> listByPassengerOrder(String passengerOrderId, String passengerId) {
        PassengerOrder ownerOrder = passengerOrderMapper.selectById(passengerOrderId);
        if (ownerOrder == null) throw new BusinessException(1404, "乘客需求不存在：" + passengerOrderId);
        if (passengerId != null && !passengerId.equals(ownerOrder.getPassengerId())) {
            throw new BusinessException(1004, "不能查看其他乘客的匹配请求");
        }
        List<MatchRequest> entities = matchRequestMapper.selectList(new QueryWrapper<MatchRequest>()
                .eq("passenger_order_id", passengerOrderId).orderByDesc("created_at"));
        List<MatchRequestVO> result = new ArrayList<>();
        for (MatchRequest entity : entities) {
            expireIfNeeded(entity);
            result.add(toVO(entity, ownerOrder, driverTripMapper.selectById(entity.getDriverTripId())));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MatchRequestVO accept(String requestId, String actorId) {
        MatchRequest entity = matchRequestMapper.selectByIdForUpdate(requestId);
        if (entity == null) {
            throw new BusinessException(1404, "请求不存在：" + requestId);
        }
        expireIfNeeded(entity);
        if (!MatchRequestStatus.PENDING.getValue().equals(entity.getStatus())) {
            throw new BusinessException(1009, "请求已处理，不能重复同意");
        }
        requireRecipient(entity, actorId);

        PassengerOrder po = passengerOrderMapper.selectByIdForUpdate(entity.getPassengerOrderId());
        if (po == null) {
            throw new BusinessException(1404, "关联数据缺失");
        }
        if (!PassengerOrderStatus.MATCHING.getValue().equals(po.getStatus())
                && !PassengerOrderStatus.NEGOTIATING.getValue().equals(po.getStatus())) {
            throw new BusinessException(1006, "乘客需求已进入其他流程：" + po.getStatus());
        }
        if (orderRecordMapper.selectLatestByPassengerOrderForUpdate(po.getId()) != null) {
            throw new BusinessException(1008, "该乘客需求已经生成正式订单");
        }
        DriverTrip trip = driverTripMapper.selectByIdForUpdate(entity.getDriverTripId());
        if (trip == null) {
            throw new BusinessException(1404, "关联数据缺失");
        }
        if (!DriverTripStatus.PUBLISHED.getValue().equals(trip.getStatus())) {
            throw new BusinessException(1007, "司机行程当前不可接单：" + trip.getStatus());
        }
        if (trip.getAvailableSeats() == null
                || po.getPassengerCount() == null
                || trip.getAvailableSeats() < po.getPassengerCount()) {
            throw new BusinessException(1007, "司机行程剩余座位不足");
        }

        // 司机信用与状态校验
        User driver = userMapper.selectById(trip.getDriverId());
        if (driver == null || driver.getCredit() == null || driver.getCredit() < 60
                || "限制接单".equals(driver.getStatus()) || "已封禁".equals(driver.getStatus())) {
            throw new BusinessException(1010, "司机当前不可接单");
        }

        // 接受只进入协商，不提前生成订单或扣减座位。
        passengerOrderMapper.updateById(po);

        entity.setStatus(MatchRequestStatus.ACCEPTED.getValue());
        entity.setHandledAt(TimeFormatter.nowDateTime());
        matchRequestMapper.updateById(entity);

        Conversation conversation = conversationMapper.selectOne(
                new QueryWrapper<Conversation>().eq("match_request_id", entity.getId()).last("limit 1"));
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setId(IdGenerator.next("conversation"));
            conversation.setMatchRequestId(entity.getId());
            conversation.setPassengerId(po.getPassengerId());
            conversation.setDriverId(trip.getDriverId());
            conversation.setStatus("协商中");
            conversationMapper.insert(conversation);
        }

        MatchRequest expired = new MatchRequest();
        expired.setStatus(MatchRequestStatus.EXPIRED.getValue());
        expired.setHandledAt(TimeFormatter.nowDateTime());
        UpdateWrapper<MatchRequest> expireWrapper = new UpdateWrapper<>();
        expireWrapper.eq("passenger_order_id", po.getId())
                .ne("id", entity.getId())
                .eq("status", MatchRequestStatus.PENDING.getValue());
        matchRequestMapper.update(expired, expireWrapper);

        return toVO(entity, po, trip);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MatchRequestVO reject(String requestId, String reason, String actorId) {
        MatchRequest entity = matchRequestMapper.selectByIdForUpdate(requestId);
        if (entity == null) {
            throw new BusinessException(1404, "请求不存在：" + requestId);
        }
        expireIfNeeded(entity);
        if (!MatchRequestStatus.PENDING.getValue().equals(entity.getStatus())) {
            throw new BusinessException(1009, "请求已处理");
        }
        requireRecipient(entity, actorId);
        entity.setStatus(MatchRequestStatus.REJECTED.getValue());
        entity.setRejectReason(reason);
        entity.setHandledAt(TimeFormatter.nowDateTime());
        matchRequestMapper.updateById(entity);

        // 把乘客需求回退到「待匹配」，方便重新选择司机
        PassengerOrder po = passengerOrderMapper.selectById(entity.getPassengerOrderId());
        QueryWrapper<MatchRequest> pendingWrapper = new QueryWrapper<>();
        pendingWrapper.eq("passenger_order_id", entity.getPassengerOrderId())
                .eq("status", MatchRequestStatus.PENDING.getValue());
        if (po != null
                && PassengerOrderStatus.NEGOTIATING.getValue().equals(po.getStatus())
                && matchRequestMapper.selectCount(pendingWrapper) == 0) {
            po.setStatus(PassengerOrderStatus.MATCHING.getValue());
            passengerOrderMapper.updateById(po);
        }
        DriverTrip trip = driverTripMapper.selectById(entity.getDriverTripId());
        return toVO(entity, po, trip);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MatchRequestVO withdraw(String requestId, String actorId) {
        MatchRequest entity = matchRequestMapper.selectByIdForUpdate(requestId);
        if (entity == null) throw new BusinessException(1404, "请求不存在：" + requestId);
        expireIfNeeded(entity);
        if (!MatchRequestStatus.PENDING.getValue().equals(entity.getStatus())) {
            throw new BusinessException(1009, "只有待处理请求可以撤回");
        }
        String initiatorId = "DRIVER".equals(entity.getInitiatorRole())
                ? entity.getDriverId() : entity.getPassengerId();
        if (actorId != null && !actorId.equals(initiatorId)) {
            throw new BusinessException(1004, "只有发起方可以撤回请求");
        }
        entity.setStatus(MatchRequestStatus.WITHDRAWN.getValue());
        entity.setHandledAt(TimeFormatter.nowDateTime());
        matchRequestMapper.updateById(entity);
        PassengerOrder po = passengerOrderMapper.selectById(entity.getPassengerOrderId());
        restoreMatchingIfNoPending(po);
        return toVO(entity, po, driverTripMapper.selectById(entity.getDriverTripId()));
    }

    private MatchRequestVO toVO(MatchRequest entity, PassengerOrder po, DriverTrip trip) {
        MatchRequestVO vo = new MatchRequestVO();
        vo.setId(entity.getId());
        vo.setPassengerOrderId(entity.getPassengerOrderId());
        vo.setDriverTripId(entity.getDriverTripId());
        vo.setPassengerId(entity.getPassengerId());
        vo.setDriverId(entity.getDriverId());
        vo.setStatus(entity.getStatus());
        vo.setInitiatorRole(entity.getInitiatorRole());
        vo.setRejectReason(entity.getRejectReason());
        vo.setOrderId(entity.getOrderId());
        vo.setCreatedAt(TimeFormatter.format(entity.getCreatedAt()));
        vo.setHandledAt(TimeFormatter.format(entity.getHandledAt()));
        vo.setExpiresAt(TimeFormatter.format(entity.getExpiresAt()));
        Conversation conversation = conversationMapper.selectOne(
                new QueryWrapper<Conversation>().eq("match_request_id", entity.getId()).last("limit 1"));
        vo.setConversationId(conversation == null ? null : conversation.getId());

        if (po != null) {
            vo.setRoute(po.getStartLocation() + " -> " + po.getEndLocation());
            vo.setTime(TimeFormatter.format(po.getDepartureTime()));
            User passenger = userMapper.selectById(po.getPassengerId());
            if (passenger != null) {
                vo.setPassenger(Masking.name(passenger.getName()));
                vo.setCredit(passenger.getCredit());
            }
        }
        if (trip != null) {
            vo.setOverlap(trip.getOverlap());
            vo.setPrice(trip.getPrice());
        }
        return vo;
    }

    private void requireParticipant(MatchRequest request, String actorId) {
        if (actorId != null
                && !actorId.equals(request.getPassengerId())
                && !actorId.equals(request.getDriverId())) {
            throw new BusinessException(1004, "不能查看与当前用户无关的匹配请求");
        }
    }

    private void requireRecipient(MatchRequest request, String actorId) {
        String recipientId = "DRIVER".equals(request.getInitiatorRole())
                ? request.getPassengerId() : request.getDriverId();
        if (actorId != null && !actorId.equals(recipientId)) {
            throw new BusinessException(1004, "只有接收方可以处理匹配请求");
        }
    }

    private void expireIfNeeded(MatchRequest request) {
        if (MatchRequestStatus.PENDING.getValue().equals(request.getStatus())
                && request.getExpiresAt() != null
                && request.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            request.setStatus(MatchRequestStatus.EXPIRED.getValue());
            request.setHandledAt(TimeFormatter.nowDateTime());
            matchRequestMapper.updateById(request);
        }
    }

    private void restoreMatchingIfNoPending(PassengerOrder po) {
        if (po == null || !PassengerOrderStatus.NEGOTIATING.getValue().equals(po.getStatus())) return;
        Long pending = matchRequestMapper.selectCount(new QueryWrapper<MatchRequest>()
                .eq("passenger_order_id", po.getId())
                .eq("status", MatchRequestStatus.PENDING.getValue()));
        if (pending == 0) {
            po.setStatus(PassengerOrderStatus.MATCHING.getValue());
            passengerOrderMapper.updateById(po);
        }
    }
}
