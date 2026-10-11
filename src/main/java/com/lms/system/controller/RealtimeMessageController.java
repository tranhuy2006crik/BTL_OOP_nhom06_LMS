package com.lms.system.controller;

import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.MessageInboxService;
import com.lms.system.dto.RealtimeMessageEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class RealtimeMessageController {

    private final MessageInboxService messageInboxService;
    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeMessageController(
            MessageInboxService messageInboxService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messageInboxService = messageInboxService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/conversations/{conversationId}/messages")
    public void sendMessage(
            @DestinationVariable Long conversationId,
            @Valid @Payload RealtimeMessageRequest request,
            Principal principal
    ) {
        AuthenticatedUser user = authenticatedUser(principal);
        RealtimeMessageEvent message = messageInboxService.sendMessage(
                user.getId(),
                conversationId,
                request.body()
        );
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, message);
    }

    @MessageMapping("/conversations/{conversationId}/read")
    public void markRead(@DestinationVariable Long conversationId, Principal principal) {
        AuthenticatedUser user = authenticatedUser(principal);
        messageInboxService.markConversationRead(user.getId(), conversationId);
    }

    private static AuthenticatedUser authenticatedUser(Principal principal) {
        if (principal instanceof org.springframework.security.core.Authentication authentication
                && authentication.getPrincipal() instanceof AuthenticatedUser user
                && user.isEnabled()) {
            return user;
        }
        throw new AccessDeniedException("Authentication is required for messaging");
    }

    public record RealtimeMessageRequest(
            @NotBlank(message = "Message cannot be empty")
            @Size(max = 2000, message = "Message must be 2000 characters or fewer")
            String body
    ) {
    }
}
