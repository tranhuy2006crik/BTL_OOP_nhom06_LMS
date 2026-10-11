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

import java.time.LocalDateTime;

@Entity
@Table(name = "direct_messages")
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private DirectConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User sender;

    @Column(name = "body", nullable = false, length = 2000)
    private String body;

    @Column(name = "sent_at", nullable = false, updatable = false, columnDefinition = "datetime")
    private LocalDateTime sentAt;

    @Column(name = "read_at", columnDefinition = "datetime")
    private LocalDateTime readAt;

    protected DirectMessage() {
    }

    public DirectMessage(DirectConversation conversation, User sender, String body) {
        this.conversation = conversation;
        this.sender = sender;
        this.body = body;
    }

    @PrePersist
    void initializeSentAt() {
        sentAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public DirectConversation getConversation() {
        return conversation;
    }

    public User getSender() {
        return sender;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
