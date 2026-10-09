package com.lms.system.model;

import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.model.access.AccessContext;
import com.lms.system.model.access.RoleAccessFactory;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final UserProfileDetails EMPTY_PROFILE_DETAILS = new UserProfileDetails(
            "COURSE_PARTICIPANTS", null, null, null, "DEFAULT", null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null
    );

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

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile profile;

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

    public void updateProfile(String newFullName, UserProfileDetails details) {
        String validatedName = requireFullName(newFullName);
        this.fullName = validatedName;
        updateProfileDetails(details);
    }

    public void updateProfileDetails(UserProfileDetails details) {
        UserProfile userProfile = ensureProfile();
        userProfile.update(details);
    }

    public void updateProfilePicture(byte[] picture, String contentType) {
        if (picture == null || picture.length == 0) {
            ensureProfile().updatePicture(null, null);
            return;
        }
        validateProfilePicture(picture, contentType);
        ensureProfile().updatePicture(picture, contentType);
    }

    private UserProfile ensureProfile() {
        if (profile == null) {
            profile = new UserProfile(this);
        }
        return profile;
    }

    private UserProfileDetails profileDetails() {
        if (profile == null) {
            return EMPTY_PROFILE_DETAILS;
        } else {
            return profile.details();
        }
    }

    public static void validateProfilePicture(byte[] picture, String contentType) {
        if (picture.length > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("Profile picture must not exceed 2 MB");
        }
        if (!isSupportedImage(picture, contentType)) {
            throw new IllegalArgumentException("Profile picture must be a JPEG, PNG, or GIF image");
        }
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

    public void resetPassword(String newRawPassword, PasswordHasher hasher) {
        if (hasher == null) {
            throw new IllegalArgumentException("PasswordHasher is required");
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

    public String getEmailVisibility() {
        return profileDetails().emailVisibility();
    }

    public String getCityTown() {
        return profileDetails().cityTown();
    }

    public String getCountry() {
        return profileDetails().country();
    }

    public String getTimezone() {
        return profileDetails().timezone();
    }

    public String getPreferredTheme() {
        return profileDetails().preferredTheme();
    }

    public String getDescription() {
        return profileDetails().description();
    }

    public String getPictureDescription() {
        return profileDetails().pictureDescription();
    }

    public String getFirstNamePhonetic() {
        return profileDetails().firstNamePhonetic();
    }

    public String getLastNamePhonetic() {
        return profileDetails().lastNamePhonetic();
    }

    public String getMiddleName() {
        return profileDetails().middleName();
    }

    public String getAlternateName() {
        return profileDetails().alternateName();
    }

    public String getInterests() {
        return profileDetails().interests();
    }

    public String getIdNumber() {
        return profileDetails().idNumber();
    }

    public String getInstitution() {
        return profileDetails().institution();
    }

    public String getDepartment() {
        return profileDetails().department();
    }

    public String getPhone() {
        return profileDetails().phone();
    }

    public String getMobilePhone() {
        return profileDetails().mobilePhone();
    }

    public String getAddress() {
        return profileDetails().address();
    }

    public String getSkypeId() {
        return profileDetails().skypeId();
    }

    public String getWebsite() {
        return profileDetails().website();
    }

    public byte[] getProfilePicture() {
        if (profile == null) {
            return null;
        } else {
            return profile.picture();
        }
    }

    public String getProfilePictureContentType() {
        if (profile == null) {
            return null;
        } else {
            return profile.pictureContentType();
        }
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

    private static boolean isSupportedImage(byte[] picture, String contentType) {
        if ("image/jpeg".equals(contentType)) {
            return picture.length >= 3
                    && (picture[0] & 0xff) == 0xff
                    && (picture[1] & 0xff) == 0xd8
                    && (picture[2] & 0xff) == 0xff;
        }
        if ("image/png".equals(contentType)) {
            byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
            return picture.length >= signature.length
                    && Arrays.equals(Arrays.copyOf(picture, signature.length), signature);
        }
        return "image/gif".equals(contentType)
                && picture.length >= 6
                && (new String(picture, 0, 6, java.nio.charset.StandardCharsets.US_ASCII).equals("GIF87a")
                || new String(picture, 0, 6, java.nio.charset.StandardCharsets.US_ASCII).equals("GIF89a"));
    }
}
