package com.pintu.service;

import com.pintu.dto.CancelRequest;
import com.pintu.dto.PassengerOrderCreateRequest;
import com.pintu.dto.PassengerOrderVO;
import com.pintu.dto.RecommendedTripVO;

import java.util.List;

/**
 * 乘客拼车需求 + 推荐匹配服务。
 */
public interface PassengerOrderService {

    PassengerOrderVO create(PassengerOrderCreateRequest request);

    List<PassengerOrderVO> listByPassenger(String passengerId, String status);

    PassengerOrderVO detail(String orderOrPassengerOrderId, String passengerId);

    PassengerOrderVO cancel(String orderOrPassengerOrderId, CancelRequest request, String passengerId);

    List<RecommendedTripVO> recommend(String passengerOrderId, String passengerId);
}
