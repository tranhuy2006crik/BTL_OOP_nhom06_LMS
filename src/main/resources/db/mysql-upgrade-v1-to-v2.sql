
USE lms_db;

ALTER TABLE roles
    ADD COLUMN scope VARCHAR(10) NOT NULL DEFAULT 'COURSE' AFTER name;

UPDATE roles
SET scope = CASE
    WHEN code = 'ADMIN' THEN 'SYSTEM'
    ELSE 'COURSE'
END
WHERE code IN ('ADMIN', 'TEACHER', 'STUDENT');

ALTER TABLE roles
    ADD UNIQUE KEY uk_roles_id_scope (id, scope),
    ADD CONSTRAINT chk_roles_scope CHECK (scope IN ('SYSTEM', 'COURSE'));

ALTER TABLE system_role_assignments
    ADD COLUMN role_scope VARCHAR(10) NOT NULL DEFAULT 'SYSTEM' AFTER role_id;

ALTER TABLE system_role_assignments
    DROP FOREIGN KEY fk_system_role_assignments_role;

ALTER TABLE system_role_assignments
    ADD CONSTRAINT fk_system_role_assignments_role_scope
        FOREIGN KEY (role_id, role_scope) REFERENCES roles (id, scope)
        ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE courses
    ADD COLUMN start_at DATETIME NULL AFTER status,
    ADD COLUMN end_at DATETIME NULL AFTER start_at,
    ADD CONSTRAINT chk_courses_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    ADD CONSTRAINT chk_courses_dates
        CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at);

ALTER TABLE course_memberships
    ADD COLUMN role_scope VARCHAR(10) NOT NULL DEFAULT 'COURSE' AFTER role_id,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP AFTER joined_at,
    DROP FOREIGN KEY fk_course_memberships_role;

ALTER TABLE course_memberships
    DROP INDEX idx_course_memberships_role;

ALTER TABLE course_memberships
    ADD KEY idx_course_memberships_role (role_id, role_scope),
    ADD CONSTRAINT chk_course_memberships_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'WITHDRAWN')),
    ADD CONSTRAINT fk_course_memberships_role_scope
        FOREIGN KEY (role_id, role_scope) REFERENCES roles (id, scope)
        ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE users
    ADD CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'));

ALTER TABLE course_sections
    ADD CONSTRAINT chk_course_sections_status CHECK (status IN ('VISIBLE', 'HIDDEN'));

ALTER TABLE activities
    ADD COLUMN available_from DATETIME NULL AFTER status,
    ADD COLUMN available_until DATETIME NULL AFTER available_from,
    ADD CONSTRAINT chk_activities_type CHECK (activity_type IN ('LESSON', 'ASSIGNMENT', 'RESOURCE')),
    ADD CONSTRAINT chk_activities_status CHECK (status IN ('VISIBLE', 'HIDDEN')),
    ADD CONSTRAINT chk_activities_availability
        CHECK (available_until IS NULL OR available_from IS NULL OR available_until >= available_from);

ALTER TABLE submissions
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    MODIFY COLUMN submitted_at DATETIME NULL DEFAULT NULL,
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER submitted_at,
    ADD CONSTRAINT chk_submissions_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'LATE')),
    ADD CONSTRAINT chk_submissions_timestamp CHECK (
        (status = 'DRAFT' AND submitted_at IS NULL)
        OR (status IN ('SUBMITTED', 'LATE') AND submitted_at IS NOT NULL)
    );

ALTER TABLE grades
    ADD COLUMN released_at DATETIME NULL AFTER graded_at,
    ADD CONSTRAINT chk_grades_release_time CHECK (released_at IS NULL OR released_at >= graded_at);

ALTER TABLE course_announcements
    MODIFY COLUMN published_at DATETIME NULL DEFAULT NULL;

ALTER TABLE course_announcements
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' AFTER content,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP AFTER published_at;

ALTER TABLE course_announcements
    ALTER COLUMN status SET DEFAULT 'DRAFT',
    ADD CONSTRAINT chk_course_announcements_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    ADD CONSTRAINT chk_course_announcements_publish_time CHECK (
        (status = 'DRAFT' AND published_at IS NULL)
        OR (status = 'PUBLISHED' AND published_at IS NOT NULL)
    );

ALTER TABLE activity_completions
    ADD CONSTRAINT chk_activity_completions_status
        CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    ADD CONSTRAINT chk_activity_completions_time
        CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL);
