package com.lms.system.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.lms.system.dto.AnnouncementResponse;
import com.lms.system.dto.EnrollmentResponse;
import com.lms.system.dto.LessonResponse;

@Repository
public class LmsWorkflowRepository {

    private final JdbcTemplate jdbcTemplate;

    public LmsWorkflowRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Lấy ID nội bộ từ email trong phiên đăng nhập; không nhận user ID do client tự gửi.
    public Long requireActiveUserId(String email) {
        List<Long> userIds = jdbcTemplate.query(
                "SELECT id FROM users WHERE email = ? AND status = 'ACTIVE'",
                (resultSet, rowNumber) -> resultSet.getLong("id"),
                email
        );
        if (userIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không hợp lệ hoặc đã bị khóa");
        }
        return userIds.get(0);
    }

    // Cho phép quản lý nếu user là giảng viên của khóa hoặc admin toàn hệ thống.
    public boolean canManageCourse(Long userId, Long courseId) {
        Boolean allowed = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM course_memberships cm
                    JOIN roles r ON r.id = cm.role_id AND r.scope = cm.role_scope
                    WHERE cm.user_id = ? AND cm.course_id = ?
                      AND cm.status = 'ACTIVE' AND r.code = 'TEACHER'
                    UNION ALL
                    SELECT 1
                    FROM system_role_assignments sra
                    JOIN roles r ON r.id = sra.role_id AND r.scope = sra.role_scope
                    WHERE sra.user_id = ? AND r.code = 'ADMIN' AND r.scope = 'SYSTEM'
                )
                """, Boolean.class, userId, courseId, userId);
        return Boolean.TRUE.equals(allowed);
    }

    // Membership phải đang ACTIVE và có role STUDENT thì mới được xem nội dung dành cho học viên.
    public boolean isActiveStudent(Long userId, Long courseId) {
        Boolean enrolled = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM course_memberships cm
                    JOIN roles r ON r.id = cm.role_id AND r.scope = cm.role_scope
                    WHERE cm.user_id = ? AND cm.course_id = ?
                      AND cm.status = 'ACTIVE' AND r.code = 'STUDENT'
                )
                """, Boolean.class, userId, courseId);
        return Boolean.TRUE.equals(enrolled);
    }

    // Chỉ khóa học đã công bố mới nhận ghi danh.
    public void requirePublishedCourse(Long courseId) {
        List<String> statuses = jdbcTemplate.query(
                "SELECT status FROM courses WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getString("status"),
                courseId
        );
        if (statuses.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học");
        }
        if (!"PUBLISHED".equals(statuses.get(0))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khóa học chưa mở ghi danh");
        }
    }

    // Kiểm tra section thuộc đúng course để không thao tác chéo giữa các khóa học.
    public void requireSectionInCourse(Long sectionId, Long courseId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_sections WHERE id = ? AND course_id = ?",
                Integer.class,
                sectionId,
                courseId
        );
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chương trong khóa học");
        }
    }

    // Metadata nằm trong activities, còn nội dung bài học nằm trong bảng lessons.
    // Học viên chỉ thấy section và activity đang hiển thị; người quản lý xem được cả nội dung ẩn.
    public List<LessonResponse> findLessons(Long courseId, Long sectionId, boolean canManage) {
        String visibilityFilter = canManage ? "" : " AND a.status = 'VISIBLE' AND s.status = 'VISIBLE'";
        return jdbcTemplate.query("""
                SELECT a.id, a.title, a.description, l.content, a.position, a.status,
                       a.created_at, a.updated_at
                FROM activities a
                JOIN lessons l ON l.activity_id = a.id
                JOIN course_sections s ON s.id = a.section_id
                WHERE s.course_id = ? AND s.id = ? AND a.activity_type = 'LESSON'
                """ + visibilityFilter + " ORDER BY a.position, a.id",
                (resultSet, rowNumber) -> new LessonResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("title"),
                        resultSet.getString("description"),
                        resultSet.getString("content"),
                        resultSet.getInt("position"),
                        resultSet.getString("status"),
                        resultSet.getTimestamp("created_at").toLocalDateTime(),
                        resultSet.getTimestamp("updated_at").toLocalDateTime()
                ),
                courseId,
                sectionId
        );
    }

    public LessonResponse createLesson(
            Long courseId,
            Long sectionId,
            Long teacherId,
            String title,
            String description,
            String content
    ) {
        // Tạo activity trước để lấy ID, sau đó dùng ID đó làm khóa chính của lessons.
        // LessonService gọi hàm này trong transaction để hai thao tác cùng thành công hoặc cùng rollback.
        Integer position = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(position), 0) + 1 FROM activities WHERE section_id = ?",
                Integer.class,
                sectionId
        );
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO activities
                        (section_id, created_by, activity_type, title, description, position, status)
                    VALUES (?, ?, 'LESSON', ?, ?, ?, 'VISIBLE')
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, sectionId);
            statement.setLong(2, teacherId);
            statement.setString(3, title.trim());
            statement.setString(4, description);
            statement.setInt(5, position == null ? 1 : position);
            return statement;
        }, keyHolder);
        long lessonId = requireGeneratedId(keyHolder);
        jdbcTemplate.update("INSERT INTO lessons (activity_id, content) VALUES (?, ?)", lessonId, content);
        return findLesson(courseId, sectionId, lessonId, true);
    }

    public LessonResponse updateLesson(
            Long courseId,
            Long sectionId,
            Long lessonId,
            String title,
            String description,
            String content
    ) {
        // Điều kiện course/section/type bảo đảm ID bài học thuộc đúng vị trí cần sửa.
        int updated = jdbcTemplate.update("""
                UPDATE activities a
                JOIN course_sections s ON s.id = a.section_id
                SET a.title = ?, a.description = ?, a.updated_at = CURRENT_TIMESTAMP
                WHERE a.id = ? AND a.activity_type = 'LESSON'
                  AND a.section_id = ? AND s.course_id = ?
                """, title.trim(), description, lessonId, sectionId, courseId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài học");
        }
        jdbcTemplate.update("UPDATE lessons SET content = ? WHERE activity_id = ?", content, lessonId);
        return findLesson(courseId, sectionId, lessonId, true);
    }

    public void hideLesson(Long courseId, Long sectionId, Long lessonId) {
        // Ẩn thay vì xóa để giữ lịch sử và các liên kết tới activity.
        int updated = jdbcTemplate.update("""
                UPDATE activities a
                JOIN course_sections s ON s.id = a.section_id
                SET a.status = 'HIDDEN', a.updated_at = CURRENT_TIMESTAMP
                WHERE a.id = ? AND a.activity_type = 'LESSON'
                  AND a.section_id = ? AND s.course_id = ?
                """, lessonId, sectionId, courseId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài học");
        }
    }

    private LessonResponse findLesson(Long courseId, Long sectionId, Long lessonId, boolean canManage) {
        // Dùng chung bộ lọc quyền hiển thị để bài học vừa tạo/sửa được đọc theo cùng quy tắc với danh sách.
        return findLessons(courseId, sectionId, canManage).stream()
                .filter(lesson -> lesson.id().equals(lessonId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài học"));
    }

    public EnrollmentResponse enroll(Long courseId, Long userId) {
        // Khóa chính ghép course_id + user_id chỉ cho phép một membership mỗi khóa.
        List<String> statuses = jdbcTemplate.query(
                "SELECT status FROM course_memberships WHERE course_id = ? AND user_id = ?",
                (resultSet, rowNumber) -> resultSet.getString("status"),
                courseId,
                userId
        );
        if (!statuses.isEmpty()) {
            String status = statuses.get(0);
            // Không tạo bản ghi trùng; chỉ cho ghi danh lại khi membership cũ đã rút khỏi khóa.
            if ("ACTIVE".equals(status)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã ghi danh khóa học này");
            }
            if (!"WITHDRAWN".equals(status)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể ghi danh khi trạng thái hiện tại là " + status);
            }
            jdbcTemplate.update("""
                    UPDATE course_memberships
                    SET status = 'ACTIVE', joined_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                    WHERE course_id = ? AND user_id = ? AND status = 'WITHDRAWN'
                    """, courseId, userId);
        } else {
            // Tra role theo code/scope thay vì giả định role_id cố định trong database.
            Integer roleCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM roles WHERE code = 'STUDENT' AND scope = 'COURSE'",
                    Integer.class
            );
            if (roleCount == null || roleCount == 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Chưa cấu hình role STUDENT");
            }
            jdbcTemplate.update("""
                    INSERT INTO course_memberships (course_id, user_id, role_id, role_scope, status)
                    SELECT ?, ?, id, 'COURSE', 'ACTIVE'
                    FROM roles WHERE code = 'STUDENT' AND scope = 'COURSE'
                    """, courseId, userId);
        }
        return findEnrollment(courseId, userId);
    }

    private EnrollmentResponse findEnrollment(Long courseId, Long userId) {
        // Đọc lại membership sau insert/update để response phản ánh trạng thái trong database.
        return jdbcTemplate.queryForObject("""
                SELECT course_id, user_id, status, joined_at
                FROM course_memberships WHERE course_id = ? AND user_id = ?
                """, (resultSet, rowNumber) -> new EnrollmentResponse(
                resultSet.getLong("course_id"),
                resultSet.getLong("user_id"),
                resultSet.getString("status"),
                resultSet.getTimestamp("joined_at").toLocalDateTime()
        ), courseId, userId);
    }

    public AnnouncementResponse createAnnouncement(Long courseId, Long authorId, String title, String content) {
        // Đăng trực tiếp ở trạng thái PUBLISHED và lưu thời điểm để học viên có thể xem.
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO course_announcements
                        (course_id, author_id, title, content, status, published_at)
                    VALUES (?, ?, ?, ?, 'PUBLISHED', CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, courseId);
            statement.setLong(2, authorId);
            statement.setString(3, title.trim());
            statement.setString(4, content);
            return statement;
        }, keyHolder);
        long announcementId = requireGeneratedId(keyHolder);
        return findAnnouncement(courseId, announcementId);
    }

    public List<AnnouncementResponse> findAnnouncements(Long courseId, boolean canManage) {
        // Người học chỉ xem thông báo đã đăng; người quản lý xem cả bản nháp nếu có.
        String visibilityFilter = canManage ? "" : " AND status = 'PUBLISHED'";
        return jdbcTemplate.query("""
                SELECT id, course_id, author_id, title, content, status, published_at, updated_at
                FROM course_announcements WHERE course_id = ?
                """ + visibilityFilter + " ORDER BY COALESCE(published_at, updated_at) DESC, id DESC",
                (resultSet, rowNumber) -> new AnnouncementResponse(
                        resultSet.getLong("id"),
                        resultSet.getLong("course_id"),
                        resultSet.getLong("author_id"),
                        resultSet.getString("title"),
                        resultSet.getString("content"),
                        resultSet.getString("status"),
                        toLocalDateTime(resultSet.getTimestamp("published_at")),
                        resultSet.getTimestamp("updated_at").toLocalDateTime()
                ),
                courseId
        );
    }

    private AnnouncementResponse findAnnouncement(Long courseId, Long announcementId) {
        return jdbcTemplate.queryForObject("""
                SELECT id, course_id, author_id, title, content, status, published_at, updated_at
                FROM course_announcements WHERE course_id = ? AND id = ?
                """, (resultSet, rowNumber) -> new AnnouncementResponse(
                resultSet.getLong("id"),
                resultSet.getLong("course_id"),
                resultSet.getLong("author_id"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getString("status"),
                toLocalDateTime(resultSet.getTimestamp("published_at")),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        ), courseId, announcementId);
    }

    private long requireGeneratedId(KeyHolder keyHolder) {
        // INSERT phải trả khóa chính để liên kết bản ghi subtype hoặc dựng response.
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Database did not return a generated ID");
        }
        return key.longValue();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        // published_at có thể null với thông báo nháp.
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}