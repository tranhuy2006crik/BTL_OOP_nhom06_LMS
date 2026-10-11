USE lms_db;

CREATE TABLE direct_conversation_stars (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    conversation_id BIGINT UNSIGNED NOT NULL,
    starred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_direct_conversation_stars_user_conversation (user_id, conversation_id),
    KEY idx_direct_conversation_stars_conversation (conversation_id),
    CONSTRAINT fk_direct_conversation_stars_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_direct_conversation_stars_conversation
        FOREIGN KEY (conversation_id) REFERENCES direct_conversations (id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
