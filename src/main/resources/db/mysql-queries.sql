
-- 1. Courses in which a student has an active membership.
SELECT c.id, c.course_code, c.title, c.description, cm.joined_at
FROM course_memberships cm
JOIN courses c ON c.id = cm.course_id
JOIN roles r ON r.id = cm.role_id
WHERE cm.user_id = :student_id
  AND r.code = 'STUDENT'
  AND cm.status = 'ACTIVE'
  AND c.status = 'PUBLISHED'
ORDER BY c.title;

-- 2. Search published courses by course title or teacher name.
SELECT DISTINCT c.id, c.course_code, c.title, u.full_name AS teacher_name
FROM courses c
JOIN course_memberships cm ON cm.course_id = c.id
JOIN roles r ON r.id = cm.role_id AND r.code = 'TEACHER'
JOIN users u ON u.id = cm.user_id
WHERE c.status = 'PUBLISHED'
  AND (c.title LIKE CONCAT('%', :search_text, '%')
       OR u.full_name LIKE CONCAT('%', :search_text, '%'))
ORDER BY c.title;

-- 3. Ordered course outline, including lesson content when the activity is a lesson.
SELECT cs.id AS section_id, cs.title AS section_title, cs.position AS section_position,
       a.id AS activity_id, a.activity_type, a.title AS activity_title,
       a.position AS activity_position, l.content AS lesson_content
FROM course_sections cs
LEFT JOIN activities a ON a.section_id = cs.id AND a.status = 'VISIBLE'
LEFT JOIN lessons l ON l.activity_id = a.id
WHERE cs.course_id = :course_id
  AND cs.status = 'VISIBLE'
ORDER BY cs.position, a.position;

-- 4. Upcoming assignments for an active student in their enrolled courses.
SELECT c.id AS course_id, c.title AS course_title, a.id AS assignment_id,
       a.title AS assignment_title, ass.due_at, ass.max_grade
FROM course_memberships cm
JOIN roles r ON r.id = cm.role_id AND r.code = 'STUDENT'
JOIN courses c ON c.id = cm.course_id
JOIN course_sections cs ON cs.course_id = c.id
JOIN activities a ON a.section_id = cs.id
JOIN assignments ass ON ass.activity_id = a.id
WHERE cm.user_id = :student_id
  AND cm.status = 'ACTIVE'
  AND c.status = 'PUBLISHED'
  AND cs.status = 'VISIBLE'
  AND a.status = 'VISIBLE'
  AND (ass.due_at IS NULL OR ass.due_at >= CURRENT_TIMESTAMP)
ORDER BY ass.due_at, c.title;

-- 5. A student's submissions and released grades for one assignment.
SELECT s.id AS submission_id, s.attempt_number, s.status AS submission_status,
       s.submitted_at, g.score, g.feedback, g.graded_at,
       grader.full_name AS graded_by_name
FROM submissions s
LEFT JOIN grades g
    ON g.submission_id = s.id AND g.released_at IS NOT NULL
LEFT JOIN users grader ON grader.id = g.graded_by
WHERE s.assignment_id = :assignment_id
  AND s.student_id = :student_id
ORDER BY s.attempt_number DESC;

-- 6. Published announcements for a course.
SELECT id, title, content, published_at
FROM course_announcements
WHERE course_id = :course_id
  AND status = 'PUBLISHED'
  AND published_at <= CURRENT_TIMESTAMP
ORDER BY published_at DESC;

-- 7. Completion progress for a student in one course, as a percentage.
SELECT
    COUNT(a.id) AS visible_activity_count,
    SUM(CASE WHEN ac.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_activity_count,
    CASE
        WHEN COUNT(a.id) = 0 THEN 0
        ELSE ROUND(
            100.0 * SUM(CASE WHEN ac.status = 'COMPLETED' THEN 1 ELSE 0 END)
            / COUNT(a.id),
            2
        )
    END AS progress_percent
FROM course_sections cs
JOIN activities a ON a.section_id = cs.id AND a.status = 'VISIBLE'
LEFT JOIN activity_completions ac
    ON ac.activity_id = a.id AND ac.user_id = :student_id
WHERE cs.course_id = :course_id
  AND cs.status = 'VISIBLE';
