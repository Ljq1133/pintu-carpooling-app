package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.ChatMessageRequest;
import com.pintu.dto.ChatMessageVO;
import com.pintu.dto.ConversationVO;
import com.pintu.dto.NegotiationProposalRequest;
import com.pintu.dto.NegotiationProposalVO;
import com.pintu.security.CurrentUser;
import com.pintu.service.NegotiationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class NegotiationController {
    private final NegotiationService service;

    @GetMapping("/{id}")
    public Result<ConversationVO> get(@PathVariable String id, Authentication authentication) {
        return Result.ok(service.getConversation(id, CurrentUser.id(authentication)));
    }

    @GetMapping("/{id}/messages")
    public Result<List<ChatMessageVO>> messages(
            @PathVariable String id, Authentication authentication) {
        return Result.ok(service.listMessages(id, CurrentUser.id(authentication)));
    }

    @PostMapping("/{id}/messages")
    public Result<ChatMessageVO> send(
            @PathVariable String id,
            @RequestBody @Valid ChatMessageRequest request,
            Authentication authentication) {
        return Result.ok(service.sendMessage(id, CurrentUser.id(authentication), request));
    }

    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable String id, Authentication authentication) {
        service.markRead(id, CurrentUser.id(authentication));
        return Result.ok();
    }

    @GetMapping("/{id}/proposals")
    public Result<List<NegotiationProposalVO>> proposals(
            @PathVariable String id, Authentication authentication) {
        return Result.ok(service.listProposals(id, CurrentUser.id(authentication)));
    }

    @PostMapping("/{id}/proposals")
    public Result<NegotiationProposalVO> propose(
            @PathVariable String id,
            @RequestBody @Valid NegotiationProposalRequest request,
            Authentication authentication) {
        return Result.ok(service.createProposal(id, CurrentUser.id(authentication), request));
    }

    @PostMapping("/{id}/proposals/{version}/confirm")
    public Result<NegotiationProposalVO> confirm(
            @PathVariable String id,
            @PathVariable Integer version,
            Authentication authentication) {
        return Result.ok(service.confirm(id, CurrentUser.id(authentication), version));
    }
}
