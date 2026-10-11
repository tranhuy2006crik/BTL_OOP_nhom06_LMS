package com.lms.system.repository;

import com.lms.system.model.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    List<AssignmentSubmission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
}
