package com.pintu.service;

import com.pintu.dto.AdminOrderVO;
import com.pintu.dto.AdminOrderQuery;
import com.pintu.dto.AdminOrderSummaryVO;
import com.pintu.dto.DriverOrderVO;
import com.pintu.dto.PaymentRequest;
import com.pintu.dto.PaymentVO;
import com.pintu.dto.ReviewRequest;
import com.pintu.dto.ReviewVO;
import com.pintu.entity.OrderRecord;

import java.util.List;

/**
 * 正式订单服务：覆盖履约状态机、付款、评价。
 */
public interface OrderService {

    /**
     * 司机点击「开始行程」：已确认 -> 履约中。
     */
    OrderRecord startTrip(String orderId, String driverId);

    /**
     * 司机点击「确认到达」：履约中 -> 待付款。
     */
    OrderRecord arrive(String orderId, String driverId);

    /**
     * 乘客付款：仅待付款 -> 已完成。
     */
    PaymentVO pay(String orderId, PaymentRequest request, String passengerId);

    /**
     * 乘客提交评价：仅在 已完成 之后允许。
     */
    ReviewVO review(String orderId, ReviewRequest request, String passengerId);

    OrderRecord get(String orderId);

    List<DriverOrderVO> listByDriver(String driverId);

    List<AdminOrderVO> listForAdmin(AdminOrderQuery query);

    AdminOrderSummaryVO summarizeForAdmin(AdminOrderQuery query);
}
