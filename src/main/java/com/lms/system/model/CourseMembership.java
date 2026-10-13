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
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_memberships")
@IdClass(CourseMembershipId.class)
public class CourseMembership {

    @Id
    @Column(name = "course_id", columnDefinition = "BIGINT UNSIGNED")
    private Long courseId;

    @Id
    @Column(name = "user_id", columnDefinition = "BIGINT UNSIGNED")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_scope", nullable = false, length = 10)
    private RoleScope roleScope = RoleScope.COURSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseMembershipStatus status = CourseMembershipStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime joinedAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime updatedAt;

    protected CourseMembership() {
    }

    public static CourseMembership bind(User user, Long courseId, Role role, CourseMembershipStatus status) {
        CourseMembership membership = new CourseMembership();
        membership.user = user;
        membership.userId = user.getId();
        membership.courseId = courseId;
        membership.role = role;
        membership.roleScope = RoleScope.COURSE;
        membership.status = status;
        return membership;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Role getRole() {
        return role;
    }

    public CourseMembershipStatus getStatus() {
        return status;
    }

    public boolean grantsCourseAccess() {
        return status != null && status.grantsCourseAccess();
    }
}
