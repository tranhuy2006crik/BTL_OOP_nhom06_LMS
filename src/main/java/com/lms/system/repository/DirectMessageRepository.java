package com.lms.system.repository;

import com.lms.system.model.DirectMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    @EntityGraph(attributePaths = "sender")
    List<DirectMessage> findByConversation_IdOrderBySentAtDescIdDesc(Long conversationId, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE DirectMessage m
            SET m.readAt = :readAt
            WHERE m.conversation.id = :conversationId
              AND m.sender.id <> :userId
              AND m.readAt IS NULL
            """)
    int markIncomingMessagesRead(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("readAt") LocalDateTime readAt
    );

    @Query("""
            SELECT m.conversation.id AS conversationId, COUNT(m.id) AS unreadCount
            FROM DirectMessage m
            WHERE m.readAt IS NULL
              AND m.sender.id <> :userId
              AND (m.conversation.firstUser.id = :userId OR m.conversation.secondUser.id = :userId)
            GROUP BY m.conversation.id
            """)
    List<UnreadCount> findUnreadCounts(@Param("userId") Long userId);

    interface UnreadCount {
        Long getConversationId();

        long getUnreadCount();
    }
}
