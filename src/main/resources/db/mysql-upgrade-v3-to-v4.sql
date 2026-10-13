USE lms_db;

CREATE TABLE direct_conversations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    first_user_id BIGINT UNSIGNED NOT NULL,
    second_user_id BIGINT UNSIGNED NOT NULL,
    last_message VARCHAR(2000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_direct_conversations_participants (first_user_id, second_user_id),
    KEY idx_direct_conversations_second_updated (second_user_id, updated_at),
    CONSTRAINT fk_direct_conversations_first_user
        FOREIGN KEY (first_user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_direct_conversations_second_user
        FOREIGN KEY (second_user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE direct_messages (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT UNSIGNED NOT NULL,
    sender_id BIGINT UNSIGNED NOT NULL,
    body VARCHAR(2000) NOT NULL,
    sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_direct_messages_conversation_sent (conversation_id, sent_at, id),
    KEY idx_direct_messages_conversation_read_sender (conversation_id, read_at, sender_id),
    CONSTRAINT fk_direct_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES direct_conversations (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_direct_messages_sender
        FOREIGN KEY (sender_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
