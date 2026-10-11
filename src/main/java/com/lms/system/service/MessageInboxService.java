package com.lms.system.service;

import com.lms.system.dto.RealtimeMessageEvent;
import com.lms.system.model.DirectConversation;
import com.lms.system.model.DirectConversationStar;
import com.lms.system.model.DirectMessage;
import com.lms.system.model.User;
import com.lms.system.model.UserStatus;
import com.lms.system.repository.DirectConversationRepository;
import com.lms.system.repository.DirectConversationStarRepository;
import com.lms.system.repository.DirectMessageRepository;
import com.lms.system.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MessageInboxService {

    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_CONVERSATIONS = 50;
    private static final int MAX_CONTACTS = 30;
    private static final int MAX_HISTORY = 100;

    private final UserRepository userRepository;
    private final DirectConversationRepository conversationRepository;
    private final DirectConversationStarRepository starRepository;
    private final DirectMessageRepository messageRepository;

    public MessageInboxService(
            UserRepository userRepository,
            DirectConversationRepository conversationRepository,
            DirectConversationStarRepository starRepository,
            DirectMessageRepository messageRepository
    ) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.starRepository = starRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> conversations(Long currentUserId, String query) {
        requireActiveUser(currentUserId);
        var page = conversationRepository.findByFirstUser_IdOrSecondUser_Id(
                currentUserId,
                currentUserId,
                PageRequest.of(0, MAX_CONVERSATIONS, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        Map<Long, Long> unreadCounts = new HashMap<>();
        for (DirectMessageRepository.UnreadCount unreadCount : messageRepository.findUnreadCounts(currentUserId)) {
            unreadCounts.put(unreadCount.getConversationId(), unreadCount.getUnreadCount());
        }
        var starredConversationIds = java.util.Set.copyOf(starRepository.findConversationIdsByUserId(currentUserId));
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return page.getContent().stream()
                .map(conversation -> toSummary(
                        conversation,
                        currentUserId,
                        unreadCounts,
                        starredConversationIds.contains(conversation.getId())
                ))
                .filter(summary -> normalizedQuery.isEmpty()
                        || summary.contactName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || (summary.lastMessage() != null
                        && summary.lastMessage().toLowerCase(Locale.ROOT).contains(normalizedQuery)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> starredConversations(Long currentUserId, String query) {
        requireActiveUser(currentUserId);
        Map<Long, Long> unreadCounts = new HashMap<>();
        for (DirectMessageRepository.UnreadCount unreadCount : messageRepository.findUnreadCounts(currentUserId)) {
            unreadCounts.put(unreadCount.getConversationId(), unreadCount.getUnreadCount());
        }
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return starRepository.findStarredConversations(
                        currentUserId,
                        PageRequest.of(0, MAX_CONVERSATIONS)
                )
                .getContent().stream()
                .map(conversation -> toSummary(conversation, currentUserId, unreadCounts, true))
                .filter(summary -> normalizedQuery.isEmpty()
                        || summary.contactName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || (summary.lastMessage() != null
                        && summary.lastMessage().toLowerCase(Locale.ROOT).contains(normalizedQuery)))
                .toList();
    }

    @Transactional(readOnly = true)
    public long starredCount(Long currentUserId) {
        requireActiveUser(currentUserId);
        return starRepository.countByUser_Id(currentUserId);
    }

    @Transactional(readOnly = true)
    public List<Contact> contacts(Long currentUserId, String query) {
        requireActiveUser(currentUserId);
        String normalizedQuery = query == null ? "" : query.trim();
        return userRepository.searchMessageContacts(
                        currentUserId,
                        UserStatus.ACTIVE,
                        normalizedQuery,
                        PageRequest.of(0, MAX_CONTACTS)
                )
                .map(user -> new Contact(user.getId(), user.getFullName()))
                .getContent();
    }

    @Transactional
    public Long startConversation(Long currentUserId, Long recipientId) {
        User currentUser = requireActiveUser(currentUserId);
        User recipient = requireActiveUser(recipientId);
        if (currentUserId.equals(recipientId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot start a conversation with yourself");
        }

        Long firstUserId = Math.min(currentUserId, recipientId);
        Long secondUserId = Math.max(currentUserId, recipientId);
        return conversationRepository.findByFirstUser_IdAndSecondUser_Id(firstUserId, secondUserId)
                .map(DirectConversation::getId)
                .orElseGet(() -> conversationRepository.save(
                        DirectConversation.between(currentUser, recipient)
                ).getId());
    }

    @Transactional
    public ConversationDetails openConversation(Long currentUserId, Long conversationId) {
        requireActiveUser(currentUserId);
        DirectConversation conversation = requireConversation(conversationId);
        requireParticipant(conversation, currentUserId);
        messageRepository.markIncomingMessagesRead(conversationId, currentUserId, LocalDateTime.now());

        User contact = conversation.otherParticipant(currentUserId);
        List<DirectMessage> history = new ArrayList<>(messageRepository
                .findByConversation_IdOrderBySentAtDescIdDesc(
                        conversationId,
                        PageRequest.of(0, MAX_HISTORY)
                ));
        java.util.Collections.reverse(history);
        List<MessageView> messages = history.stream()
                .map(message -> new MessageView(
                        message.getId(),
                        message.getSender().getId(),
                        message.getSender().getFullName(),
                        message.getBody(),
                        message.getSentAt(),
                        message.getSender().getId().equals(currentUserId)
                ))
                .toList();
        return new ConversationDetails(
                conversation.getId(),
                contact.getId(),
                contact.getFullName(),
                starRepository.existsByUser_IdAndConversation_Id(currentUserId, conversationId),
                messages
        );
    }

    @Transactional
    public void setConversationStarred(Long currentUserId, Long conversationId, boolean starred) {
        User user = requireActiveUser(currentUserId);
        DirectConversation conversation = requireConversation(conversationId);
        requireParticipant(conversation, currentUserId);
        boolean alreadyStarred = starRepository.existsByUser_IdAndConversation_Id(currentUserId, conversationId);
        if (starred && !alreadyStarred) {
            starRepository.save(new DirectConversationStar(user, conversation));
        } else if (!starred && alreadyStarred) {
            starRepository.deleteByUser_IdAndConversation_Id(currentUserId, conversationId);
        }
    }

    @Transactional
    public RealtimeMessageEvent sendMessage(Long currentUserId, Long conversationId, String body) {
        User sender = requireActiveUser(currentUserId);
        DirectConversation conversation = requireConversation(conversationId);
        requireParticipant(conversation, currentUserId);
        String normalizedBody = body == null ? "" : body.trim();
        if (normalizedBody.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (normalizedBody.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message must be 2000 characters or fewer");
        }
        DirectMessage message = messageRepository.save(new DirectMessage(conversation, sender, normalizedBody));
        conversation.recordMessage(normalizedBody);
        return new RealtimeMessageEvent(
                message.getId(),
                sender.getId(),
                sender.getFullName(),
                message.getBody(),
                message.getSentAt()
        );
    }

    @Transactional(readOnly = true)
    public boolean canAccessConversation(Long userId, Long conversationId) {
        return userRepository.findById(userId)
                .filter(User::canLogin)
                .isPresent()
                && conversationRepository.findById(conversationId)
                .filter(conversation -> conversation.hasParticipant(userId))
                .isPresent();
    }

    @Transactional
    public void markConversationRead(Long userId, Long conversationId) {
        requireActiveUser(userId);
        DirectConversation conversation = requireConversation(conversationId);
        requireParticipant(conversation, userId);
        messageRepository.markIncomingMessagesRead(conversationId, userId, LocalDateTime.now());
    }

    private User requireActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::canLogin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active user not found"));
    }

    private DirectConversation requireConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private static void requireParticipant(DirectConversation conversation, Long userId) {
        if (!conversation.hasParticipant(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
        }
    }

    private static ConversationSummary toSummary(
            DirectConversation conversation,
            Long currentUserId,
            Map<Long, Long> unreadCounts,
            boolean starred
    ) {
        User contact = conversation.otherParticipant(currentUserId);
        return new ConversationSummary(
                conversation.getId(),
                contact.getId(),
                contact.getFullName(),
                conversation.getLastMessage(),
                conversation.getUpdatedAt(),
                unreadCounts.getOrDefault(conversation.getId(), 0L),
                starred
        );
    }

    public record ConversationSummary(
            Long id,
            Long contactId,
            String contactName,
            String lastMessage,
            LocalDateTime updatedAt,
            long unreadCount,
            boolean starred
    ) {
    }

    public record ConversationDetails(
            Long id,
            Long contactId,
            String contactName,
            boolean starred,
            List<MessageView> messages
    ) {
        public ConversationDetails {
            messages = List.copyOf(messages);
        }
    }

    public record MessageView(
            Long id,
            Long senderId,
            String senderName,
            String body,
            LocalDateTime sentAt,
            boolean mine
    ) {
    }

    public record Contact(Long id, String fullName) {
    }
}
