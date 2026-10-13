package com.lms.system.repository;

import com.lms.system.model.CourseMembership;
import com.lms.system.model.CourseMembershipId;
import com.lms.system.model.RoleScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseMembershipRepository extends JpaRepository<CourseMembership, CourseMembershipId> {

    @Query("""
            SELECT membership FROM CourseMembership membership
            JOIN FETCH membership.role role
            WHERE membership.courseId = :courseId
              AND membership.roleScope = com.lms.system.model.RoleScope.COURSE
              AND role.code = 'TEACHER'
            """)
    List<CourseMembership> findTeacherMembershipsByCourseId(@Param("courseId") Long courseId);
}
