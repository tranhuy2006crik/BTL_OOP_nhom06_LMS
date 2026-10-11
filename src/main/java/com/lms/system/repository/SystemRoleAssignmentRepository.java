package com.lms.system.repository;

import com.lms.system.model.SystemRoleAssignment;
import com.lms.system.model.SystemRoleAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemRoleAssignmentRepository
        extends JpaRepository<SystemRoleAssignment, SystemRoleAssignmentId> {

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}
