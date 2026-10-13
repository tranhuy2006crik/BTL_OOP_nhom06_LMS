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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "direct_conversations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_direct_conversations_participants",
                columnNames = {"first_user_id", "second_user_id"}
        )
)
public class DirectConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "first_user_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User firstUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "second_user_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User secondUser;

    @Column(name = "last_message", length = 2000)
    private String lastMessage;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime updatedAt;

    protected DirectConversation() {
    }

    public static DirectConversation between(User first, User second) {
        if (first == null || second == null || first.getId() == null || second.getId() == null
                || first.getId().equals(second.getId())) {
            throw new IllegalArgumentException("Two different persisted users are required");
        }
        DirectConversation conversation = new DirectConversation();
        if (first.getId() < second.getId()) {
            conversation.firstUser = first;
            conversation.secondUser = second;
        } else {
            conversation.firstUser = second;
            conversation.secondUser = first;
        }
        return conversation;
    }

    public boolean hasParticipant(Long userId) {
        return userId != null && (firstUser.getId().equals(userId) || secondUser.getId().equals(userId));
    }

    public User otherParticipant(Long userId) {
        if (firstUser.getId().equals(userId)) {
            return secondUser;
        }
        if (secondUser.getId().equals(userId)) {
            return firstUser;
        }
        throw new IllegalArgumentException("User is not a participant in this conversation");
    }

    public void recordMessage(String body) {
        lastMessage = body;
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    void initializeTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getFirstUser() {
        return firstUser;
    }

    public User getSecondUser() {
        return secondUser;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
