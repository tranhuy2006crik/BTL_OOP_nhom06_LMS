package com.lms.system.repository;

import com.lms.system.dto.StudentCourseProgress;
import com.lms.system.dto.StudentGradeRow;
import com.lms.system.dto.GradingCourseRow;
import com.lms.system.dto.SubmissionForGrading;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class GradebookRepository {

    private final JdbcTemplate jdbcTemplate;

    public GradebookRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Lấy danh sách bài tập của các khóa học viên đang tham gia và chỉ ghép điểm đã công bố.
    public List<StudentGradeRow> findStudentGrades(Long studentId) {
        return jdbcTemplate.query("""
                SELECT c.id AS course_id, c.title AS course_title,
                       a.id AS assignment_id, a.title AS assignment_title,
                       ass.max_grade, ass.due_at,
                       s.id AS submission_id, s.attempt_number,
                       s.status AS submission_status, s.submitted_at,
                       g.score, g.feedback, g.graded_at
                FROM course_memberships cm
                JOIN roles r ON r.id = cm.role_id AND r.scope = cm.role_scope
                JOIN courses c ON c.id = cm.course_id
                JOIN course_sections cs ON cs.course_id = c.id AND cs.status = 'VISIBLE'
                JOIN activities a ON a.section_id = cs.id
                    AND a.activity_type = 'ASSIGNMENT' AND a.status = 'VISIBLE'
                JOIN assignments ass ON ass.activity_id = a.id
                LEFT JOIN submissions s ON s.assignment_id = ass.activity_id
                    AND s.student_id = cm.user_id
                    AND s.attempt_number = (
                        SELECT MAX(latest.attempt_number)
                        FROM submissions latest
                        WHERE latest.assignment_id = ass.activity_id
                          AND latest.student_id = cm.user_id
                    )
                -- Chỉ nối điểm đã công bố; điểm và nhận xét chưa công bố luôn được trả về rỗng cho học viên.
                LEFT JOIN grades g ON g.submission_id = s.id AND g.released_at IS NOT NULL
                WHERE cm.user_id = ? AND cm.status = 'ACTIVE' AND r.code = 'STUDENT'
                  AND cm.role_scope = 'COURSE' AND r.scope = 'COURSE'
                  AND c.status = 'PUBLISHED'
                ORDER BY c.title, ass.due_at, a.position, s.attempt_number DESC
                """, (resultSet, rowNumber) -> new StudentGradeRow(
                resultSet.getLong("course_id"),
                resultSet.getString("course_title"),
                resultSet.getLong("assignment_id"),
                resultSet.getString("assignment_title"),
                resultSet.getBigDecimal("max_grade"),
                localDateTime(resultSet, "due_at"),
                nullableLong(resultSet, "submission_id"),
                nullableInteger(resultSet, "attempt_number"),
                resultSet.getString("submission_status"),
                localDateTime(resultSet, "submitted_at"),
                resultSet.getBigDecimal("score"),
                resultSet.getString("feedback"),
                localDateTime(resultSet, "graded_at")
        ), studentId);
    }

    // Tính tiến độ từ số hoạt động đang hiển thị và số hoạt động học viên đã hoàn thành.
    public List<StudentCourseProgress> findStudentProgress(Long studentId) {
        return jdbcTemplate.query("""
                SELECT c.id AS course_id, c.title AS course_title,
                       COUNT(DISTINCT a.id) AS total_activities,
                       COUNT(DISTINCT CASE WHEN ac.status = 'COMPLETED' THEN a.id END)
                           AS completed_activities
                FROM course_memberships cm
                JOIN roles r ON r.id = cm.role_id AND r.scope = cm.role_scope
                JOIN courses c ON c.id = cm.course_id
                LEFT JOIN course_sections cs ON cs.course_id = c.id AND cs.status = 'VISIBLE'
                LEFT JOIN activities a ON a.section_id = cs.id AND a.status = 'VISIBLE'
                LEFT JOIN activity_completions ac
                    ON ac.activity_id = a.id AND ac.user_id = cm.user_id
                WHERE cm.user_id = ? AND cm.status = 'ACTIVE' AND r.code = 'STUDENT'
                  AND cm.role_scope = 'COURSE' AND r.scope = 'COURSE'
                  AND c.status = 'PUBLISHED'
                GROUP BY c.id, c.title
                ORDER BY c.title
                """, (resultSet, rowNumber) -> new StudentCourseProgress(
                resultSet.getLong("course_id"),
                resultSet.getString("course_title"),
                resultSet.getLong("total_activities"),
                resultSet.getLong("completed_activities")
        ), studentId);
    }

    // Lấy bài đã nộp để chấm, kèm nội dung, tệp và trạng thái công bố hiện tại.
    public List<SubmissionForGrading> findSubmissionsForGrading(Long courseId) {
        return jdbcTemplate.query("""
                SELECT s.id AS submission_id, a.id AS assignment_id, a.title AS assignment_title,
                       u.full_name AS student_name, u.email AS student_email,
                       s.status, s.submitted_at, s.attempt_number, s.text_content,
                       ass.max_grade, g.score, g.feedback, g.graded_at, g.released_at,
                       (SELECT GROUP_CONCAT(f.original_name ORDER BY f.original_name SEPARATOR ', ')
                        FROM submission_files sf
                        JOIN files f ON f.id = sf.file_id
                        WHERE sf.submission_id = s.id) AS attached_file_names
                FROM submissions s
                JOIN assignments ass ON ass.activity_id = s.assignment_id
                JOIN activities a ON a.id = ass.activity_id AND a.activity_type = 'ASSIGNMENT'
                JOIN course_sections cs ON cs.id = a.section_id
                JOIN users u ON u.id = s.student_id
                LEFT JOIN grades g ON g.submission_id = s.id
                WHERE cs.course_id = ? AND s.status IN ('SUBMITTED', 'LATE')
                ORDER BY a.position, a.title, s.submitted_at, u.full_name, s.attempt_number
                """, (resultSet, rowNumber) -> toSubmissionForGrading(resultSet), courseId);
    }

    public String findCourseTitle(Long courseId) {
        List<String> titles = jdbcTemplate.query(
                "SELECT title FROM courses WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getString("title"),
                courseId
        );
        if (titles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học");
        }
        return titles.get(0);
    }

    public List<GradingCourseRow> findCoursesForGrading(Long userId) {
        return jdbcTemplate.query("""
                SELECT c.id AS course_id, c.title AS course_title,
                       COUNT(DISTINCT s.id) AS submission_count,
                       COUNT(DISTINCT CASE WHEN s.id IS NOT NULL AND g.submission_id IS NULL
                                           THEN s.id END) AS ungraded_count,
                       COUNT(DISTINCT CASE WHEN g.submission_id IS NOT NULL
                                                AND g.released_at IS NULL
                                           THEN s.id END) AS awaiting_release_count
                FROM courses c
                LEFT JOIN course_sections cs ON cs.course_id = c.id
                LEFT JOIN activities a ON a.section_id = cs.id AND a.activity_type = 'ASSIGNMENT'
                LEFT JOIN assignments ass ON ass.activity_id = a.id
                LEFT JOIN submissions s ON s.assignment_id = ass.activity_id
                    AND s.status IN ('SUBMITTED', 'LATE')
                LEFT JOIN grades g ON g.submission_id = s.id
                WHERE c.status <> 'ARCHIVED' AND (EXISTS (
                    SELECT 1
                    FROM course_memberships cm
                    JOIN roles r ON r.id = cm.role_id AND r.scope = cm.role_scope
                    WHERE cm.user_id = ? AND cm.course_id = c.id AND cm.status = 'ACTIVE'
                      AND r.code = 'TEACHER' AND r.scope = 'COURSE'
                ) OR EXISTS (
                    SELECT 1
                    FROM system_role_assignments sra
                    JOIN roles r ON r.id = sra.role_id AND r.scope = sra.role_scope
                    WHERE sra.user_id = ? AND r.code = 'ADMIN' AND r.scope = 'SYSTEM'
                ))
                GROUP BY c.id, c.title
                ORDER BY c.title
                """, (resultSet, rowNumber) -> new GradingCourseRow(
                resultSet.getLong("course_id"),
                resultSet.getString("course_title"),
                resultSet.getLong("submission_count"),
                resultSet.getLong("ungraded_count"),
                resultSet.getLong("awaiting_release_count")
        ), userId, userId);
    }

    public BigDecimal lockSubmissionMaxGrade(Long courseId, Long submissionId) {
        List<BigDecimal> maxGrades = jdbcTemplate.query("""
                SELECT ass.max_grade
                FROM submissions s
                JOIN assignments ass ON ass.activity_id = s.assignment_id
                JOIN activities a ON a.id = ass.activity_id
                JOIN course_sections cs ON cs.id = a.section_id
                WHERE s.id = ? AND cs.course_id = ?
                  AND s.status IN ('SUBMITTED', 'LATE')
                FOR UPDATE
                """, (resultSet, rowNumber) -> resultSet.getBigDecimal("max_grade"),
                submissionId, courseId);
        if (maxGrades.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài nộp hợp lệ");
        }
        return maxGrades.get(0);
    }

    // Lưu mới hoặc cập nhật điểm; mỗi lần chỉnh sửa đều chuyển điểm về trạng thái chưa công bố.
    public void saveGrade(Long submissionId, Long graderId, BigDecimal score, String feedback) {
        jdbcTemplate.update("""
                INSERT INTO grades (submission_id, graded_by, score, feedback, graded_at, released_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, NULL)
                ON DUPLICATE KEY UPDATE
                    graded_by = VALUES(graded_by),
                    score = VALUES(score),
                    feedback = VALUES(feedback),
                    graded_at = CURRENT_TIMESTAMP,
                    released_at = NULL
                """, submissionId, graderId, score, feedback);
    }

    // Công bố điểm; phân biệt bài nộp không tồn tại với trường hợp chưa lưu điểm.
    public void releaseGrade(Long courseId, Long submissionId) {
        int updated = jdbcTemplate.update("""
                UPDATE grades g
                JOIN submissions s ON s.id = g.submission_id
                JOIN assignments ass ON ass.activity_id = s.assignment_id
                JOIN activities a ON a.id = ass.activity_id
                JOIN course_sections cs ON cs.id = a.section_id
                SET g.released_at = CURRENT_TIMESTAMP
                WHERE s.id = ? AND cs.course_id = ?
                """, submissionId, courseId);
        if (updated == 0) {
            Integer submissionExists = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM submissions s
                    JOIN assignments ass ON ass.activity_id = s.assignment_id
                    JOIN activities a ON a.id = ass.activity_id
                    JOIN course_sections cs ON cs.id = a.section_id
                    WHERE s.id = ? AND cs.course_id = ?
                    """, Integer.class, submissionId, courseId);
            if (submissionExists == null || submissionExists == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài nộp trong khóa học");
            }
            Integer gradeExists = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM grades g
                    JOIN submissions s ON s.id = g.submission_id
                    JOIN assignments ass ON ass.activity_id = s.assignment_id
                    JOIN activities a ON a.id = ass.activity_id
                    JOIN course_sections cs ON cs.id = a.section_id
                    WHERE s.id = ? AND cs.course_id = ?
                    """, Integer.class, submissionId, courseId);
            if (gradeExists == null || gradeExists == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Hãy lưu điểm trước khi công bố");
            }
        }
    }

    private static SubmissionForGrading toSubmissionForGrading(ResultSet resultSet) throws SQLException {
        return new SubmissionForGrading(
                resultSet.getLong("submission_id"),
                resultSet.getLong("assignment_id"),
                resultSet.getString("assignment_title"),
                resultSet.getString("student_name"),
                resultSet.getString("student_email"),
                resultSet.getString("status"),
                localDateTime(resultSet, "submitted_at"),
                resultSet.getInt("attempt_number"),
                resultSet.getString("text_content"),
                resultSet.getString("attached_file_names"),
                resultSet.getBigDecimal("max_grade"),
                resultSet.getBigDecimal("score"),
                resultSet.getString("feedback"),
                localDateTime(resultSet, "graded_at"),
                localDateTime(resultSet, "released_at")
        );
    }

    private static LocalDateTime localDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }
}
