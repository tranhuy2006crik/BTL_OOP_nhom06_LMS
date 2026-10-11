package com.lms.system.repository;

import com.lms.system.model.CourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, Long> {

    List<CourseAssignment> findByCourseIdOrderByDueAtAsc(Long courseId);

    Optional<CourseAssignment> findByIdAndCourseId(Long id, Long courseId);
}
