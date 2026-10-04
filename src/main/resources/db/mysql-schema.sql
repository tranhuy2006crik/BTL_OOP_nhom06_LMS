
CREATE DATABASE IF NOT EXISTS lms_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE lms_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_status (status),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(80) NOT NULL,
    scope VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_code (code),
    UNIQUE KEY uk_roles_id_scope (id, scope),
    CONSTRAINT chk_roles_scope CHECK (scope IN ('SYSTEM', 'COURSE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_role_assignments (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    role_scope VARCHAR(10) NOT NULL DEFAULT 'SYSTEM',
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_system_role_assignments_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_system_role_assignments_role_scope
        FOREIGN KEY (role_id, role_scope) REFERENCES roles (id, scope)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS course_categories (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    parent_id BIGINT UNSIGNED NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT NULL,
    PRIMARY KEY (id),
    KEY idx_course_categories_parent (parent_id),
    CONSTRAINT fk_course_categories_parent
        FOREIGN KEY (parent_id) REFERENCES course_categories (id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS courses (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id BIGINT UNSIGNED NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    course_code VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    start_at DATETIME NULL,
    end_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_courses_course_code (course_code),
    KEY idx_courses_category (category_id),
    KEY idx_courses_created_by (created_by),
    KEY idx_courses_status_title (status, title),
    CONSTRAINT fk_courses_category
        FOREIGN KEY (category_id) REFERENCES course_categories (id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_courses_creator
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_courses_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_courses_dates CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS course_memberships (
    course_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    role_scope VARCHAR(10) NOT NULL DEFAULT 'COURSE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (course_id, user_id),
    KEY idx_course_memberships_user_status (user_id, status),
    KEY idx_course_memberships_role (role_id, role_scope),
    CONSTRAINT chk_course_memberships_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'WITHDRAWN')),
    CONSTRAINT fk_course_memberships_course
        FOREIGN KEY (course_id) REFERENCES courses (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_course_memberships_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_course_memberships_role_scope
        FOREIGN KEY (role_id, role_scope) REFERENCES roles (id, scope)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS course_sections (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    position INT UNSIGNED NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_sections_course_position (course_id, position),
    CONSTRAINT fk_course_sections_course
        FOREIGN KEY (course_id) REFERENCES courses (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_course_sections_status CHECK (status IN ('VISIBLE', 'HIDDEN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activities (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    section_id BIGINT UNSIGNED NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    activity_type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    position INT UNSIGNED NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'HIDDEN',
    available_from DATETIME NULL,
    available_until DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_activities_section_position (section_id, position),
    KEY idx_activities_created_by (created_by),
    KEY idx_activities_type_status (activity_type, status),
    CONSTRAINT fk_activities_section
        FOREIGN KEY (section_id) REFERENCES course_sections (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_activities_creator
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_activities_type CHECK (activity_type IN ('LESSON', 'ASSIGNMENT', 'RESOURCE')),
    CONSTRAINT chk_activities_status CHECK (status IN ('VISIBLE', 'HIDDEN')),
    CONSTRAINT chk_activities_availability
        CHECK (available_until IS NULL OR available_from IS NULL OR available_until >= available_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lessons (
    activity_id BIGINT UNSIGNED NOT NULL,
    content LONGTEXT NOT NULL,
    PRIMARY KEY (activity_id),
    CONSTRAINT fk_lessons_activity
        FOREIGN KEY (activity_id) REFERENCES activities (id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assignments (
    activity_id BIGINT UNSIGNED NOT NULL,
    due_at DATETIME NULL,
    max_grade DECIMAL(7,2) NOT NULL DEFAULT 100.00,
    allow_late_submission BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (activity_id),
    CONSTRAINT fk_assignments_activity
        FOREIGN KEY (activity_id) REFERENCES activities (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_assignments_max_grade CHECK (max_grade > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS submissions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT UNSIGNED NOT NULL,
    student_id BIGINT UNSIGNED NOT NULL,
    attempt_number INT UNSIGNED NOT NULL DEFAULT 1,
    text_content LONGTEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_submissions_attempt (assignment_id, student_id, attempt_number),
    KEY idx_submissions_student_submitted (student_id, submitted_at),
    CONSTRAINT fk_submissions_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments (activity_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_submissions_student
        FOREIGN KEY (student_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_submissions_attempt_number CHECK (attempt_number > 0),
    CONSTRAINT chk_submissions_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'LATE')),
    CONSTRAINT chk_submissions_timestamp CHECK (
        (status = 'DRAFT' AND submitted_at IS NULL)
        OR (status IN ('SUBMITTED', 'LATE') AND submitted_at IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS grades (
    submission_id BIGINT UNSIGNED NOT NULL,
    graded_by BIGINT UNSIGNED NOT NULL,
    score DECIMAL(7,2) NOT NULL,
    feedback TEXT NULL,
    graded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at DATETIME NULL,
    PRIMARY KEY (submission_id),
    KEY idx_grades_graded_by (graded_by),
    CONSTRAINT fk_grades_submission
        FOREIGN KEY (submission_id) REFERENCES submissions (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_grades_grader
        FOREIGN KEY (graded_by) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_grades_score CHECK (score >= 0),
    CONSTRAINT chk_grades_release_time CHECK (released_at IS NULL OR released_at >= graded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS course_announcements (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_id BIGINT UNSIGNED NOT NULL,
    author_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course_announcements_course_published (course_id, published_at),
    KEY idx_course_announcements_author (author_id),
    CONSTRAINT fk_course_announcements_course
        FOREIGN KEY (course_id) REFERENCES courses (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_course_announcements_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_course_announcements_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT chk_course_announcements_publish_time CHECK (
        (status = 'DRAFT' AND published_at IS NULL)
        OR (status = 'PUBLISHED' AND published_at IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS files (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    uploaded_by BIGINT UNSIGNED NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT UNSIGNED NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_files_storage_key (storage_key),
    KEY idx_files_uploaded_by (uploaded_by),
    CONSTRAINT fk_files_uploader
        FOREIGN KEY (uploaded_by) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activity_files (
    activity_id BIGINT UNSIGNED NOT NULL,
    file_id BIGINT UNSIGNED NOT NULL,
    purpose VARCHAR(30) NOT NULL DEFAULT 'RESOURCE',
    position INT UNSIGNED NOT NULL DEFAULT 1,
    PRIMARY KEY (activity_id, file_id),
    KEY idx_activity_files_file (file_id),
    CONSTRAINT fk_activity_files_activity
        FOREIGN KEY (activity_id) REFERENCES activities (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_activity_files_file
        FOREIGN KEY (file_id) REFERENCES files (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS submission_files (
    submission_id BIGINT UNSIGNED NOT NULL,
    file_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (submission_id, file_id),
    KEY idx_submission_files_file (file_id),
    CONSTRAINT fk_submission_files_submission
        FOREIGN KEY (submission_id) REFERENCES submissions (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_submission_files_file
        FOREIGN KEY (file_id) REFERENCES files (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activity_completions (
    activity_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    completed_at DATETIME NULL,
    PRIMARY KEY (activity_id, user_id),
    KEY idx_activity_completions_user_status (user_id, status),
    CONSTRAINT chk_activity_completions_status
        CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT chk_activity_completions_time
        CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL),
    CONSTRAINT fk_activity_completions_activity
        FOREIGN KEY (activity_id) REFERENCES activities (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_activity_completions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (code, name, scope)
VALUES
    ('ADMIN', 'Administrator', 'SYSTEM'),
    ('TEACHER', 'Teacher', 'COURSE'),
    ('STUDENT', 'Student', 'COURSE') AS new_role
ON DUPLICATE KEY UPDATE
    name = new_role.name,
    scope = new_role.scope;

-- Post-install verification: expect 17 tables and the three role/scope pairs.
SELECT COUNT(*) AS lms_table_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'lms_db'
  AND TABLE_TYPE = 'BASE TABLE';

SELECT code, name, scope
FROM roles
ORDER BY code;
