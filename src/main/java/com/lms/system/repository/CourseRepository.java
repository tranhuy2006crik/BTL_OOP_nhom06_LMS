package com.lms.system.repository;

import com.lms.system.model.Course;
import com.lms.system.model.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByCourseCodeIgnoreCase(String courseCode);

    boolean existsByCategoryId(Long categoryId);

    long countByStatus(CourseStatus status);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findWithCategoryById(@Param("id") Long id);

    @Query(value = """
            SELECT c FROM Course c
            LEFT JOIN FETCH c.category
            WHERE (:query = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:status IS NULL OR c.status = :status)
            ORDER BY LOWER(c.title), c.id
            """,
            countQuery = """
                    SELECT COUNT(c) FROM Course c
                    WHERE (:query = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :query, '%')))
                      AND (:status IS NULL OR c.status = :status)
                    """)
    Page<Course> searchAdminCourses(
            @Param("query") String query,
            @Param("status") CourseStatus status,
            Pageable pageable
    );
}
