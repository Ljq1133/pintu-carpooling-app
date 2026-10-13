package com.pintu.service;

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

import java.util.List;

public interface SafetyService {

    List<EmergencyContactVO> listEmergencyContacts(String userId);

    EmergencyContactVO createEmergencyContact(String userId, EmergencyContactSaveRequest request);

    EmergencyContactVO updateEmergencyContact(String userId, String contactId, EmergencyContactSaveRequest request);

    void deleteEmergencyContact(String userId, String contactId);

    SafetyAlertVO createOrderAlert(String userId, String orderId, SafetyAlertCreateRequest request);

    VirtualPhoneBindingVO ensureVirtualPhoneBound(String orderId, String userId);

    VirtualPhoneBindingVO getVirtualPhone(String orderId, String userId);

    void unbindOrderVirtualPhone(String orderId, String reason);

    void closeOrderSafetyResources(String orderId, String reason);

    TripShareLinkVO createShareLink(String userId, String orderId, TripShareCreateRequest request);

    TripShareViewVO getShareView(String token);

    LocationPointVO reportDriverLocation(String driverId, String orderId, LocationReportRequest request);

    List<LocationPointVO> reportDriverLocationBatch(String driverId, String orderId, LocationBatchReportRequest request);

    List<LocationPointVO> listTrajectory(String userId, String orderId);

    List<DataRetentionPolicyVO> listRetentionPolicies();
}
