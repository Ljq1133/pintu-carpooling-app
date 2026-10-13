package com.pintu.service;

import com.pintu.dto.DriverTripCreateRequest;
import com.pintu.dto.DriverTripVO;
import com.pintu.dto.VehicleVO;
import com.pintu.dto.VehicleSaveRequest;
import com.pintu.dto.RecommendedPassengerVO;

import java.util.List;

/**
 * 司机端服务：车辆 + 行程。
 */
public interface DriverService {

    List<VehicleVO> listVehicles(String driverId);

    VehicleVO getVehicle(String driverId, String vehicleId);

    VehicleVO createVehicle(String driverId, VehicleSaveRequest request);

    VehicleVO updateVehicle(String driverId, String vehicleId, VehicleSaveRequest request);

    void deleteVehicle(String driverId, String vehicleId);

    DriverTripVO createTrip(DriverTripCreateRequest request);

    List<DriverTripVO> listTrips(String driverId);

    List<RecommendedPassengerVO> recommendPassengers(String driverId, String tripId);
}
