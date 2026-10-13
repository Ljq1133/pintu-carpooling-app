package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.TimeFormatter;
import com.pintu.dto.DriverTripCreateRequest;
import com.pintu.dto.DriverTripVO;
import com.pintu.dto.GeoPointVO;
import com.pintu.dto.PriceEstimateRequest;
import com.pintu.dto.RecommendedPassengerVO;
import com.pintu.dto.VehicleVO;
import com.pintu.dto.VehicleSaveRequest;
import com.pintu.entity.AuditRecord;
import com.pintu.entity.VehicleAttachment;
import com.pintu.enums.AuditStatus;
import com.pintu.entity.DriverTrip;
import com.pintu.entity.User;
import com.pintu.entity.Vehicle;
import com.pintu.entity.PassengerOrder;
import com.pintu.enums.DriverTripStatus;
import com.pintu.mapper.DriverTripMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.VehicleMapper;
import com.pintu.mapper.VehicleAttachmentMapper;
import com.pintu.mapper.AuditRecordMapper;
import com.pintu.mapper.PassengerOrderMapper;
import com.pintu.service.DriverService;
import com.pintu.service.MapService;
import com.pintu.service.AddressService;
import com.pintu.service.matching.MatchEvaluation;
import com.pintu.service.matching.MatchingEngine;
import com.pintu.common.util.GeoPrivacy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverTripMapper driverTripMapper;
    private final VehicleMapper vehicleMapper;
    private final UserMapper userMapper;
    private final VehicleAttachmentMapper vehicleAttachmentMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final MapService mapService;
    private final AddressService addressService;
    private final PassengerOrderMapper passengerOrderMapper;
    private final MatchingEngine matchingEngine;

    @Override
    public List<VehicleVO> listVehicles(String driverId) {
        QueryWrapper<Vehicle> wrapper = new QueryWrapper<>();
        wrapper.eq("driver_id", driverId).orderByAsc("created_at");
        List<Vehicle> vehicles = vehicleMapper.selectList(wrapper);
        List<VehicleVO> result = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            VehicleVO vo = new VehicleVO();
            BeanUtils.copyProperties(vehicle, vo);
            result.add(vo);
        }
        return result;
    }

    @Override
    public VehicleVO getVehicle(String driverId, String vehicleId) {
        return toVehicleVO(requireOwnedVehicle(driverId, vehicleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehicleVO createVehicle(String driverId, VehicleSaveRequest request) {
        ensurePlateAvailable(request.getPlate(), null);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(IdGenerator.next("veh"));
        vehicle.setDriverId(driverId);
        applyVehicle(vehicle, request);
        vehicle.setStatus("待审核");
        vehicleMapper.insert(vehicle);
        replaceLicense(vehicle.getId(), request.getLicenseKey());
        createVehicleAudit(vehicle);
        return toVehicleVO(vehicle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehicleVO updateVehicle(String driverId, String vehicleId, VehicleSaveRequest request) {
        Vehicle vehicle = requireOwnedVehicle(driverId, vehicleId);
        ensurePlateAvailable(request.getPlate(), vehicleId);
        applyVehicle(vehicle, request);
        vehicle.setStatus("待审核");
        vehicleMapper.updateById(vehicle);
        replaceLicense(vehicle.getId(), request.getLicenseKey());
        createVehicleAudit(vehicle);
        return toVehicleVO(vehicle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVehicle(String driverId, String vehicleId) {
        requireOwnedVehicle(driverId, vehicleId);
        Long usages = driverTripMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DriverTrip>()
                .eq(DriverTrip::getVehicleId, vehicleId));
        if (usages > 0) {
            throw new BusinessException(1023, "Vehicle is referenced by a trip and cannot be deleted");
        }
        vehicleMapper.deleteById(vehicleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DriverTripVO createTrip(DriverTripCreateRequest request) {
        String driverId = request.getDriverId() == null ? "driver-001" : request.getDriverId();
        User driver = userMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException(1404, "司机不存在：" + driverId);
        }
        if (driver.getCredit() == null || driver.getCredit() < 60
                || "限制接单".equals(driver.getStatus()) || "已封禁".equals(driver.getStatus())) {
            throw new BusinessException(1010, "司机当前不可发布行程");
        }
        Vehicle vehicle = vehicleMapper.selectById(request.getVehicleId());
        if (vehicle == null) {
            throw new BusinessException(1404, "车辆不存在：" + request.getVehicleId());
        }
        if (!"已认证".equals(vehicle.getStatus())) {
            throw new BusinessException(1017, "车辆未通过审核：" + vehicle.getStatus());
        }
        if (!driverId.equals(vehicle.getDriverId())) {
            throw new BusinessException(1004, "不能使用其他司机的车辆发布行程");
        }
        if (vehicle.getSeats() == null || request.getAvailableSeats() > vehicle.getSeats()) {
            throw new BusinessException(1002, "可拼座位不能超过车辆座位数");
        }
        if (request.getDepartureTime() == null
                || request.getDepartureTime().isBefore(java.time.LocalDateTime.now().minusSeconds(90))) {
            throw new BusinessException(1002, "出发时间不能早于当前时间");
        }
        populateCoordinates(request);

        DriverTrip trip = new DriverTrip();
        trip.setId(IdGenerator.next(IdGenerator.PREFIX_DRIVER_TRIP));
        trip.setDriverId(driverId);
        trip.setVehicleId(vehicle.getId());
        trip.setStartLocation(request.getStart());
        trip.setEndLocation(request.getEnd());
        trip.setStartLng(request.getStartLng());
        trip.setStartLat(request.getStartLat());
        trip.setEndLng(request.getEndLng());
        trip.setEndLat(request.getEndLat());
        trip.setDepartureTime(request.getDepartureTime());
        trip.setAvailableSeats(request.getAvailableSeats());
        trip.setPrice(request.getPrice() == null ? estimatePrice(request) : request.getPrice());
        trip.setOverlap(86);
        trip.setStatus(DriverTripStatus.PUBLISHED.getValue());
        driverTripMapper.insert(trip);
        addressService.recordHistory(driverId, request.getStart(), request.getStart(),
                request.getStartLng(), request.getStartLat());
        addressService.recordHistory(driverId, request.getEnd(), request.getEnd(),
                request.getEndLng(), request.getEndLat());
        return toVO(trip, driver, vehicle);
    }

    private void populateCoordinates(DriverTripCreateRequest request) {
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
    }

    private BigDecimal estimatePrice(DriverTripCreateRequest request) {
        PriceEstimateRequest estimate = new PriceEstimateRequest();
        estimate.setStartLng(request.getStartLng());
        estimate.setStartLat(request.getStartLat());
        estimate.setEndLng(request.getEndLng());
        estimate.setEndLat(request.getEndLat());
        estimate.setPassengerCount(1);
        return mapService.estimate(estimate).getTotal();
    }

    @Override
    public List<DriverTripVO> listTrips(String driverId) {
        QueryWrapper<DriverTrip> wrapper = new QueryWrapper<>();
        wrapper.eq("driver_id", driverId).orderByDesc("created_at");
        List<DriverTrip> trips = driverTripMapper.selectList(wrapper);
        User driver = userMapper.selectById(driverId);
        List<DriverTripVO> result = new ArrayList<>();
        for (DriverTrip trip : trips) {
            Vehicle vehicle = vehicleMapper.selectById(trip.getVehicleId());
            result.add(toVO(trip, driver, vehicle));
        }
        return result;
    }

    @Override
    public List<RecommendedPassengerVO> recommendPassengers(String driverId, String tripId) {
        DriverTrip trip = driverTripMapper.selectById(tripId);
        if (trip == null) throw new BusinessException(1404, "司机行程不存在：" + tripId);
        if (!driverId.equals(trip.getDriverId())) {
            throw new BusinessException(1004, "不能查看其他司机的候选乘客");
        }
        User driver = userMapper.selectById(driverId);
        List<PassengerOrder> orders = passengerOrderMapper.selectList(
                new QueryWrapper<PassengerOrder>()
                        .in("status", "待匹配", "协商中")
                        .le("passenger_count", trip.getAvailableSeats())
                        .between("departure_time", trip.getDepartureTime().minusHours(2),
                                trip.getDepartureTime().plusHours(2)));
        List<RecommendedPassengerVO> result = new ArrayList<>();
        for (PassengerOrder order : orders) {
            User passenger = userMapper.selectById(order.getPassengerId());
            MatchEvaluation evaluation = matchingEngine.evaluate(order, trip, passenger, driver);
            if (!evaluation.eligible()) continue;
            RecommendedPassengerVO vo = new RecommendedPassengerVO();
            vo.setPassengerOrderId(order.getId());
            vo.setPassengerId(order.getPassengerId());
            vo.setPassengerName(passenger == null ? null : com.pintu.common.util.Masking.name(passenger.getName()));
            vo.setStart(order.getStartLocation());
            vo.setEnd(order.getEndLocation());
            vo.setStartLng(GeoPrivacy.blur(order.getStartLng()));
            vo.setStartLat(GeoPrivacy.blur(order.getStartLat()));
            vo.setEndLng(GeoPrivacy.blur(order.getEndLng()));
            vo.setEndLat(GeoPrivacy.blur(order.getEndLat()));
            vo.setDepartureTime(TimeFormatter.format(order.getDepartureTime()));
            vo.setPassengerCount(order.getPassengerCount());
            vo.setPassengerCredit(passenger == null ? null : passenger.getCredit());
            vo.setOverlap(evaluation.routeOverlap());
            vo.setScore(evaluation.comprehensiveScore());
            vo.setReason(evaluation.reason());
            result.add(vo);
        }
        result.sort(java.util.Comparator.comparingInt(RecommendedPassengerVO::getScore).reversed());
        return result;
    }

    private DriverTripVO toVO(DriverTrip trip, User driver, Vehicle vehicle) {
        DriverTripVO vo = new DriverTripVO();
        vo.setId(trip.getId());
        vo.setDriverId(trip.getDriverId());
        vo.setDriverName(driver == null ? null : driver.getName());
        vo.setDriverCredit(driver == null ? null : driver.getCredit());
        vo.setVehicleId(trip.getVehicleId());
        vo.setVehicle(vehicle == null ? null : vehicle.getPlate() + " · " + vehicle.getModel());
        vo.setStart(trip.getStartLocation());
        vo.setEnd(trip.getEndLocation());
        vo.setRoute(trip.getStartLocation() + " -> " + trip.getEndLocation());
        vo.setStartLng(trip.getStartLng());
        vo.setStartLat(trip.getStartLat());
        vo.setEndLng(trip.getEndLng());
        vo.setEndLat(trip.getEndLat());
        vo.setDepartureTime(TimeFormatter.format(trip.getDepartureTime()));
        vo.setAvailableSeats(trip.getAvailableSeats());
        vo.setPrice(trip.getPrice());
        vo.setOverlap(trip.getOverlap());
        vo.setStatus(trip.getStatus());
        return vo;
    }

    private Vehicle requireOwnedVehicle(String driverId, String vehicleId) {
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException(1404, "Vehicle does not exist");
        }
        if (!driverId.equals(vehicle.getDriverId())) {
            throw new BusinessException(1004, "Vehicle does not belong to current driver");
        }
        return vehicle;
    }

    private void ensurePlateAvailable(String plate, String excludedVehicleId) {
        QueryWrapper<Vehicle> wrapper = new QueryWrapper<>();
        wrapper.eq("plate", plate);
        if (excludedVehicleId != null) {
            wrapper.ne("id", excludedVehicleId);
        }
        if (vehicleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(1024, "Plate number is already registered");
        }
    }

    private void applyVehicle(Vehicle vehicle, VehicleSaveRequest request) {
        vehicle.setPlate(request.getPlate().trim().toUpperCase());
        vehicle.setBrand(request.getBrand().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setColor(request.getColor().trim());
        vehicle.setSeats(request.getSeats());
        vehicle.setVehicleType(request.getVehicleType().trim());
    }

    private void replaceLicense(String vehicleId, String storageKey) {
        vehicleAttachmentMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VehicleAttachment>()
                .eq(VehicleAttachment::getVehicleId, vehicleId)
                .eq(VehicleAttachment::getAttachmentType, "DRIVING_LICENSE"));
        VehicleAttachment attachment = new VehicleAttachment();
        attachment.setId(IdGenerator.next("va"));
        attachment.setVehicleId(vehicleId);
        attachment.setAttachmentType("DRIVING_LICENSE");
        attachment.setStorageKey(storageKey);
        vehicleAttachmentMapper.insert(attachment);
    }

    private void createVehicleAudit(Vehicle vehicle) {
        AuditRecord audit = new AuditRecord();
        audit.setId(IdGenerator.next("audit"));
        audit.setTargetType("vehicle");
        audit.setTargetId(vehicle.getId());
        audit.setName(vehicle.getBrand() + " " + vehicle.getModel());
        audit.setPlate(vehicle.getPlate());
        audit.setStatus(AuditStatus.PENDING.getValue());
        audit.setSubmittedAt(TimeFormatter.nowDateTime());
        auditRecordMapper.insert(audit);
    }

    private VehicleVO toVehicleVO(Vehicle vehicle) {
        VehicleVO vo = new VehicleVO();
        BeanUtils.copyProperties(vehicle, vo);
        return vo;
    }
}
