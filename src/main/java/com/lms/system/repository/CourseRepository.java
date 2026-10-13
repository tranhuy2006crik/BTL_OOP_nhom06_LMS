package com.lms.system.repository;

import com.lms.system.model.Course;
import com.lms.system.model.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByCourseCodeIgnoreCase(String courseCode);

    boolean existsByCategoryId(Long categoryId);

    long countByStatus(CourseStatus status);

    @Query("""
            SELECT cm.courseId AS courseId, instructor.fullName AS instructorName
            FROM CourseMembership cm
            JOIN cm.user instructor
            JOIN cm.role role
            WHERE cm.courseId IN :courseIds
              AND cm.status = com.lms.system.model.CourseMembershipStatus.ACTIVE
              AND cm.roleScope = com.lms.system.model.RoleScope.COURSE
              AND role.code = 'TEACHER'
            ORDER BY cm.courseId, LOWER(instructor.fullName)
            """)
    List<CourseInstructorName> findActiveInstructorNamesByCourseIds(
            @Param("courseIds") Collection<Long> courseIds
    );

    @EntityGraph(attributePaths = "category")
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findWithCategoryById(@Param("id") Long id);

    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT DISTINCT c FROM Course c, CourseMembership cm
            JOIN cm.role role
            WHERE cm.courseId = c.id
              AND cm.userId = :userId
              AND cm.status = com.lms.system.model.CourseMembershipStatus.ACTIVE
              AND cm.roleScope = com.lms.system.model.RoleScope.COURSE
              AND role.code IN ('STUDENT', 'TEACHER')
              AND c.status <> com.lms.system.model.CourseStatus.ARCHIVED
              AND (role.code = 'TEACHER' OR c.status = com.lms.system.model.CourseStatus.PUBLISHED)
            """)
    List<Course> findCoursesForUser(@Param("userId") Long userId);

    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT c FROM Course c
            WHERE c.status = com.lms.system.model.CourseStatus.PUBLISHED
              AND NOT EXISTS (
                  SELECT cm FROM CourseMembership cm
                  WHERE cm.courseId = c.id
                    AND cm.userId = :userId
                    AND cm.status = com.lms.system.model.CourseMembershipStatus.ACTIVE
              )
            ORDER BY LOWER(c.title), c.id
            """)
    List<Course> findPublishedCoursesAvailableToUser(@Param("userId") Long userId);

    @Query("""
            SELECT cm.courseId FROM CourseMembership cm
            JOIN cm.role role
            WHERE cm.userId = :userId
              AND cm.status = com.lms.system.model.CourseMembershipStatus.ACTIVE
              AND cm.roleScope = com.lms.system.model.RoleScope.COURSE
              AND role.code = 'STUDENT'
            """)
    List<Long> findActiveStudentCourseIds(@Param("userId") Long userId);

    @Query(value = """
            SELECT c FROM Course c
            LEFT JOIN FETCH c.category
            WHERE (:query = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR EXISTS (
                       SELECT cm FROM CourseMembership cm
                       JOIN cm.user instructor
                       JOIN cm.role role
                       WHERE cm.courseId = c.id
                         AND cm.status = com.lms.system.model.CourseMembershipStatus.ACTIVE
                         AND cm.roleScope = com.lms.system.model.RoleScope.COURSE
                         AND role.code = 'TEACHER'
                         AND LOWER(instructor.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                   ))
              AND (:status IS NULL OR c.status = :status)
            ORDER BY LOWER(c.title), c.id
            """,
            countQuery = """
                    SELECT COUNT(c) FROM Course c
                    WHERE (:query = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR EXISTS (
                               SELECT cm FROM CourseMembership cm
                               JOIN cm.user instructor
                               JOIN cm.role role
                               WHERE cm.courseId = c.id
                                 AND cm.status = com.lms.system.model.CourseMembershipStatus.ACTIVE
                                 AND cm.roleScope = com.lms.system.model.RoleScope.COURSE
                                 AND role.code = 'TEACHER'
                                 AND LOWER(instructor.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                           ))
                      AND (:status IS NULL OR c.status = :status)
                    """)
    Page<Course> searchAdminCourses(
            @Param("query") String query,
            @Param("status") CourseStatus status,
            Pageable pageable
    );

    interface CourseInstructorName {
        Long getCourseId();

        String getInstructorName();
    }
}
