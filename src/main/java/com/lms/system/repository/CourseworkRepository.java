package com.lms.system.repository;

import com.lms.system.dto.CourseAssignmentView;
import com.lms.system.dto.CourseFileView;
import com.lms.system.dto.CourseResourceView;
import com.lms.system.dto.CourseSubmissionView;
import com.lms.system.service.CourseFileStorageService.StoredCourseFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class CourseworkRepository {

    private final JdbcTemplate jdbcTemplate;

    public CourseworkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CourseAssignmentView> findAssignments(Long courseId, Long sectionId, Long studentId, boolean canManage) {
        List<AssignmentRow> rows = jdbcTemplate.query("""
                SELECT a.id, a.title, a.description, a.position, a.status, ass.due_at,
                       ass.max_grade, ass.allow_late_submission,
                       s.id AS submission_id, s.attempt_number, s.status AS submission_status,
                       s.submitted_at
                FROM activities a
                JOIN course_sections cs ON cs.id = a.section_id
                JOIN assignments ass ON ass.activity_id = a.id
                LEFT JOIN submissions s ON s.assignment_id = a.id AND s.student_id = ?
                    AND s.attempt_number = (
                        SELECT MAX(latest.attempt_number)
                        FROM submissions latest
                        WHERE latest.assignment_id = a.id AND latest.student_id = ?
                    )
                WHERE a.section_id = ? AND cs.course_id = ? AND a.activity_type = 'ASSIGNMENT'
                  AND (? = TRUE OR (a.status = 'VISIBLE' AND cs.status = 'VISIBLE'))
                ORDER BY a.position, a.id
                """, (resultSet, rowNumber) -> new AssignmentRow(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getInt("position"),
                resultSet.getString("status"),
                localDateTime(resultSet.getTimestamp("due_at")),
                resultSet.getBigDecimal("max_grade"),
                resultSet.getBoolean("allow_late_submission"),
                nullableLong(resultSet.getLong("submission_id"), resultSet.wasNull()),
                nullableInteger(resultSet.getInt("attempt_number"), resultSet.wasNull()),
                resultSet.getString("submission_status"),
                localDateTime(resultSet.getTimestamp("submitted_at"))
        ), studentId, studentId, sectionId, courseId, canManage);

        Map<Long, List<CourseFileView>> activityFiles = findActivityFiles(
                rows.stream().map(AssignmentRow::id).toList());
        List<Long> submissionIds = rows.stream().map(AssignmentRow::submissionId)
                .filter(id -> id != null).toList();
        Map<Long, List<CourseFileView>> submissionFiles = findSubmissionFiles(submissionIds);

        return rows.stream().map(row -> {
            CourseSubmissionView submission = row.submissionId() == null ? null : new CourseSubmissionView(
                    row.submissionId(),
                    row.attemptNumber(),
                    row.submissionStatus(),
                    row.submittedAt(),
                    submissionFiles.getOrDefault(row.submissionId(), List.of())
            );
            return new CourseAssignmentView(
                    row.id(), row.title(), row.description(), row.position(), row.status(),
                    row.dueAt(), row.maxGrade(), row.allowLateSubmission(),
                    activityFiles.getOrDefault(row.id(), List.of()), submission
            );
        }).toList();
    }

    public List<CourseResourceView> findResources(Long courseId, Long sectionId, boolean canManage) {
        List<ResourceRow> rows = jdbcTemplate.query("""
                SELECT a.id, a.title, a.description, a.position, a.status
                FROM activities a
                JOIN course_sections cs ON cs.id = a.section_id
                WHERE a.section_id = ? AND cs.course_id = ? AND a.activity_type = 'RESOURCE'
                  AND (? = TRUE OR (a.status = 'VISIBLE' AND cs.status = 'VISIBLE'))
                ORDER BY a.position, a.id
                """, (resultSet, rowNumber) -> new ResourceRow(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getInt("position"),
                resultSet.getString("status")
        ), sectionId, courseId, canManage);

        Map<Long, List<CourseFileView>> filesByActivity = findActivityFiles(
                rows.stream().map(ResourceRow::id).toList());
        return rows.stream().map(row -> new CourseResourceView(
                row.id(), row.title(), row.description(), row.position(), row.status(),
                filesByActivity.getOrDefault(row.id(), List.of())
        )).toList();
    }

    public Long createAssignment(
            Long courseId,
            Long sectionId,
            Long teacherId,
            String title,
            String description,
            LocalDateTime dueAt,
            BigDecimal maxGrade,
            boolean allowLateSubmission
    ) {
        long activityId = createActivity(courseId, sectionId, teacherId, "ASSIGNMENT", title, description);
        jdbcTemplate.update("""
                INSERT INTO assignments (activity_id, due_at, max_grade, allow_late_submission)
                VALUES (?, ?, ?, ?)
                """, activityId, timestamp(dueAt), maxGrade, allowLateSubmission);
        return activityId;
    }

    public void updateAssignment(
            Long courseId,
            Long sectionId,
            Long assignmentId,
            String title,
            String description,
            LocalDateTime dueAt,
            BigDecimal maxGrade,
            boolean allowLateSubmission
    ) {
        int updated = jdbcTemplate.update("""
                UPDATE activities a
                JOIN course_sections cs ON cs.id = a.section_id
                JOIN assignments ass ON ass.activity_id = a.id
                SET a.title = ?, a.description = ?, a.updated_at = CURRENT_TIMESTAMP,
                    ass.due_at = ?, ass.max_grade = ?, ass.allow_late_submission = ?
                WHERE a.id = ? AND a.section_id = ? AND cs.course_id = ?
                  AND a.activity_type = 'ASSIGNMENT'
                """, title.trim(), description, timestamp(dueAt), maxGrade, allowLateSubmission,
                assignmentId, sectionId, courseId);
        if (updated == 0) {
            Integer exists = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM activities a
                    JOIN course_sections cs ON cs.id = a.section_id
                    WHERE a.id = ? AND a.section_id = ? AND cs.course_id = ?
                      AND a.activity_type = 'ASSIGNMENT'
                    """, Integer.class, assignmentId, sectionId, courseId);
            if (exists == null || exists == 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy bài tập trong chương này");
            }
        }
    }

    public Long createResource(
            Long courseId,
            Long sectionId,
            Long teacherId,
            String title,
            String description
    ) {
        return createActivity(courseId, sectionId, teacherId, "RESOURCE", title, description);
    }

    public CourseFileView saveFile(Long uploaderId, StoredCourseFile file) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO files (uploaded_by, storage_key, original_name, mime_type, size_bytes)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, uploaderId);
            statement.setString(2, file.storageKey());
            statement.setString(3, file.originalName());
            statement.setString(4, file.mimeType());
            statement.setLong(5, file.sizeBytes());
            return statement;
        }, keyHolder);
        return new CourseFileView(requireGeneratedId(keyHolder), file.originalName(), file.mimeType(), file.sizeBytes());
    }

    public void attachFilesToActivity(Long activityId, List<Long> fileIds, String purpose) {
        int position = 1;
        for (Long fileId : fileIds) {
            jdbcTemplate.update("""
                    INSERT INTO activity_files (activity_id, file_id, purpose, position)
                    VALUES (?, ?, ?, ?)
                    """, activityId, fileId, purpose, position++);
        }
    }

    public Long submit(
            Long courseId,
            Long assignmentId,
            Long studentId,
            String textContent,
            List<Long> fileIds,
            LocalDateTime submittedAt
    ) {
        List<SubmissionWindow> windows = jdbcTemplate.query("""
                SELECT c.status AS course_status, cs.status AS section_status,
                       a.status AS activity_status, ass.due_at, ass.allow_late_submission
                FROM assignments ass
                JOIN activities a ON a.id = ass.activity_id AND a.activity_type = 'ASSIGNMENT'
                JOIN course_sections cs ON cs.id = a.section_id
                JOIN courses c ON c.id = cs.course_id
                WHERE ass.activity_id = ? AND cs.course_id = ?
                FOR UPDATE
                """, (resultSet, rowNumber) -> new SubmissionWindow(
                resultSet.getString("course_status"),
                resultSet.getString("section_status"),
                resultSet.getString("activity_status"),
                localDateTime(resultSet.getTimestamp("due_at")),
                resultSet.getBoolean("allow_late_submission")
        ), assignmentId, courseId);
        if (windows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập trong khóa học");
        }
        SubmissionWindow window = windows.get(0);
        if (!"PUBLISHED".equals(window.courseStatus())
                || !"VISIBLE".equals(window.sectionStatus())
                || !"VISIBLE".equals(window.activityStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bài tập hiện không nhận bài nộp");
        }
        Integer membershipCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM course_memberships cm
                JOIN roles r ON r.id = cm.role_id AND r.scope = cm.role_scope
                WHERE cm.course_id = ? AND cm.user_id = ? AND cm.status = 'ACTIVE'
                  AND r.code = 'STUDENT'
                """, Integer.class, courseId, studentId);
        if (membershipCount == null || membershipCount == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền nộp bài trong khóa học này");
        }

        boolean late = window.dueAt() != null && submittedAt.isAfter(window.dueAt());
        if (late && !window.allowLateSubmission()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đã quá hạn nộp bài");
        }
        Integer previousAttempts = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(attempt_number), 0) FROM submissions WHERE assignment_id = ? AND student_id = ?",
                Integer.class, assignmentId, studentId);
        int attemptNumber = (previousAttempts == null ? 0 : previousAttempts) + 1;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String status = late ? "LATE" : "SUBMITTED";
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO submissions
                        (assignment_id, student_id, attempt_number, text_content, status, submitted_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, assignmentId);
            statement.setLong(2, studentId);
            statement.setInt(3, attemptNumber);
            statement.setString(4, textContent);
            statement.setString(5, status);
            statement.setTimestamp(6, Timestamp.valueOf(submittedAt));
            return statement;
        }, keyHolder);
        long submissionId = requireGeneratedId(keyHolder);
        for (Long fileId : fileIds) {
            jdbcTemplate.update(
                    "INSERT INTO submission_files (submission_id, file_id) VALUES (?, ?)",
                    submissionId, fileId
            );
        }
        jdbcTemplate.update("""
                INSERT INTO activity_completions (activity_id, user_id, status, completed_at)
                VALUES (?, ?, 'COMPLETED', CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    status = 'COMPLETED',
                    completed_at = CURRENT_TIMESTAMP
                """, assignmentId, studentId);
        return submissionId;
    }

    public FileRecord findActivityFile(Long courseId, Long fileId) {
        List<FileRecord> files = jdbcTemplate.query("""
                SELECT f.id, f.storage_key, f.original_name, f.mime_type, f.size_bytes,
                       c.status AS course_status, cs.status AS section_status, a.status AS activity_status
                FROM files f
                JOIN activity_files af ON af.file_id = f.id
                JOIN activities a ON a.id = af.activity_id
                JOIN course_sections cs ON cs.id = a.section_id
                JOIN courses c ON c.id = cs.course_id
                WHERE f.id = ? AND cs.course_id = ?
                  AND a.activity_type IN ('ASSIGNMENT', 'RESOURCE')
                """, (resultSet, rowNumber) -> new FileRecord(
                resultSet.getLong("id"), resultSet.getString("storage_key"),
                resultSet.getString("original_name"), resultSet.getString("mime_type"),
                resultSet.getLong("size_bytes"), resultSet.getString("course_status"),
                resultSet.getString("section_status"), resultSet.getString("activity_status"), null
        ), fileId, courseId);
        return files.stream().findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp trong khóa học"));
    }

    public FileRecord findSubmissionFile(Long courseId, Long submissionId, Long fileId) {
        List<FileRecord> files = jdbcTemplate.query("""
                SELECT f.id, f.storage_key, f.original_name, f.mime_type, f.size_bytes,
                       c.status AS course_status, cs.status AS section_status, a.status AS activity_status,
                       s.student_id
                FROM files f
                JOIN submission_files sf ON sf.file_id = f.id
                JOIN submissions s ON s.id = sf.submission_id
                JOIN assignments ass ON ass.activity_id = s.assignment_id
                JOIN activities a ON a.id = ass.activity_id
                JOIN course_sections cs ON cs.id = a.section_id
                JOIN courses c ON c.id = cs.course_id
                WHERE f.id = ? AND s.id = ? AND cs.course_id = ?
                """, (resultSet, rowNumber) -> new FileRecord(
                resultSet.getLong("id"), resultSet.getString("storage_key"),
                resultSet.getString("original_name"), resultSet.getString("mime_type"),
                resultSet.getLong("size_bytes"), resultSet.getString("course_status"),
                resultSet.getString("section_status"), resultSet.getString("activity_status"),
                resultSet.getLong("student_id")
        ), fileId, submissionId, courseId);
        return files.stream().findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp bài nộp"));
    }

    public Map<Long, List<CourseFileView>> findSubmissionFilesForIds(List<Long> submissionIds) {
        return findSubmissionFiles(submissionIds);
    }

    public Map<Long, List<CourseFileView>> findActivityFilesForIds(List<Long> activityIds) {
        return findActivityFiles(activityIds);
    }

    private Map<Long, List<CourseFileView>> findActivityFiles(List<Long> activityIds) {
        if (activityIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = placeholders(activityIds.size());
        List<ActivityFileRow> rows = jdbcTemplate.query("""
                SELECT af.activity_id, f.id, f.original_name, f.mime_type, f.size_bytes
                FROM activity_files af
                JOIN files f ON f.id = af.file_id
                WHERE af.activity_id IN (""" + placeholders + ") ORDER BY af.position, f.id",
                (resultSet, rowNumber) -> new ActivityFileRow(
                        resultSet.getLong("activity_id"),
                        toFileView(resultSet)
                ),
                activityIds.toArray()
        );
        Map<Long, List<CourseFileView>> result = new HashMap<>();
        rows.forEach(row -> result.computeIfAbsent(row.activityId(), ignored -> new ArrayList<>())
                .add(row.file()));
        return result;
    }

    private Map<Long, List<CourseFileView>> findSubmissionFiles(List<Long> submissionIds) {
        if (submissionIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = placeholders(submissionIds.size());
        List<SubmissionFileRow> rows = jdbcTemplate.query("""
                SELECT sf.submission_id, f.id, f.original_name, f.mime_type, f.size_bytes
                FROM submission_files sf
                JOIN files f ON f.id = sf.file_id
                WHERE sf.submission_id IN (""" + placeholders + ") ORDER BY f.id",
                (resultSet, rowNumber) -> new SubmissionFileRow(
                        resultSet.getLong("submission_id"),
                        toFileView(resultSet)
                ),
                submissionIds.toArray()
        );
        Map<Long, List<CourseFileView>> result = new HashMap<>();
        rows.forEach(row -> result.computeIfAbsent(row.submissionId(), ignored -> new ArrayList<>())
                .add(row.file()));
        return result;
    }

    private long createActivity(
            Long courseId, Long sectionId, Long teacherId, String type, String title, String description
    ) {
        Integer sectionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_sections WHERE id = ? AND course_id = ?",
                Integer.class, sectionId, courseId);
        if (sectionCount == null || sectionCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chương trong khóa học");
        }
        Integer position = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(position), 0) + 1 FROM activities WHERE section_id = ?",
                Integer.class, sectionId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO activities
                        (section_id, created_by, activity_type, title, description, position, status)
                    VALUES (?, ?, ?, ?, ?, ?, 'VISIBLE')
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, sectionId);
            statement.setLong(2, teacherId);
            statement.setString(3, type);
            statement.setString(4, title.trim());
            statement.setString(5, description);
            statement.setInt(6, position == null ? 1 : position);
            return statement;
        }, keyHolder);
        return requireGeneratedId(keyHolder);
    }

    private static CourseFileView toFileView(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new CourseFileView(
                resultSet.getLong("id"),
                resultSet.getString("original_name"),
                resultSet.getString("mime_type"),
                resultSet.getLong("size_bytes")
        );
    }

    private static String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private static long requireGeneratedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Database did not return a generated ID");
        }
        return key.longValue();
    }

    private static LocalDateTime localDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static Long nullableLong(long value, boolean wasNull) {
        return wasNull ? null : value;
    }

    private static Integer nullableInteger(int value, boolean wasNull) {
        return wasNull ? null : value;
    }

    private record AssignmentRow(
            Long id,
            String title,
            String description,
            Integer position,
            String status,
            LocalDateTime dueAt,
            BigDecimal maxGrade,
            boolean allowLateSubmission,
            Long submissionId,
            Integer attemptNumber,
            String submissionStatus,
            LocalDateTime submittedAt
    ) {
    }

    private record ResourceRow(Long id, String title, String description, Integer position, String status) {
    }

    private record ActivityFileRow(Long activityId, CourseFileView file) {
    }

    private record SubmissionFileRow(Long submissionId, CourseFileView file) {
    }

    private record SubmissionWindow(
            String courseStatus,
            String sectionStatus,
            String activityStatus,
            LocalDateTime dueAt,
            boolean allowLateSubmission
    ) {
    }

    public record FileRecord(
            Long id,
            String storageKey,
            String originalName,
            String mimeType,
            long sizeBytes,
            String courseStatus,
            String sectionStatus,
            String activityStatus,
            Long studentId
    ) {
    }
}
