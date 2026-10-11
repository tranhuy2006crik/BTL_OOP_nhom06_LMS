package com.lms.system.repository;

import com.lms.system.model.DirectConversation;
import com.lms.system.model.DirectConversationStar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DirectConversationStarRepository extends JpaRepository<DirectConversationStar, Long> {

    boolean existsByUser_IdAndConversation_Id(Long userId, Long conversationId);

    void deleteByUser_IdAndConversation_Id(Long userId, Long conversationId);

    long countByUser_Id(Long userId);

    @Query("""
            SELECT star.conversation.id
            FROM DirectConversationStar star
            WHERE star.user.id = :userId
            """)
    List<Long> findConversationIdsByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"conversation.firstUser", "conversation.secondUser"})
    @Query("""
            SELECT star.conversation
            FROM DirectConversationStar star
            WHERE star.user.id = :userId
            ORDER BY star.conversation.updatedAt DESC
            """)
    Page<DirectConversation> findStarredConversations(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
