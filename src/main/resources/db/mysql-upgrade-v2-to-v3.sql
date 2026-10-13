USE lms_db;

CREATE TABLE user_profiles (
    user_id BIGINT UNSIGNED NOT NULL,
    email_visibility VARCHAR(30) NOT NULL DEFAULT 'COURSE_PARTICIPANTS',
    city_town VARCHAR(100) NULL,
    country VARCHAR(100) NULL,
    timezone VARCHAR(80) NULL,
    preferred_theme VARCHAR(20) NOT NULL DEFAULT 'DEFAULT',
    description TEXT NULL,
    picture_description VARCHAR(255) NULL,
    first_name_phonetic VARCHAR(100) NULL,
    last_name_phonetic VARCHAR(100) NULL,
    middle_name VARCHAR(100) NULL,
    alternate_name VARCHAR(100) NULL,
    interests TEXT NULL,
    id_number VARCHAR(100) NULL,
    institution VARCHAR(150) NULL,
    department VARCHAR(150) NULL,
    phone VARCHAR(40) NULL,
    mobile_phone VARCHAR(40) NULL,
    address VARCHAR(255) NULL,
    skype_id VARCHAR(100) NULL,
    website VARCHAR(255) NULL,
    profile_picture MEDIUMBLOB NULL,
    profile_picture_content_type VARCHAR(30) NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
