package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.TimeFormatter;
import com.pintu.common.util.Masking;
import com.pintu.dto.AdminOrderVO;
import com.pintu.dto.AdminOrderQuery;
import com.pintu.dto.AdminOrderSummaryVO;
import com.pintu.dto.DriverOrderVO;
import com.pintu.dto.PaymentRequest;
import com.pintu.dto.PaymentVO;
import com.pintu.dto.ReviewRequest;
import com.pintu.dto.ReviewVO;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.PassengerOrder;
import com.pintu.entity.Payment;
import com.pintu.entity.Review;
import com.pintu.entity.User;
import com.pintu.entity.Vehicle;
import com.pintu.entity.DriverTrip;
import com.pintu.enums.OrderStatus;
import com.pintu.enums.PaymentStatus;
import com.pintu.enums.ReviewStatus;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.PassengerOrderMapper;
import com.pintu.mapper.PaymentMapper;
import com.pintu.mapper.ReviewMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.VehicleMapper;
import com.pintu.mapper.DriverTripMapper;
import com.pintu.service.OrderService;
import com.pintu.service.SafetyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRecordMapper orderRecordMapper;
    private final PassengerOrderMapper passengerOrderMapper;
    private final PaymentMapper paymentMapper;
    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;
    private final VehicleMapper vehicleMapper;
    private final DriverTripMapper driverTripMapper;
    private final SafetyService safetyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderRecord startTrip(String orderId, String driverId) {
        OrderRecord order = requireForUpdate(orderId);
        requireDriver(order, driverId);
        if (!OrderStatus.CONFIRMED.getValue().equals(order.getStatus())) {
            throw new BusinessException(1011, "仅已确认订单可开始行程，当前状态：" + order.getStatus());
        }
        order.setStatus(OrderStatus.ONGOING.getValue());
        order.setActualDeparture(TimeFormatter.nowDateTime());
        orderRecordMapper.updateById(order);
        syncPassengerOrderStatus(order, OrderStatus.ONGOING.getValue());
        syncDriverTripStatus(order, OrderStatus.ONGOING.getValue());
        safetyService.ensureVirtualPhoneBound(order.getId(), driverId);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderRecord arrive(String orderId, String driverId) {
        OrderRecord order = requireForUpdate(orderId);
        requireDriver(order, driverId);
        if (!OrderStatus.ONGOING.getValue().equals(order.getStatus())) {
            throw new BusinessException(1012, "仅履约中订单可确认到达，当前状态：" + order.getStatus());
        }
        order.setStatus(OrderStatus.PENDING_PAYMENT.getValue());
        order.setActualArrival(TimeFormatter.nowDateTime());
        orderRecordMapper.updateById(order);
        syncPassengerOrderStatus(order, OrderStatus.PENDING_PAYMENT.getValue());
        syncDriverTripStatus(order, OrderStatus.COMPLETED.getValue());
        safetyService.closeOrderSafetyResources(order.getId(), "trip_arrived");
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO pay(String orderId, PaymentRequest request, String passengerId) {
        OrderRecord order = requireForUpdate(orderId);
        requirePassenger(order, passengerId);
        if (PaymentStatus.PAID.getValue().equals(order.getPaymentStatus())) {
            throw new BusinessException(1013, "订单已支付，不可重复付款");
        }
        if (!OrderStatus.PENDING_PAYMENT.getValue().equals(order.getStatus())) {
            throw new BusinessException(1014, "订单状态不允许付款：" + order.getStatus());
        }
        if (order.getPrice() == null || request.getAmount().compareTo(order.getPrice()) != 0) {
            throw new BusinessException(1014, "付款金额与订单金额不一致");
        }

        Payment payment = new Payment();
        payment.setId(IdGenerator.next(IdGenerator.PREFIX_PAYMENT));
        payment.setOrderId(order.getId());
        payment.setAmount(request.getAmount());
        payment.setMethod(request.getMethod() == null ? "mock_balance" : request.getMethod());
        payment.setStatus(PaymentStatus.PAID.getValue());
        payment.setPaidAt(TimeFormatter.nowDateTime());
        paymentMapper.insert(payment);

        order.setStatus(OrderStatus.COMPLETED.getValue());
        order.setPaymentStatus(PaymentStatus.PAID.getValue());
        orderRecordMapper.updateById(order);
        syncPassengerOrderStatus(order, OrderStatus.COMPLETED.getValue());
        safetyService.closeOrderSafetyResources(order.getId(), "order_paid");

        // 完成数 +1，节省 / 减碳 简单按订单金额比例累加
        User passenger = userMapper.selectById(order.getPassengerId());
        if (passenger != null) {
            passenger.setTripCount(passenger.getTripCount() == null ? 1 : passenger.getTripCount() + 1);
            userMapper.updateById(passenger);
        }
        User driver = userMapper.selectById(order.getDriverId());
        if (driver != null) {
            driver.setTripCount(driver.getTripCount() == null ? 1 : driver.getTripCount() + 1);
            userMapper.updateById(driver);
        }

        PaymentVO vo = new PaymentVO();
        vo.setId(payment.getId());
        vo.setOrderId(order.getId());
        vo.setAmount(payment.getAmount());
        vo.setStatus(PaymentStatus.PAID.getValue());
        vo.setOrderStatus(OrderStatus.COMPLETED.getValue());
        vo.setPaymentStatus(PaymentStatus.PAID.getValue());
        vo.setReviewStatus(order.getReviewStatus());
        vo.setPaidAt(TimeFormatter.format(payment.getPaidAt()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO review(String orderId, ReviewRequest request, String passengerId) {
        OrderRecord order = requireForUpdate(orderId);
        requirePassenger(order, passengerId);
        if (!PaymentStatus.PAID.getValue().equals(order.getPaymentStatus())) {
            throw new BusinessException(1015, "未支付订单不能评价");
        }
        if (ReviewStatus.REVIEWED.getValue().equals(order.getReviewStatus())) {
            throw new BusinessException(1016, "订单已评价，不可重复");
        }

        Review review = new Review();
        review.setId(IdGenerator.next(IdGenerator.PREFIX_REVIEW));
        review.setOrderId(order.getId());
        review.setPassengerId(order.getPassengerId());
        review.setDriverId(order.getDriverId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        reviewMapper.insert(review);

        order.setReviewStatus(ReviewStatus.REVIEWED.getValue());
        orderRecordMapper.updateById(order);

        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(order.getId());
        vo.setRating(review.getRating());
        vo.setComment(review.getComment());
        vo.setStatus(ReviewStatus.REVIEWED.getValue());
        vo.setOrderStatus(order.getStatus());
        return vo;
    }

    @Override
    public OrderRecord get(String orderId) {
        return require(orderId);
    }

    @Override
    public List<DriverOrderVO> listByDriver(String driverId) {
        QueryWrapper<OrderRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("driver_id", driverId).orderByDesc("created_at");
        List<OrderRecord> records = orderRecordMapper.selectList(wrapper);
        List<DriverOrderVO> result = new ArrayList<>();
        for (OrderRecord record : records) {
            DriverOrderVO vo = new DriverOrderVO();
            vo.setId(record.getId());
            vo.setPassengerOrderId(record.getPassengerOrderId());
            vo.setDriverTripId(record.getDriverTripId());
            vo.setPassengerId(record.getPassengerId());
            User passenger = userMapper.selectById(record.getPassengerId());
            vo.setPassengerName(passenger == null ? null : Masking.name(passenger.getName()));
            vo.setStart(record.getStartLocation());
            vo.setEnd(record.getEndLocation());
            vo.setRoute(record.getStartLocation() + " -> " + record.getEndLocation());
            vo.setStartLng(record.getStartLng());
            vo.setStartLat(record.getStartLat());
            vo.setEndLng(record.getEndLng());
            vo.setEndLat(record.getEndLat());
            vo.setDepartureTime(TimeFormatter.format(record.getDepartureTime()));
            vo.setActualDeparture(TimeFormatter.format(record.getActualDeparture()));
            vo.setActualArrival(TimeFormatter.format(record.getActualArrival()));
            vo.setPassengerCount(record.getPassengerCount());
            vo.setPrice(record.getPrice());
            vo.setStatus(record.getStatus());
            vo.setPaymentStatus(record.getPaymentStatus());
            vo.setReviewStatus(record.getReviewStatus());
            vo.setCancelReason(record.getCancelReason());

            DriverTrip trip = driverTripMapper.selectById(record.getDriverTripId());
            Vehicle vehicle = trip == null ? null : vehicleMapper.selectById(trip.getVehicleId());
            if (vehicle != null) {
                vo.setVehicle(Masking.plate(vehicle.getPlate()) + " · " + vehicle.getModel());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<AdminOrderVO> listForAdmin(AdminOrderQuery query) {
        QueryWrapper<OrderRecord> wrapper = buildAdminOrderWrapper(query);
        wrapper.orderByDesc("created_at");
        List<OrderRecord> records = orderRecordMapper.selectList(wrapper);
        List<AdminOrderVO> result = new ArrayList<>();
        for (OrderRecord record : records) {
            result.add(toAdminOrderVO(record));
        }
        return result;
    }

    @Override
    public AdminOrderSummaryVO summarizeForAdmin(AdminOrderQuery query) {
        List<OrderRecord> records = orderRecordMapper.selectList(buildAdminOrderWrapper(query));
        AdminOrderSummaryVO summary = new AdminOrderSummaryVO();
        summary.setTotal(records.size());
        for (OrderRecord record : records) {
            addCount(summary.getStatusCounts(), record.getStatus());
            addCount(summary.getPaymentStatusCounts(), record.getPaymentStatus());
            if (OrderStatus.PENDING_PAYMENT.getValue().equals(record.getStatus())) {
                summary.setPendingPayment(summary.getPendingPayment() + 1);
            }
            if (OrderStatus.COMPLETED.getValue().equals(record.getStatus())) {
                summary.setCompleted(summary.getCompleted() + 1);
            }
            if (OrderStatus.CANCELLED.getValue().equals(record.getStatus())) {
                summary.setCancelled(summary.getCancelled() + 1);
            }
        }
        return summary;
    }

    private QueryWrapper<OrderRecord> buildAdminOrderWrapper(AdminOrderQuery query) {
        AdminOrderQuery safeQuery = query == null ? new AdminOrderQuery() : query;
        QueryWrapper<OrderRecord> wrapper = new QueryWrapper<>();
        if (safeQuery.getStatus() != null && !safeQuery.getStatus().isBlank()) {
            wrapper.eq("status", safeQuery.getStatus());
        }
        if (safeQuery.getPaymentStatus() != null && !safeQuery.getPaymentStatus().isBlank()) {
            wrapper.eq("payment_status", safeQuery.getPaymentStatus());
        }
        if (safeQuery.getKeyword() != null && !safeQuery.getKeyword().isBlank()) {
            String keyword = safeQuery.getKeyword().trim();
            wrapper.and(w -> w.like("id", keyword)
                    .or().like("start_location", keyword)
                    .or().like("end_location", keyword)
                    .or().like("passenger_id", keyword)
                    .or().like("driver_id", keyword));
        }
        LocalDateTime dateFrom = parseAdminDateTime(safeQuery.getDateFrom(), false);
        LocalDateTime dateTo = parseAdminDateTime(safeQuery.getDateTo(), true);
        if (dateFrom != null) {
            wrapper.ge("created_at", dateFrom);
        }
        if (dateTo != null) {
            wrapper.le("created_at", dateTo);
        }
        return wrapper;
    }

    private LocalDateTime parseAdminDateTime(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            if (trimmed.length() == 10) {
                return LocalDate.parse(trimmed).atTime(endOfDay ? LocalTime.of(23, 59, 59) : LocalTime.MIN);
            }
            return LocalDateTime.parse(trimmed, TimeFormatter.PATTERN);
        } catch (RuntimeException error) {
            throw new BusinessException(1002, "日期格式应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm");
        }
    }

    private void addCount(java.util.Map<String, Integer> counts, String key) {
        String safeKey = key == null || key.isBlank() ? "未知" : key;
        counts.put(safeKey, counts.getOrDefault(safeKey, 0) + 1);
    }

    private AdminOrderVO toAdminOrderVO(OrderRecord record) {
        AdminOrderVO vo = new AdminOrderVO();
        vo.setId(record.getId());
        vo.setRoute(record.getStartLocation() + " -> " + record.getEndLocation());
        User passenger = userMapper.selectById(record.getPassengerId());
        User driver = userMapper.selectById(record.getDriverId());
        vo.setPassenger(passenger == null ? null : Masking.name(passenger.getName()));
        vo.setDriver(driver == null ? null : Masking.name(driver.getName()));
        vo.setStatus(record.getStatus());
        vo.setPaymentStatus(record.getPaymentStatus());
        vo.setPrice(record.getPrice());
        vo.setRisk(OrderStatus.PENDING_PAYMENT.getValue().equals(record.getStatus()) ? "待支付" : "正常");
        vo.setCreatedAt(TimeFormatter.format(record.getCreatedAt()));
        return vo;
    }

    private OrderRecord require(String orderId) {
        OrderRecord order = orderRecordMapper.selectById(orderId);
        if (order == null) throw new BusinessException(1404, "订单不存在：" + orderId);
        return order;
    }

    private OrderRecord requireForUpdate(String orderId) {
        OrderRecord order = orderRecordMapper.selectByIdForUpdate(orderId);
        if (order == null) throw new BusinessException(1404, "订单不存在：" + orderId);
        return order;
    }

    private void requireDriver(OrderRecord order, String driverId) {
        if (driverId != null && !driverId.equals(order.getDriverId())) {
            throw new BusinessException(1004, "不能操作其他司机的订单");
        }
    }

    private void requirePassenger(OrderRecord order, String passengerId) {
        if (passengerId != null && !passengerId.equals(order.getPassengerId())) {
            throw new BusinessException(1004, "不能操作其他乘客的订单");
        }
    }

    private void syncPassengerOrderStatus(OrderRecord record, String status) {
        PassengerOrder po = passengerOrderMapper.selectById(record.getPassengerOrderId());
        if (po != null) {
            po.setStatus(status);
            passengerOrderMapper.updateById(po);
        }
    }

    private void syncDriverTripStatus(OrderRecord record, String status) {
        DriverTrip trip = driverTripMapper.selectByIdForUpdate(record.getDriverTripId());
        if (trip != null) {
            trip.setStatus(status);
            driverTripMapper.updateById(trip);
        }
    }
}
