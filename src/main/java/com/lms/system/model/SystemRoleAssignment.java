package com.lms.system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_role_assignments")
@IdClass(SystemRoleAssignmentId.class)
public class SystemRoleAssignment {

    @Id
    @Column(name = "user_id", columnDefinition = "BIGINT UNSIGNED")
    private Long userId;

    @Id
    @Column(name = "role_id", columnDefinition = "BIGINT UNSIGNED")
    private Long roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_scope", nullable = false, length = 10)
    private RoleScope roleScope = RoleScope.SYSTEM;

    @Column(name = "assigned_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime assignedAt;

    protected SystemRoleAssignment() {
    }

    public static SystemRoleAssignment bind(User user, Role role) {
        SystemRoleAssignment assignment = new SystemRoleAssignment();
        assignment.user = user;
        assignment.role = role;
        assignment.roleId = role.getId();
        assignment.userId = user.getId();
        assignment.roleScope = RoleScope.SYSTEM;
        assignment.assignedAt = LocalDateTime.now();
        return assignment;
    }

    @PrePersist
    void onCreate() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
        if (roleScope == null) {
            roleScope = RoleScope.SYSTEM;
        }
    }

    public Role getRole() {
        return role;
    }

    public RoleScope getRoleScope() {
        return roleScope;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
}
