package com.lms.system.service;

import com.lms.system.model.DirectConversation;
import com.lms.system.model.DirectConversationStar;
import com.lms.system.model.DirectMessage;
import com.lms.system.model.User;
import com.lms.system.repository.DirectConversationRepository;
import com.lms.system.repository.DirectConversationStarRepository;
import com.lms.system.repository.DirectMessageRepository;
import com.lms.system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageInboxServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final DirectConversationRepository conversationRepository = mock(DirectConversationRepository.class);
    private final DirectConversationStarRepository starRepository = mock(DirectConversationStarRepository.class);
    private final DirectMessageRepository messageRepository = mock(DirectMessageRepository.class);
    private final MessageInboxService service = new MessageInboxService(
            userRepository,
            conversationRepository,
            starRepository,
            messageRepository
    );
    private final User sender = mock(User.class);
    private final DirectConversation conversation = mock(DirectConversation.class);

    @BeforeEach
    void setUp() {
        when(sender.canLogin()).thenReturn(true);
        when(sender.getId()).thenReturn(1L);
        when(sender.getFullName()).thenReturn("Sender");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(conversation.hasParticipant(1L)).thenReturn(true);
        when(messageRepository.save(any(DirectMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void trimsAndPersistsMessageForConversationParticipant() {
        service.sendMessage(1L, 10L, "  Hello there  ");

        verify(messageRepository).save(any(DirectMessage.class));
        verify(conversation).recordMessage("Hello there");
    }

    @Test
    void preventsNonParticipantsFromSendingMessages() {
        when(conversation.hasParticipant(1L)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> service.sendMessage(1L, 10L, "Hello"));

        verify(messageRepository, never()).save(any(DirectMessage.class));
    }

    @Test
    void rejectsEmptyMessages() {
        assertThrows(IllegalArgumentException.class, () -> service.sendMessage(1L, 10L, "   "));

        verify(messageRepository, never()).save(any(DirectMessage.class));
    }

    @Test
    void rejectsMessagesOverTheDatabaseLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.sendMessage(1L, 10L, "x".repeat(2001))
        );

        verify(messageRepository, never()).save(any(DirectMessage.class));
    }

    @Test
    void starsConversationForCurrentUser() {
        when(starRepository.existsByUser_IdAndConversation_Id(1L, 10L)).thenReturn(false);

        service.setConversationStarred(1L, 10L, true);

        verify(starRepository).save(any(DirectConversationStar.class));
    }

    @Test
    void removesCurrentUsersStarFromConversation() {
        when(starRepository.existsByUser_IdAndConversation_Id(1L, 10L)).thenReturn(true);

        service.setConversationStarred(1L, 10L, false);

        verify(starRepository).deleteByUser_IdAndConversation_Id(1L, 10L);
    }

    @Test
    void preventsNonParticipantsFromChangingConversationStars() {
        when(conversation.hasParticipant(1L)).thenReturn(false);

        assertThrows(
                ResponseStatusException.class,
                () -> service.setConversationStarred(1L, 10L, true)
        );

        verify(starRepository, never()).save(any(DirectConversationStar.class));
    }
}
