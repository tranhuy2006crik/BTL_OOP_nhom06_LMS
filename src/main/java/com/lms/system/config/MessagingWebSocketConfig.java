package com.lms.system.config;

import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.MessageInboxService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableWebSocketMessageBroker
public class MessagingWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Pattern CONVERSATION_TOPIC = Pattern.compile("^/topic/conversations/(\\d+)$");
    private static final Pattern SEND_MESSAGE = Pattern.compile("^/app/conversations/(\\d+)/messages$");
    private static final Pattern MARK_READ = Pattern.compile("^/app/conversations/(\\d+)/read$");

    private final MessageInboxService messageInboxService;

    public MessagingWebSocketConfig(MessageInboxService messageInboxService) {
        this.messageInboxService = messageInboxService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ConversationAccessInterceptor(messageInboxService));
    }

    static final class ConversationAccessInterceptor implements ChannelInterceptor {

        private final MessageInboxService messageInboxService;

        ConversationAccessInterceptor(MessageInboxService messageInboxService) {
            this.messageInboxService = messageInboxService;
        }

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null || accessor.getCommand() == null) {
                return message;
            }

            Principal principal = accessor.getUser();
            if (accessor.getCommand() == StompCommand.CONNECT) {
                authenticatedUserId(principal);
                return message;
            }
            if (accessor.getCommand() == StompCommand.DISCONNECT
                    || accessor.getCommand() == StompCommand.UNSUBSCRIBE) {
                return message;
            }

            Long userId = authenticatedUserId(principal);
            String destination = accessor.getDestination();
            Matcher matcher;
            if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
                matcher = destination == null ? null : CONVERSATION_TOPIC.matcher(destination);
                if (matcher == null || !matcher.matches()) {
                    throw new AccessDeniedException("Subscription destination is not permitted");
                }
                requireConversationAccess(userId, matcher.group(1));
                return message;
            }
            if (accessor.getCommand() == StompCommand.SEND) {
                matcher = destination == null ? null : SEND_MESSAGE.matcher(destination);
                if (matcher == null || !matcher.matches()) {
                    matcher = destination == null ? null : MARK_READ.matcher(destination);
                }
                if (matcher == null || !matcher.matches()) {
                    throw new AccessDeniedException("Message destination is not permitted");
                }
                requireConversationAccess(userId, matcher.group(1));
                return message;
            }
            throw new AccessDeniedException("STOMP command is not permitted");
        }

        private void requireConversationAccess(Long userId, String conversationId) {
            try {
                if (!messageInboxService.canAccessConversation(userId, Long.valueOf(conversationId))) {
                    throw new AccessDeniedException("Conversation access is not permitted");
                }
            } catch (NumberFormatException exception) {
                throw new AccessDeniedException("Conversation access is not permitted", exception);
            }
        }

        private static Long authenticatedUserId(Principal principal) {
            if (principal instanceof Authentication authentication
                    && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof AuthenticatedUser user
                    && user.isEnabled()) {
                return user.getId();
            }
            throw new AccessDeniedException("Authentication is required for messaging");
        }
    }
}
