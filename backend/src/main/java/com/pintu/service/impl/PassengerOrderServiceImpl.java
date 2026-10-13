package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.TimeFormatter;
import com.pintu.common.util.Masking;
import com.pintu.common.util.GeoPrivacy;
import com.pintu.dto.CancelRequest;
import com.pintu.dto.PassengerOrderCreateRequest;
import com.pintu.dto.PassengerOrderVO;
import com.pintu.dto.RecommendedTripVO;
import com.pintu.dto.GeoPointVO;
import com.pintu.entity.DriverTrip;
import com.pintu.entity.MatchRequest;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.PassengerOrder;
import com.pintu.entity.User;
import com.pintu.entity.Vehicle;
import com.pintu.enums.OrderStatus;
import com.pintu.enums.DriverTripStatus;
import com.pintu.enums.MatchRequestStatus;
import com.pintu.enums.PassengerOrderStatus;
import com.pintu.mapper.DriverTripMapper;
import com.pintu.mapper.MatchRequestMapper;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.PassengerOrderMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.VehicleMapper;
import com.pintu.service.PassengerOrderService;
import com.pintu.service.MapService;
import com.pintu.service.AddressService;
import com.pintu.service.SafetyService;
import com.pintu.service.matching.MatchEvaluation;
import com.pintu.service.matching.MatchingEngine;
import com.pintu.security.SensitiveDataCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PassengerOrderServiceImpl implements PassengerOrderService {

    private final PassengerOrderMapper passengerOrderMapper;
    private final DriverTripMapper driverTripMapper;
    private final UserMapper userMapper;
    private final VehicleMapper vehicleMapper;
    private final OrderRecordMapper orderRecordMapper;
    private final MatchRequestMapper matchRequestMapper;
    private final SensitiveDataCrypto sensitiveDataCrypto;
    private final MapService mapService;
    private final AddressService addressService;
    private final MatchingEngine matchingEngine;
    private final SafetyService safetyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PassengerOrderVO create(PassengerOrderCreateRequest request) {
        String passengerId = request.getPassengerId() == null ? "passenger-001" : request.getPassengerId();
        User passenger = userMapper.selectById(passengerId);
        if (passenger == null) {
            throw new BusinessException(1404, "乘客不存在：" + passengerId);
        }
        if (passenger.getCredit() == null || passenger.getCredit() < 60
                || "已封禁".equals(passenger.getStatus()) || "限制发单".equals(passenger.getStatus())) {
            throw new BusinessException(1005, "当前账号状态不允许发布需求：" + passenger.getStatus());
        }
        validateDepartureTime(request.getDepartureTime());
        populateCoordinates(request);

        PassengerOrder order = new PassengerOrder();
        order.setId(IdGenerator.next(IdGenerator.PREFIX_PASSENGER_ORDER));
        order.setPassengerId(passengerId);
        order.setStartLocation(request.getStart());
        order.setEndLocation(request.getEnd());
        order.setStartLng(request.getStartLng());
        order.setStartLat(request.getStartLat());
        order.setEndLng(request.getEndLng());
        order.setEndLat(request.getEndLat());
        order.setDepartureTime(request.getDepartureTime());
        order.setPassengerCount(request.getPassengerCount());
        order.setNote(request.getNote());
        order.setStatus(PassengerOrderStatus.MATCHING.getValue());
        passengerOrderMapper.insert(order);
        addressService.recordHistory(passengerId, request.getStart(), request.getStart(),
                request.getStartLng(), request.getStartLat());
        addressService.recordHistory(passengerId, request.getEnd(), request.getEnd(),
                request.getEndLng(), request.getEndLat());

        return toVO(order, passenger, null, null);
    }

    private void validateDepartureTime(java.time.LocalDateTime departureTime) {
        if (departureTime == null || departureTime.isBefore(java.time.LocalDateTime.now().minusSeconds(90))) {
            throw new BusinessException(1002, "出发时间不能早于当前时间");
        }
    }

    private void populateCoordinates(PassengerOrderCreateRequest request) {
        if (request.getStartLng() == null || request.getStartLat() == null) {
            GeoPointVO start = mapService.geocode(request.getStart());
            request.setStartLng(start.getLng());
            request.setStartLat(start.getLat());
        }
        if (request.getEndLng() == null || request.getEndLat() == null) {
            GeoPointVO end = mapService.geocode(request.getEnd());
            request.setEndLng(end.getLng());
            request.setEndLat(end.getLat());
        }
        mapService.route(request.getStartLng(), request.getStartLat(),
                request.getEndLng(), request.getEndLat());
    }

    @Override
    public List<PassengerOrderVO> listByPassenger(String passengerId, String status) {
        QueryWrapper<PassengerOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("passenger_id", passengerId).orderByDesc("created_at");
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        List<PassengerOrder> records = passengerOrderMapper.selectList(wrapper);
        User passenger = userMapper.selectById(passengerId);
        List<PassengerOrderVO> result = new ArrayList<>();
        for (PassengerOrder record : records) {
            OrderRecord orderRecord = findLatestOrderByPassengerOrder(record.getId());
            result.add(toVO(record, passenger, orderRecord, loadDriverFor(orderRecord)));
        }
        return result;
    }

    @Override
    public PassengerOrderVO detail(String orderOrPassengerOrderId, String passengerId) {
        PassengerOrder passengerOrder = loadPassengerOrder(orderOrPassengerOrderId);
        requireOwner(passengerOrder, passengerId);
        OrderRecord orderRecord = findLatestOrderByPassengerOrder(passengerOrder.getId());
        User passenger = userMapper.selectById(passengerOrder.getPassengerId());
        return toVO(passengerOrder, passenger, orderRecord, loadDriverFor(orderRecord));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PassengerOrderVO cancel(String orderOrPassengerOrderId, CancelRequest request, String passengerId) {
        OrderRecord orderReference = orderRecordMapper.selectById(orderOrPassengerOrderId);
        String passengerOrderId = orderReference == null ? orderOrPassengerOrderId : orderReference.getPassengerOrderId();
        PassengerOrder passengerOrder = passengerOrderMapper.selectByIdForUpdate(passengerOrderId);
        if (passengerOrder == null) {
            throw new BusinessException(1404, "需求或订单不存在：" + orderOrPassengerOrderId);
        }
        requireOwner(passengerOrder, passengerId);
        if (PassengerOrderStatus.COMPLETED.getValue().equals(passengerOrder.getStatus())
                || PassengerOrderStatus.CANCELLED.getValue().equals(passengerOrder.getStatus())) {
            throw new BusinessException(1006, "当前状态不允许取消：" + passengerOrder.getStatus());
        }

        OrderRecord orderRecord = orderReference == null
                ? orderRecordMapper.selectLatestByPassengerOrderForUpdate(passengerOrder.getId())
                : orderRecordMapper.selectByIdForUpdate(orderReference.getId());
        if (orderRecord != null
                && (OrderStatus.COMPLETED.getValue().equals(orderRecord.getStatus())
                || OrderStatus.CANCELLED.getValue().equals(orderRecord.getStatus()))) {
            throw new BusinessException(1006, "当前状态不允许取消：" + orderRecord.getStatus());
        }
        if (orderRecord != null
                && !OrderStatus.COMPLETED.getValue().equals(orderRecord.getStatus())
                && !OrderStatus.CANCELLED.getValue().equals(orderRecord.getStatus())) {
            orderRecord.setStatus(OrderStatus.CANCELLED.getValue());
            orderRecord.setCancelReason(request == null ? null : request.getReason());
            orderRecordMapper.updateById(orderRecord);
            safetyService.closeOrderSafetyResources(orderRecord.getId(), "order_cancelled");

            DriverTrip trip = driverTripMapper.selectByIdForUpdate(orderRecord.getDriverTripId());
            if (trip != null) {
                trip.setAvailableSeats(Objects.requireNonNullElse(trip.getAvailableSeats(), 0)
                        + Objects.requireNonNullElse(orderRecord.getPassengerCount(), 0));
                if (DriverTripStatus.FULL.getValue().equals(trip.getStatus())) {
                    trip.setStatus(DriverTripStatus.PUBLISHED.getValue());
                }
                driverTripMapper.updateById(trip);
            }
        }

        MatchRequest expired = new MatchRequest();
        expired.setStatus(MatchRequestStatus.EXPIRED.getValue());
        UpdateWrapper<MatchRequest> expireWrapper = new UpdateWrapper<>();
        expireWrapper.eq("passenger_order_id", passengerOrder.getId())
                .eq("status", MatchRequestStatus.PENDING.getValue());
        matchRequestMapper.update(expired, expireWrapper);

        passengerOrder.setStatus(PassengerOrderStatus.CANCELLED.getValue());
        passengerOrderMapper.updateById(passengerOrder);
        User passenger = userMapper.selectById(passengerOrder.getPassengerId());
        return toVO(passengerOrder, passenger, orderRecord, loadDriverFor(orderRecord));
    }

    @Override
    public List<RecommendedTripVO> recommend(String passengerOrderId, String passengerId) {
        PassengerOrder order = passengerOrderMapper.selectById(passengerOrderId);
        if (order == null) {
            throw new BusinessException(1404, "需求不存在：" + passengerOrderId);
        }
        requireOwner(order, passengerId);

        QueryWrapper<DriverTrip> wrapper = new QueryWrapper<>();
        wrapper.eq("status", DriverTripStatus.PUBLISHED.getValue())
                .ge("available_seats", order.getPassengerCount())
                .between("departure_time", order.getDepartureTime().minusHours(2),
                        order.getDepartureTime().plusHours(2));
        List<DriverTrip> trips = driverTripMapper.selectList(wrapper);

        List<RecommendedTripVO> recommended = new ArrayList<>();
        User passenger = userMapper.selectById(order.getPassengerId());
        for (DriverTrip trip : trips) {
            User driver = userMapper.selectById(trip.getDriverId());
            MatchEvaluation evaluation = matchingEngine.evaluate(order, trip, passenger, driver);
            if (!evaluation.eligible()) continue;
            recommended.add(buildRecommendedVO(trip, driver, evaluation));
        }
        recommended.sort(Comparator.comparingInt(
                (RecommendedTripVO value) -> Objects.requireNonNullElse(value.getScore(), 0)).reversed());
        return recommended;
    }

    private RecommendedTripVO buildRecommendedVO(
            DriverTrip trip, User driver, MatchEvaluation evaluation) {
        RecommendedTripVO vo = new RecommendedTripVO();
        vo.setDriverTripId(trip.getId());
        vo.setDriverId(trip.getDriverId());
        vo.setDriverName(Masking.name(driver.getName()));
        vo.setStart(trip.getStartLocation());
        vo.setEnd(trip.getEndLocation());
        vo.setRoute(trip.getStartLocation() + " -> " + trip.getEndLocation());
        vo.setStartLng(GeoPrivacy.blur(trip.getStartLng()));
        vo.setStartLat(GeoPrivacy.blur(trip.getStartLat()));
        vo.setEndLng(GeoPrivacy.blur(trip.getEndLng()));
        vo.setEndLat(GeoPrivacy.blur(trip.getEndLat()));
        vo.setDepartureTime(TimeFormatter.format(trip.getDepartureTime()));
        vo.setAvailableSeats(trip.getAvailableSeats());
        vo.setOverlap(evaluation.routeOverlap());
        vo.setDriverCredit(driver.getCredit());
        vo.setPrice(trip.getPrice());
        vo.setRecommendLevel(evaluation.comprehensiveScore() >= 80 ? "推荐" : "可选推荐");
        vo.setScore(evaluation.comprehensiveScore());
        vo.setTimeScore(evaluation.timeScore());
        vo.setReason(evaluation.reason());

        Vehicle vehicle = vehicleMapper.selectById(trip.getVehicleId());
        if (vehicle != null) {
            RecommendedTripVO.VehicleVO v = new RecommendedTripVO.VehicleVO();
            v.setPlate(Masking.plate(vehicle.getPlate()));
            v.setModel(vehicle.getModel());
            v.setColor(vehicle.getColor());
            vo.setVehicle(v);
        }
        return vo;
    }

    private PassengerOrder loadPassengerOrder(String id) {
        PassengerOrder order = passengerOrderMapper.selectById(id);
        if (order != null) return order;
        OrderRecord record = orderRecordMapper.selectById(id);
        if (record != null) {
            order = passengerOrderMapper.selectById(record.getPassengerOrderId());
            if (order != null) return order;
        }
        throw new BusinessException(1404, "需求或订单不存在：" + id);
    }

    private void requireOwner(PassengerOrder order, String passengerId) {
        if (passengerId != null && !passengerId.equals(order.getPassengerId())) {
            throw new BusinessException(1004, "不能操作其他乘客的订单");
        }
    }

    private OrderRecord findLatestOrderByPassengerOrder(String passengerOrderId) {
        QueryWrapper<OrderRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("passenger_order_id", passengerOrderId).orderByDesc("created_at").last("limit 1");
        return orderRecordMapper.selectOne(wrapper);
    }

    private User loadDriverFor(OrderRecord record) {
        if (record == null || record.getDriverId() == null) return null;
        return userMapper.selectById(record.getDriverId());
    }

    private PassengerOrderVO toVO(PassengerOrder order, User passenger, OrderRecord record, User driver) {
        PassengerOrderVO vo = new PassengerOrderVO();
        vo.setId(record == null ? order.getId() : record.getId());
        vo.setPassengerOrderId(order.getId());
        vo.setOrderId(record == null ? null : record.getId());
        vo.setPassengerId(order.getPassengerId());
        vo.setPassengerName(passenger == null ? null : passenger.getName());
        vo.setStart(order.getStartLocation());
        vo.setEnd(order.getEndLocation());
        vo.setRoute(order.getStartLocation() + " -> " + order.getEndLocation());
        vo.setStartLng(order.getStartLng());
        vo.setStartLat(order.getStartLat());
        vo.setEndLng(order.getEndLng());
        vo.setEndLat(order.getEndLat());
        vo.setDepartureTime(TimeFormatter.format(order.getDepartureTime()));
        vo.setPassengerCount(order.getPassengerCount());
        vo.setNote(order.getNote());
        vo.setStatus(record == null ? order.getStatus() : record.getStatus());
        vo.setCreatedAt(TimeFormatter.format(order.getCreatedAt()));

        if (record != null) {
            vo.setDriverId(record.getDriverId());
            vo.setPrice(record.getPrice());
            vo.setPaymentStatus(record.getPaymentStatus());
            vo.setReviewStatus(record.getReviewStatus());
            vo.setCancelReason(record.getCancelReason());
            if (driver != null) {
                vo.setDriverName(Masking.name(driver.getName()));
                vo.setDriverPhone(Masking.phone(sensitiveDataCrypto.decrypt(driver.getPhone())));
                Vehicle vehicle = findVehicleByTrip(record.getDriverTripId());
                if (vehicle != null) {
                    vo.setVehicle(Masking.plate(vehicle.getPlate()) + " · " + vehicle.getModel());
                }
            }
            vo.setSummary(buildSummary(record, driver));
        }
        return vo;
    }

    private Vehicle findVehicleByTrip(String driverTripId) {
        if (driverTripId == null) return null;
        DriverTrip trip = driverTripMapper.selectById(driverTripId);
        return trip == null ? null : vehicleMapper.selectById(trip.getVehicleId());
    }

    private String buildSummary(OrderRecord record, User driver) {
        StringBuilder sb = new StringBuilder();
        sb.append(record.getStartLocation()).append(" 到 ").append(record.getEndLocation());
        if (driver != null) sb.append("，").append(Masking.name(driver.getName()));
        sb.append("，").append(record.getPassengerCount()).append(" 人同行");
        return sb.toString();
    }
}
