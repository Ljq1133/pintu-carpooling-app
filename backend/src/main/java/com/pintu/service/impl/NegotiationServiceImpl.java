package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.common.util.TimeFormatter;
import com.pintu.dto.ChatMessageRequest;
import com.pintu.dto.ChatMessageVO;
import com.pintu.dto.ConversationVO;
import com.pintu.dto.NegotiationProposalRequest;
import com.pintu.dto.NegotiationProposalVO;
import com.pintu.entity.ChatMessage;
import com.pintu.entity.Conversation;
import com.pintu.entity.DriverTrip;
import com.pintu.entity.MatchRequest;
import com.pintu.entity.NegotiationProposal;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.OrderStatusHistory;
import com.pintu.entity.PassengerOrder;
import com.pintu.entity.ProposalConfirmation;
import com.pintu.enums.DriverTripStatus;
import com.pintu.enums.MatchRequestStatus;
import com.pintu.enums.OrderStatus;
import com.pintu.enums.PaymentStatus;
import com.pintu.enums.ReviewStatus;
import com.pintu.mapper.ChatMessageMapper;
import com.pintu.mapper.ConversationMapper;
import com.pintu.mapper.DriverTripMapper;
import com.pintu.mapper.MatchRequestMapper;
import com.pintu.mapper.NegotiationProposalMapper;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.OrderStatusHistoryMapper;
import com.pintu.mapper.PassengerOrderMapper;
import com.pintu.mapper.ProposalConfirmationMapper;
import com.pintu.service.NegotiationService;
import com.pintu.service.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NegotiationServiceImpl implements NegotiationService {
    private final ConversationMapper conversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final NegotiationProposalMapper proposalMapper;
    private final ProposalConfirmationMapper confirmationMapper;
    private final MatchRequestMapper matchRequestMapper;
    private final PassengerOrderMapper passengerOrderMapper;
    private final DriverTripMapper driverTripMapper;
    private final OrderRecordMapper orderRecordMapper;
    private final OrderStatusHistoryMapper historyMapper;
    private final SecurityAuditService auditService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public ConversationVO getConversation(String conversationId, String actorId) {
        Conversation conversation = requireParticipant(conversationId, actorId, false);
        return toConversationVO(conversation, actorId);
    }

    @Override
    public List<ChatMessageVO> listMessages(String conversationId, String actorId) {
        requireParticipant(conversationId, actorId, false);
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByAsc(ChatMessage::getCreatedAt))
                .stream().map(this::toMessageVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendMessage(String conversationId, String actorId, ChatMessageRequest request) {
        Conversation conversation = requireParticipant(conversationId, actorId, true);
        validateMessage(request);
        if (request.getClientMessageId() != null && !request.getClientMessageId().isBlank()) {
            ChatMessage existing = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getConversationId, conversationId)
                    .eq(ChatMessage::getClientMessageId, request.getClientMessageId())
                    .last("limit 1"));
            if (existing != null) return toMessageVO(existing);
        }
        ChatMessage message = new ChatMessage();
        message.setId(IdGenerator.next("msg"));
        message.setConversationId(conversationId);
        message.setSenderId(actorId);
        message.setMessageType(request.getMessageType().toLowerCase());
        message.setContent(request.getContent());
        message.setAttachmentKey(request.getAttachmentKey());
        message.setClientMessageId(request.getClientMessageId());
        chatMessageMapper.insert(message);
        auditService.record(actorId, "NEGOTIATION_MESSAGE", conversationId,
                null, true, "type=" + message.getMessageType());
        ChatMessageVO result = toMessageVO(message);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(String conversationId, String actorId) {
        requireParticipant(conversationId, actorId, false);
        chatMessageMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .ne(ChatMessage::getSenderId, actorId)
                .isNull(ChatMessage::getReadAt)
                .set(ChatMessage::getReadAt, TimeFormatter.nowDateTime()));
    }

    @Override
    public List<NegotiationProposalVO> listProposals(String conversationId, String actorId) {
        requireParticipant(conversationId, actorId, false);
        List<NegotiationProposal> proposals = proposalMapper.selectList(
                new LambdaQueryWrapper<NegotiationProposal>()
                        .eq(NegotiationProposal::getConversationId, conversationId)
                        .orderByDesc(NegotiationProposal::getVersion));
        return proposals.stream().map(value -> toProposalVO(value, null)).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NegotiationProposalVO createProposal(
            String conversationId, String actorId, NegotiationProposalRequest request) {
        Conversation conversation = requireParticipant(conversationId, actorId, true);
        NegotiationProposal latest = latestProposal(conversationId);
        NegotiationProposal proposal = new NegotiationProposal();
        proposal.setId(IdGenerator.next("proposal"));
        proposal.setConversationId(conversationId);
        proposal.setVersion(latest == null ? 1 : latest.getVersion() + 1);
        proposal.setProposerId(actorId);
        proposal.setPrice(request.getPrice());
        proposal.setDepartureTime(request.getDepartureTime());
        proposal.setPassengerCount(request.getPassengerCount());
        proposal.setPickupLocation(request.getPickupLocation());
        proposal.setPickupLng(request.getPickupLng());
        proposal.setPickupLat(request.getPickupLat());
        proposal.setNote(request.getNote());
        proposalMapper.insert(proposal);
        auditService.record(actorId, "NEGOTIATION_PROPOSAL", conversationId,
                null, true, "version=" + proposal.getVersion());
        NegotiationProposalVO result = toProposalVO(proposal, null);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversation.getId(), result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NegotiationProposalVO confirm(String conversationId, String actorId, Integer version) {
        Conversation conversation = requireParticipant(conversationId, actorId, true);
        NegotiationProposal proposal = latestProposal(conversationId);
        if (proposal == null || !proposal.getVersion().equals(version)) {
            throw new BusinessException(1031, "只能确认当前最新版本方案");
        }
        ProposalConfirmation existing = confirmationMapper.selectOne(
                new LambdaQueryWrapper<ProposalConfirmation>()
                        .eq(ProposalConfirmation::getProposalId, proposal.getId())
                        .eq(ProposalConfirmation::getUserId, actorId)
                        .last("limit 1"));
        if (existing == null) {
            ProposalConfirmation confirmation = new ProposalConfirmation();
            confirmation.setId(IdGenerator.next("confirm"));
            confirmation.setProposalId(proposal.getId());
            confirmation.setUserId(actorId);
            confirmation.setRole(actorId.equals(conversation.getPassengerId()) ? "PASSENGER" : "DRIVER");
            confirmation.setConfirmedAt(TimeFormatter.nowDateTime());
            confirmationMapper.insert(confirmation);
        }
        auditService.record(actorId, "NEGOTIATION_CONFIRM", conversationId,
                null, true, "version=" + version);

        long count = confirmationMapper.selectCount(new LambdaQueryWrapper<ProposalConfirmation>()
                .eq(ProposalConfirmation::getProposalId, proposal.getId()));
        String orderId = count >= 2 ? finalizeOrder(conversation, proposal, actorId) : null;
        NegotiationProposalVO result = toProposalVO(proposal, orderId);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, result);
        return result;
    }

    private String finalizeOrder(
            Conversation unlockedConversation, NegotiationProposal proposal, String actorId) {
        Conversation conversation = conversationMapper.selectByIdForUpdate(unlockedConversation.getId());
        if (conversation.getOrderId() != null) return conversation.getOrderId();
        MatchRequest request = matchRequestMapper.selectByIdForUpdate(conversation.getMatchRequestId());
        PassengerOrder orderDemand = passengerOrderMapper.selectByIdForUpdate(request.getPassengerOrderId());
        DriverTrip trip = driverTripMapper.selectByIdForUpdate(request.getDriverTripId());
        if (!MatchRequestStatus.ACCEPTED.getValue().equals(request.getStatus())) {
            throw new BusinessException(1032, "匹配请求已失效，不能生成订单");
        }
        if (trip.getAvailableSeats() == null || trip.getAvailableSeats() < proposal.getPassengerCount()) {
            throw new BusinessException(1007, "确认时剩余座位不足");
        }
        if (orderRecordMapper.selectLatestByPassengerOrderForUpdate(orderDemand.getId()) != null) {
            throw new BusinessException(1008, "该需求已经生成正式订单");
        }

        OrderRecord order = new OrderRecord();
        order.setId(IdGenerator.next(IdGenerator.PREFIX_ORDER));
        order.setPassengerOrderId(orderDemand.getId());
        order.setDriverTripId(trip.getId());
        order.setMatchRequestId(request.getId());
        order.setPassengerId(orderDemand.getPassengerId());
        order.setDriverId(trip.getDriverId());
        order.setStartLocation(proposal.getPickupLocation() == null
                ? orderDemand.getStartLocation() : proposal.getPickupLocation());
        order.setEndLocation(orderDemand.getEndLocation());
        order.setStartLng(proposal.getPickupLng() == null
                ? orderDemand.getStartLng() : proposal.getPickupLng());
        order.setStartLat(proposal.getPickupLat() == null
                ? orderDemand.getStartLat() : proposal.getPickupLat());
        order.setEndLng(orderDemand.getEndLng());
        order.setEndLat(orderDemand.getEndLat());
        order.setDepartureTime(proposal.getDepartureTime());
        order.setPassengerCount(proposal.getPassengerCount());
        order.setPrice(proposal.getPrice());
        order.setStatus(OrderStatus.CONFIRMED.getValue());
        order.setPaymentStatus(PaymentStatus.UNPAID.getValue());
        order.setReviewStatus(ReviewStatus.UNREVIEWED.getValue());
        orderRecordMapper.insert(order);

        trip.setAvailableSeats(trip.getAvailableSeats() - proposal.getPassengerCount());
        if (trip.getAvailableSeats() == 0) trip.setStatus(DriverTripStatus.FULL.getValue());
        driverTripMapper.updateById(trip);
        orderDemand.setStatus(OrderStatus.CONFIRMED.getValue());
        passengerOrderMapper.updateById(orderDemand);
        request.setOrderId(order.getId());
        matchRequestMapper.updateById(request);
        conversation.setOrderId(order.getId());
        conversation.setStatus("已确认");
        conversation.setClosedAt(TimeFormatter.nowDateTime());
        conversationMapper.updateById(conversation);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setId(IdGenerator.next("osh"));
        history.setOrderId(order.getId());
        history.setOperatorId(actorId);
        history.setOperatorRole(actorId.equals(conversation.getPassengerId()) ? "PASSENGER" : "DRIVER");
        history.setFromStatus("协商中");
        history.setToStatus(OrderStatus.CONFIRMED.getValue());
        history.setReason("双方确认协商方案 v" + proposal.getVersion());
        historyMapper.insert(history);
        return order.getId();
    }

    private Conversation requireParticipant(String id, String actorId, boolean requireOpen) {
        Conversation conversation = conversationMapper.selectById(id);
        if (conversation == null) throw new BusinessException(1404, "协商会话不存在：" + id);
        if (!actorId.equals(conversation.getPassengerId()) && !actorId.equals(conversation.getDriverId())) {
            throw new BusinessException(1004, "只有订单双方可以访问协商会话");
        }
        if (requireOpen && !"协商中".equals(conversation.getStatus())) {
            throw new BusinessException(1030, "协商会话已关闭");
        }
        return conversation;
    }

    private void validateMessage(ChatMessageRequest request) {
        String type = request.getMessageType().toLowerCase();
        if (!List.of("text", "image").contains(type)) {
            throw new BusinessException(1002, "消息类型只支持 text 或 image");
        }
        if ("text".equals(type) && (request.getContent() == null || request.getContent().isBlank())) {
            throw new BusinessException(1002, "文字消息不能为空");
        }
        if ("image".equals(type)
                && (request.getAttachmentKey() == null || request.getAttachmentKey().isBlank())) {
            throw new BusinessException(1002, "图片消息必须包含附件");
        }
    }

    private NegotiationProposal latestProposal(String conversationId) {
        return proposalMapper.selectOne(new LambdaQueryWrapper<NegotiationProposal>()
                .eq(NegotiationProposal::getConversationId, conversationId)
                .orderByDesc(NegotiationProposal::getVersion)
                .last("limit 1"));
    }

    private ConversationVO toConversationVO(Conversation value, String actorId) {
        ConversationVO vo = new ConversationVO();
        vo.setId(value.getId());
        vo.setOrderId(value.getOrderId());
        vo.setMatchRequestId(value.getMatchRequestId());
        vo.setPassengerId(value.getPassengerId());
        vo.setDriverId(value.getDriverId());
        vo.setStatus(value.getStatus());
        vo.setUnreadCount(chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, value.getId())
                .ne(ChatMessage::getSenderId, actorId)
                .isNull(ChatMessage::getReadAt)));
        vo.setCreatedAt(TimeFormatter.format(value.getCreatedAt()));
        vo.setClosedAt(TimeFormatter.format(value.getClosedAt()));
        return vo;
    }

    private ChatMessageVO toMessageVO(ChatMessage value) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(value.getId());
        vo.setConversationId(value.getConversationId());
        vo.setSenderId(value.getSenderId());
        vo.setMessageType(value.getMessageType());
        vo.setContent(value.getContent());
        vo.setAttachmentKey(value.getAttachmentKey());
        vo.setClientMessageId(value.getClientMessageId());
        vo.setReadAt(TimeFormatter.format(value.getReadAt()));
        vo.setCreatedAt(TimeFormatter.format(value.getCreatedAt()));
        return vo;
    }

    private NegotiationProposalVO toProposalVO(NegotiationProposal value, String orderId) {
        NegotiationProposalVO vo = new NegotiationProposalVO();
        vo.setId(value.getId());
        vo.setConversationId(value.getConversationId());
        vo.setVersion(value.getVersion());
        vo.setProposerId(value.getProposerId());
        vo.setPrice(value.getPrice());
        vo.setDepartureTime(TimeFormatter.format(value.getDepartureTime()));
        vo.setPassengerCount(value.getPassengerCount());
        vo.setPickupLocation(value.getPickupLocation());
        vo.setPickupLng(value.getPickupLng());
        vo.setPickupLat(value.getPickupLat());
        vo.setNote(value.getNote());
        vo.setCreatedAt(TimeFormatter.format(value.getCreatedAt()));
        vo.setOrderId(orderId);
        List<ProposalConfirmation> confirmations = confirmationMapper.selectList(
                new LambdaQueryWrapper<ProposalConfirmation>()
                        .eq(ProposalConfirmation::getProposalId, value.getId()));
        vo.setConfirmedRoles(confirmations.stream().map(ProposalConfirmation::getRole).toList());
        return vo;
    }
}
