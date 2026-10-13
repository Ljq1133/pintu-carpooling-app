package com.pintu.service;

import com.pintu.dto.AppealCreateRequest;
import com.pintu.dto.AppealTicketVO;
import com.pintu.dto.AppealTransitionRequest;
import com.pintu.dto.ArbitrationRequest;
import com.pintu.dto.ArbitrationVO;

import java.util.List;

public interface AppealService {

    AppealTicketVO createPassengerAppeal(String passengerId, String orderId, AppealCreateRequest request);

    List<AppealTicketVO> listPassengerAppeals(String passengerId);

    AppealTicketVO getPassengerAppeal(String passengerId, String appealId);

    List<AppealTicketVO> listAdminAppeals(String adminId, String status);

    AppealTicketVO getAdminAppeal(String adminId, String appealId);

    AppealTicketVO transitionByAdmin(String adminId, String appealId, AppealTransitionRequest request);

    ArbitrationVO arbitrate(String adminId, String appealId, ArbitrationRequest request);
}
