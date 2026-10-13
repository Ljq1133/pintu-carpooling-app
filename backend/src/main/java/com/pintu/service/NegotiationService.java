package com.pintu.service;

import com.pintu.dto.ChatMessageRequest;
import com.pintu.dto.ChatMessageVO;
import com.pintu.dto.ConversationVO;
import com.pintu.dto.NegotiationProposalRequest;
import com.pintu.dto.NegotiationProposalVO;

import java.util.List;

public interface NegotiationService {
    ConversationVO getConversation(String conversationId, String actorId);

    List<ChatMessageVO> listMessages(String conversationId, String actorId);

    ChatMessageVO sendMessage(String conversationId, String actorId, ChatMessageRequest request);

    void markRead(String conversationId, String actorId);

    List<NegotiationProposalVO> listProposals(String conversationId, String actorId);

    NegotiationProposalVO createProposal(
            String conversationId, String actorId, NegotiationProposalRequest request);

    NegotiationProposalVO confirm(String conversationId, String actorId, Integer version);
}
