package com.lms.system.config;

import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.MessageInboxService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessagingWebSocketConfigTest {

    private final MessageInboxService messageInboxService = mock(MessageInboxService.class);
    private final MessagingWebSocketConfig.ConversationAccessInterceptor interceptor =
            new MessagingWebSocketConfig.ConversationAccessInterceptor(messageInboxService);
    private final AuthenticatedUser user = new AuthenticatedUser(
            7L,
            "member@example.com",
            "Member",
            "hash",
            true,
            List.of()
    );
    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, "credentials", user.getAuthorities());

    @Test
    void permitsParticipantToSubscribeToTheirConversation() {
        Message<byte[]> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/topic/conversations/15",
                authentication
        );
        when(messageInboxService.canAccessConversation(7L, 15L)).thenReturn(true);

        assertSame(message, interceptor.preSend(message, null));

        verify(messageInboxService).canAccessConversation(7L, 15L);
    }

    @Test
    void deniesNonParticipantSubscription() {
        Message<byte[]> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/topic/conversations/15",
                authentication
        );
        when(messageInboxService.canAccessConversation(7L, 15L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void deniesPublishingDirectlyToBrokerTopic() {
        Message<byte[]> message = stompMessage(
                StompCommand.SEND,
                "/topic/conversations/15",
                authentication
        );

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void deniesUnauthenticatedConnection() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null, null);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, null));
    }

    private static Message<byte[]> stompMessage(
            StompCommand command,
            String destination,
            UsernamePasswordAuthenticationToken authentication
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setUser(authentication);
        accessor.setDestination(destination);
        if (command == StompCommand.SUBSCRIBE) {
            accessor.setSubscriptionId("test-subscription");
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
