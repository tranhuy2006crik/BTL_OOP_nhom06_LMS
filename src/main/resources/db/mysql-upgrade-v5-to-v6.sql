CREATE TABLE IF NOT EXISTS course_assignments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_id BIGINT UNSIGNED NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    instructions TEXT NULL,
    due_at DATETIME NOT NULL,
    max_score INT NOT NULL DEFAULT 100,
    allow_late BOOLEAN NOT NULL DEFAULT FALSE,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course_assignments_course_due (course_id, due_at),
    CONSTRAINT fk_course_assignments_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_course_assignments_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment_submissions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT UNSIGNED NOT NULL,
    student_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    text_answer TEXT NULL,
    attachment_name VARCHAR(255) NULL,
    attachment_path VARCHAR(500) NULL,
    grade INT NULL,
    feedback TEXT NULL,
    attempt_number INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_assignment_submission_student (assignment_id, student_id),
    KEY idx_assignment_submissions_assignment (assignment_id),
    KEY idx_assignment_submissions_student (student_id),
    CONSTRAINT fk_assignment_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES course_assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_submissions_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_materials (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_id BIGINT UNSIGNED NOT NULL,
    uploaded_by BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    storage_path VARCHAR(500) NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course_materials_course (course_id),
    CONSTRAINT fk_course_materials_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_course_materials_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
