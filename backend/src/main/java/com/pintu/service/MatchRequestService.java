package com.pintu.service;

import com.pintu.dto.MatchRequestCreateRequest;
import com.pintu.dto.MatchRequestVO;

import java.util.List;

/**
 * 匹配请求服务（覆盖创建、查询、同意、拒绝）。
 */
public interface MatchRequestService {

    MatchRequestVO create(MatchRequestCreateRequest request, String passengerId);

    MatchRequestVO createByDriver(MatchRequestCreateRequest request, String driverId);

    MatchRequestVO get(String requestId, String actorId);

    List<MatchRequestVO> listByDriverTrip(String driverTripId, String driverId);

    List<MatchRequestVO> listByPassengerOrder(String passengerOrderId, String passengerId);

    /**
     * 司机同意请求：事务性地创建正式订单、扣减座位、回写需求和请求状态。
     */
    MatchRequestVO accept(String requestId, String actorId);

    MatchRequestVO reject(String requestId, String reason, String actorId);

    MatchRequestVO withdraw(String requestId, String actorId);
}
