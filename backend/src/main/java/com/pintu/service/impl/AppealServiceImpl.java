package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.Masking;
import com.pintu.common.util.TimeFormatter;
import com.pintu.dto.AppealCreateRequest;
import com.pintu.dto.AppealEvidenceVO;
import com.pintu.dto.AppealTicketVO;
import com.pintu.dto.AppealTransitionRequest;
import com.pintu.dto.AppealTransitionVO;
import com.pintu.dto.ArbitrationRequest;
import com.pintu.dto.ArbitrationVO;
import com.pintu.entity.AppealEvidence;
import com.pintu.entity.AppealTicket;
import com.pintu.entity.AppealTransition;
import com.pintu.entity.ArbitrationRecord;
import com.pintu.entity.CreditRecord;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.User;
import com.pintu.enums.AdminPermission;
import com.pintu.enums.OrderStatus;
import com.pintu.enums.PaymentStatus;
import com.pintu.mapper.AppealEvidenceMapper;
import com.pintu.mapper.AppealTicketMapper;
import com.pintu.mapper.AppealTransitionMapper;
import com.pintu.mapper.ArbitrationRecordMapper;
import com.pintu.mapper.CreditRecordMapper;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.service.AdminService;
import com.pintu.service.AppealService;
import com.pintu.service.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppealServiceImpl implements AppealService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_CLAIMED = "CLAIMED";
    private static final String STATUS_CLOSED = "CLOSED";

    private final AppealTicketMapper appealTicketMapper;
    private final AppealTransitionMapper appealTransitionMapper;
    private final AppealEvidenceMapper appealEvidenceMapper;
    private final ArbitrationRecordMapper arbitrationRecordMapper;
    private final OrderRecordMapper orderRecordMapper;
    private final UserMapper userMapper;
    private final CreditRecordMapper creditRecordMapper;
    private final AdminService adminService;
    private final SecurityAuditService securityAuditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppealTicketVO createPassengerAppeal(String passengerId, String orderId, AppealCreateRequest request) {
        OrderRecord order = requireOrder(orderId);
        if (passengerId != null && !passengerId.equals(order.getPassengerId())) {
            throw new BusinessException(1004, "Only the order passenger can create an appeal");
        }

        AppealTicket ticket = new AppealTicket();
        ticket.setId(IdGenerator.next("appeal"));
        ticket.setOrderId(orderId);
        ticket.setApplicantId(passengerId);
        ticket.setReasonType(request.getReasonType().trim());
        ticket.setDescription(normalize(request.getDescription()));
        ticket.setStatus(STATUS_OPEN);
        ticket.setCreatedAt(TimeFormatter.nowDateTime());
        ticket.setUpdatedAt(TimeFormatter.nowDateTime());
        appealTicketMapper.insert(ticket);

        insertTransition(ticket.getId(), passengerId, null, STATUS_OPEN, "created");
        if (request.getEvidenceKeys() != null) {
            for (String key : request.getEvidenceKeys()) {
                if (key == null || key.isBlank()) continue;
                AppealEvidence evidence = new AppealEvidence();
                evidence.setId(IdGenerator.next("ae"));
                evidence.setAppealId(ticket.getId());
                evidence.setUploaderId(passengerId);
                evidence.setStorageKey(key.trim());
                evidence.setContentType(resolveContentType(key));
                evidence.setCreatedAt(TimeFormatter.nowDateTime());
                appealEvidenceMapper.insert(evidence);
            }
        }
        securityAuditService.record(passengerId, "APPEAL_CREATE", orderId, null, true, ticket.getId());
        return toVO(ticket);
    }

    @Override
    public List<AppealTicketVO> listPassengerAppeals(String passengerId) {
        return appealTicketMapper.selectList(new QueryWrapper<AppealTicket>()
                        .eq("applicant_id", passengerId)
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public AppealTicketVO getPassengerAppeal(String passengerId, String appealId) {
        AppealTicket ticket = requireAppeal(appealId);
        if (passengerId != null && !passengerId.equals(ticket.getApplicantId())) {
            throw new BusinessException(1004, "Only the applicant can view this appeal");
        }
        return toVO(ticket);
    }

    @Override
    public List<AppealTicketVO> listAdminAppeals(String adminId, String status) {
        adminService.requirePermission(adminId, AdminPermission.APPEAL_MANAGE.getCode());
        QueryWrapper<AppealTicket> wrapper = new QueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status.trim());
        }
        wrapper.orderByDesc("created_at");
        return appealTicketMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public AppealTicketVO getAdminAppeal(String adminId, String appealId) {
        adminService.requirePermission(adminId, AdminPermission.APPEAL_MANAGE.getCode());
        return toVO(requireAppeal(appealId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppealTicketVO transitionByAdmin(String adminId, String appealId, AppealTransitionRequest request) {
        adminService.requirePermission(adminId, AdminPermission.APPEAL_MANAGE.getCode());
        AppealTicket ticket = requireAppeal(appealId);
        String fromStatus = ticket.getStatus();
        String nextStatus = normalizeStatus(request.getStatus());
        if (STATUS_CLOSED.equals(fromStatus)) {
            throw new BusinessException(1006, "Closed appeals cannot transition");
        }

        if (request.getAssigneeId() != null && !request.getAssigneeId().isBlank()) {
            ticket.setAssigneeId(request.getAssigneeId().trim());
        } else if (STATUS_CLAIMED.equals(nextStatus) && ticket.getAssigneeId() == null) {
            ticket.setAssigneeId(adminId);
        }
        ticket.setStatus(nextStatus);
        ticket.setUpdatedAt(TimeFormatter.nowDateTime());
        if (STATUS_CLOSED.equals(nextStatus)) {
            ticket.setClosedAt(TimeFormatter.nowDateTime());
            ticket.setResolution(normalize(request.getComment()));
        }
        appealTicketMapper.updateById(ticket);
        insertTransition(ticket.getId(), adminId, fromStatus, nextStatus, normalize(request.getComment()));
        securityAuditService.record(adminId, "APPEAL_TRANSITION", appealId, null, true,
                fromStatus + "->" + nextStatus);
        return toVO(ticket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArbitrationVO arbitrate(String adminId, String appealId, ArbitrationRequest request) {
        adminService.requirePermission(adminId, AdminPermission.ARBITRATION_MANAGE.getCode());
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            throw new BusinessException(1002, "Arbitration requires second confirmation");
        }
        AppealTicket ticket = requireAppeal(appealId);
        OrderRecord order = requireOrder(ticket.getOrderId());

        ArbitrationRecord record = new ArbitrationRecord();
        record.setId(IdGenerator.next("arb"));
        record.setAppealId(appealId);
        record.setAdminId(adminId);
        record.setActionType(request.getActionType().trim().toUpperCase());
        record.setAmount(request.getAmount());
        record.setCreditDelta(request.getCreditDelta());
        record.setRemark(normalize(request.getRemark()));
        record.setConfirmedBy(adminId);
        record.setCreatedAt(TimeFormatter.nowDateTime());
        arbitrationRecordMapper.insert(record);

        applyArbitration(ticket, order, request, record.getActionType(), adminId);
        securityAuditService.record(adminId, "APPEAL_ARBITRATION", appealId, null, true, record.getActionType());
        return toArbitrationVO(record);
    }

    private void applyArbitration(
            AppealTicket ticket,
            OrderRecord order,
            ArbitrationRequest request,
            String actionType,
            String adminId) {
        boolean orderChanged = false;
        if ("REFUND".equals(actionType)) {
            order.setPaymentStatus(PaymentStatus.REFUNDED.getValue());
            orderChanged = true;
        }
        if ("FORCE_SETTLEMENT".equals(actionType)) {
            order.setStatus(OrderStatus.COMPLETED.getValue());
            order.setPaymentStatus(PaymentStatus.PAID.getValue());
            orderChanged = true;
        }
        if ("ORDER_NOTE".equals(actionType)) {
            order.setAdminNote(normalize(request.getRemark()));
            orderChanged = true;
        }
        if (orderChanged) {
            orderRecordMapper.updateById(order);
        }
        if ("CREDIT_CORRECTION".equals(actionType) && request.getCreditDelta() != null) {
            applyCreditCorrection(
                    request.getTargetUserId() == null || request.getTargetUserId().isBlank()
                            ? ticket.getApplicantId()
                            : request.getTargetUserId().trim(),
                    request.getCreditDelta(),
                    adminId,
                    ticket.getId());
        }
    }

    private void applyCreditCorrection(String userId, Integer delta, String adminId, String appealId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(1404, "User does not exist: " + userId);
        }
        int next = Math.max(0, Math.min(100, (user.getCredit() == null ? 100 : user.getCredit()) + delta));
        user.setCredit(next);
        userMapper.updateById(user);

        CreditRecord credit = new CreditRecord();
        credit.setId(IdGenerator.next(IdGenerator.PREFIX_CREDIT));
        credit.setUserId(userId);
        credit.setDelta(delta);
        credit.setReason("appeal:" + appealId);
        credit.setOperatorId(adminId);
        credit.setCreatedAt(TimeFormatter.nowDateTime());
        creditRecordMapper.insert(credit);
    }

    private OrderRecord requireOrder(String orderId) {
        OrderRecord order = orderRecordMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(1404, "Order does not exist: " + orderId);
        }
        return order;
    }

    private AppealTicket requireAppeal(String appealId) {
        AppealTicket ticket = appealTicketMapper.selectById(appealId);
        if (ticket == null) {
            throw new BusinessException(1404, "Appeal does not exist: " + appealId);
        }
        return ticket;
    }

    private void insertTransition(String appealId, String operatorId, String fromStatus, String toStatus, String comment) {
        AppealTransition transition = new AppealTransition();
        transition.setId(IdGenerator.next("at"));
        transition.setAppealId(appealId);
        transition.setOperatorId(operatorId);
        transition.setFromStatus(fromStatus);
        transition.setToStatus(toStatus);
        transition.setComment(comment);
        transition.setCreatedAt(TimeFormatter.nowDateTime());
        appealTransitionMapper.insert(transition);
    }

    private AppealTicketVO toVO(AppealTicket ticket) {
        AppealTicketVO vo = new AppealTicketVO();
        vo.setId(ticket.getId());
        vo.setOrderId(ticket.getOrderId());
        vo.setApplicantId(ticket.getApplicantId());
        vo.setAssigneeId(ticket.getAssigneeId());
        vo.setReasonType(ticket.getReasonType());
        vo.setDescription(ticket.getDescription());
        vo.setStatus(ticket.getStatus());
        vo.setResolution(ticket.getResolution());
        vo.setCreatedAt(TimeFormatter.format(ticket.getCreatedAt()));
        vo.setUpdatedAt(TimeFormatter.format(ticket.getUpdatedAt()));
        vo.setClosedAt(TimeFormatter.format(ticket.getClosedAt()));
        OrderRecord order = orderRecordMapper.selectById(ticket.getOrderId());
        if (order != null) {
            vo.setRoute(order.getStartLocation() + " -> " + order.getEndLocation());
        }
        User applicant = userMapper.selectById(ticket.getApplicantId());
        if (applicant != null) {
            vo.setApplicantName(Masking.name(applicant.getName()));
        }
        vo.setEvidences(appealEvidenceMapper.selectList(new QueryWrapper<AppealEvidence>()
                        .eq("appeal_id", ticket.getId())
                        .orderByAsc("created_at"))
                .stream()
                .map(this::toEvidenceVO)
                .toList());
        vo.setTransitions(appealTransitionMapper.selectList(new QueryWrapper<AppealTransition>()
                        .eq("appeal_id", ticket.getId())
                        .orderByAsc("created_at"))
                .stream()
                .map(this::toTransitionVO)
                .toList());
        vo.setArbitrations(arbitrationRecordMapper.selectList(new QueryWrapper<ArbitrationRecord>()
                        .eq("appeal_id", ticket.getId())
                        .orderByAsc("created_at"))
                .stream()
                .map(this::toArbitrationVO)
                .toList());
        return vo;
    }

    private AppealEvidenceVO toEvidenceVO(AppealEvidence evidence) {
        AppealEvidenceVO vo = new AppealEvidenceVO();
        vo.setId(evidence.getId());
        vo.setStorageKey(evidence.getStorageKey());
        vo.setContentType(evidence.getContentType());
        vo.setCreatedAt(TimeFormatter.format(evidence.getCreatedAt()));
        return vo;
    }

    private AppealTransitionVO toTransitionVO(AppealTransition transition) {
        AppealTransitionVO vo = new AppealTransitionVO();
        vo.setId(transition.getId());
        vo.setOperatorId(transition.getOperatorId());
        vo.setFromStatus(transition.getFromStatus());
        vo.setToStatus(transition.getToStatus());
        vo.setComment(transition.getComment());
        vo.setCreatedAt(TimeFormatter.format(transition.getCreatedAt()));
        return vo;
    }

    private ArbitrationVO toArbitrationVO(ArbitrationRecord record) {
        ArbitrationVO vo = new ArbitrationVO();
        vo.setId(record.getId());
        vo.setAppealId(record.getAppealId());
        vo.setAdminId(record.getAdminId());
        vo.setActionType(record.getActionType());
        vo.setAmount(record.getAmount());
        vo.setCreditDelta(record.getCreditDelta());
        vo.setRemark(record.getRemark());
        vo.setConfirmedBy(record.getConfirmedBy());
        vo.setCreatedAt(TimeFormatter.format(record.getCreatedAt()));
        return vo;
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!List.of(STATUS_OPEN, STATUS_CLAIMED, "IN_PROGRESS", "WAITING_SUPPLEMENT", STATUS_CLOSED)
                .contains(normalized)) {
            throw new BusinessException(1002, "Unsupported appeal status: " + status);
        }
        return normalized;
    }

    private String resolveContentType(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
