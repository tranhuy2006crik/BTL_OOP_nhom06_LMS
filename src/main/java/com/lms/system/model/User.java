package com.lms.system.model;

import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.model.access.AccessContext;
import com.lms.system.model.access.RoleAccessFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    private static final int MIN_PASSWORD_LENGTH = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user")
    private Set<SystemRoleAssignment> systemRoleAssignments = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<CourseMembership> courseMemberships = new HashSet<>();

    protected User() {
    }

    public static User register(String email, String fullName, String passwordHash) {
        User user = new User();
        user.email = normalizeEmail(email);
        user.fullName = requireFullName(fullName);
        user.passwordHash = requireHash(passwordHash);
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public boolean canLogin() {
        return status != null && status.allowsLogin();
    }

    public void updateProfile(String newFullName) {
        this.fullName = requireFullName(newFullName);
    }

    public void changePassword(String currentRawPassword, String newRawPassword, PasswordHasher hasher) {
        if (hasher == null) {
            throw new IllegalArgumentException("PasswordHasher is required");
        }
        if (!hasher.matches(currentRawPassword, passwordHash)) {
            throw new InvalidPasswordException("Mật khẩu hiện tại không đúng");
        }
        validateRawPassword(newRawPassword);
        if (hasher.matches(newRawPassword, passwordHash)) {
            throw new InvalidPasswordException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        this.passwordHash = hasher.hash(newRawPassword);
    }

    public boolean hasPermission(Permission permission, Long courseId) {
        if (!canLogin() || permission == null) {
            return false;
        }
        if (permission.isSelfService()) {
            return true;
        }
        for (SystemRoleAssignment assignment : systemRoleAssignments) {
            if (assignment.getRole() == null) {
                continue;
            }
            if (RoleAccessFactory.from(assignment.getRole()).grants(permission, AccessContext.system())) {
                return true;
            }
            if (courseId != null
                    && RoleAccessFactory.from(assignment.getRole()).grants(permission, AccessContext.course(courseId))) {
                return true;
            }
        }
        if (courseId == null) {
            return false;
        }
        return courseMemberships.stream()
                .filter(CourseMembership::grantsCourseAccess)
                .filter(membership -> courseId.equals(membership.getCourseId()))
                .filter(membership -> membership.getRole() != null)
                .anyMatch(membership -> RoleAccessFactory.from(membership.getRole())
                        .grants(permission, AccessContext.course(courseId)));
    }

    public boolean isSystemAdmin() {
        return systemRoleAssignments.stream()
                .map(SystemRoleAssignment::getRole)
                .filter(role -> role != null)
                .anyMatch(role -> role.roleCode() == RoleCode.ADMIN);
    }

    public Set<String> securityRoleNames() {
        Set<String> names = new LinkedHashSet<>();
        names.add("USER");
        if (isSystemAdmin()) {
            names.add(RoleCode.ADMIN.name());
        }
        courseMemberships.stream()
                .filter(CourseMembership::grantsCourseAccess)
                .map(CourseMembership::getRole)
                .filter(role -> role != null)
                .map(Role::roleCode)
                .map(Enum::name)
                .forEach(names::add);
        return names;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void attachSystemRole(SystemRoleAssignment assignment) {
        systemRoleAssignments.add(assignment);
    }

    public void attachCourseMembership(CourseMembership membership) {
        courseMemberships.add(membership);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Set<SystemRoleAssignment> getSystemRoleAssignments() {
        return systemRoleAssignments;
    }

    public Set<CourseMembership> getCourseMemberships() {
        return courseMemberships;
    }

    public static void validateRawPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidPasswordException("Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
        }
        boolean hasLetter = rawPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = rawPassword.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new InvalidPasswordException("Mật khẩu phải gồm cả chữ và số");
        }
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được trống");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains("@") || normalized.length() > 254) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        return normalized;
    }

    private static String requireFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên không được trống");
        }
        String trimmed = fullName.trim();
        if (trimmed.length() > 150) {
            throw new IllegalArgumentException("Họ tên tối đa 150 ký tự");
        }
        return trimmed;
    }

    private static String requireHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("password_hash is required");
        }
        return passwordHash;
    }
}
