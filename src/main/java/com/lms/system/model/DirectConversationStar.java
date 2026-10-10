package com.lms.system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "direct_conversation_stars",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_direct_conversation_stars_user_conversation",
                columnNames = {"user_id", "conversation_id"}
        )
)
public class DirectConversationStar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private DirectConversation conversation;

    @Column(name = "starred_at", nullable = false, updatable = false, columnDefinition = "datetime")
    private LocalDateTime starredAt;

    protected DirectConversationStar() {
    }

    public DirectConversationStar(User user, DirectConversation conversation) {
        this.user = user;
        this.conversation = conversation;
    }

    @PrePersist
    void initializeTimestamp() {
        starredAt = LocalDateTime.now();
    }

    public DirectConversation getConversation() {
        return conversation;
    }
}
