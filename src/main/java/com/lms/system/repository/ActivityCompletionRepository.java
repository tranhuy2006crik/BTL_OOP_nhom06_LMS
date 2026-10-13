package com.lms.system.repository;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class ActivityCompletionRepository {

    private final JdbcTemplate jdbcTemplate;

    public ActivityCompletionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<Long> findCompletedActivityIds(Long courseId, Long sectionId, Long userId) {
        List<Long> activityIds = jdbcTemplate.query("""
                SELECT a.id
                FROM activities a
                JOIN course_sections cs ON cs.id = a.section_id
                JOIN activity_completions ac ON ac.activity_id = a.id AND ac.user_id = ?
                WHERE cs.course_id = ? AND cs.id = ?
                  AND cs.status = 'VISIBLE' AND a.status = 'VISIBLE'
                  AND ac.status = 'COMPLETED'
                """, (resultSet, rowNumber) -> resultSet.getLong("id"), userId, courseId, sectionId);
        return Set.copyOf(new HashSet<>(activityIds));
    }

    public void setCompleted(Long courseId, Long sectionId, Long activityId, Long userId, boolean completed) {
        List<String> activityTypes = jdbcTemplate.query("""
                SELECT a.activity_type
                FROM activities a
                JOIN course_sections cs ON cs.id = a.section_id
                JOIN courses c ON c.id = cs.course_id
                WHERE a.id = ? AND cs.id = ? AND cs.course_id = ?
                  AND a.status = 'VISIBLE' AND cs.status = 'VISIBLE'
                  AND c.status = 'PUBLISHED'
                """, (resultSet, rowNumber) -> resultSet.getString("activity_type"),
                activityId, sectionId, courseId);
        if (activityTypes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hoạt động trong khóa học");
        }
        if ("ASSIGNMENT".equals(activityTypes.get(0))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Bài tập chỉ được đánh dấu hoàn thành sau khi nộp bài");
        }

        if (completed) {
            jdbcTemplate.update("""
                    INSERT INTO activity_completions (activity_id, user_id, status, completed_at)
                    VALUES (?, ?, 'COMPLETED', CURRENT_TIMESTAMP)
                    ON DUPLICATE KEY UPDATE
                        status = 'COMPLETED',
                        completed_at = CURRENT_TIMESTAMP
                    """, activityId, userId);
        } else {
            jdbcTemplate.update("""
                    INSERT INTO activity_completions (activity_id, user_id, status, completed_at)
                    VALUES (?, ?, 'IN_PROGRESS', NULL)
                    ON DUPLICATE KEY UPDATE
                        status = 'IN_PROGRESS',
                        completed_at = NULL
                    """, activityId, userId);
        }
    }
}
