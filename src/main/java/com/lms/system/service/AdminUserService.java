package com.lms.system.service;

import com.lms.system.dto.admin.AdminUserForm;
import com.lms.system.exception.DuplicateEmailException;
import com.lms.system.exception.ResourceNotFoundException;
import com.lms.system.model.PasswordHasher;
import com.lms.system.model.Role;
import com.lms.system.model.RoleCode;
import com.lms.system.model.RoleScope;
import com.lms.system.model.SystemRoleAssignment;
import com.lms.system.model.SystemRoleAssignmentId;
import com.lms.system.model.User;
import com.lms.system.model.UserStatus;
import com.lms.system.repository.RoleRepository;
import com.lms.system.repository.SystemRoleAssignmentRepository;
import com.lms.system.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SystemRoleAssignmentRepository assignmentRepository;
    private final PasswordHasher passwordHasher;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            SystemRoleAssignmentRepository assignmentRepository,
            PasswordHasher passwordHasher
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional(readOnly = true)
    public Page<User> search(String query, UserStatus status, Pageable pageable) {
        return userRepository.searchAdminUsers(query.trim(), status, pageable);
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long countUsers(UserStatus status) {
        return userRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public User get(Long userId) {
        return userRepository.findWithAccessById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
    }

    @Transactional
    public User create(AdminUserForm form) {
        String email = User.normalizeEmail(form.getEmail());
        ensureEmailAvailable(email, null);
        User.validateRawPassword(form.getPassword());

        User user = User.register(email, form.getFullName(), passwordHasher.hash(form.getPassword()));
        user = userRepository.save(user);
        if (form.isAdmin()) {
            addAdminRole(user);
        }
        return user;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public User update(Long userId, AdminUserForm form, Long actorId) {
        User user = get(userId);
        if (user.getStatus() == UserStatus.DELETED) {
            throw new IllegalArgumentException("Deleted accounts cannot be edited");
        }
        String email = User.normalizeEmail(form.getEmail());
        ensureEmailAvailable(email, userId);
        user.updateAccount(email, form.getFullName());
        userRepository.save(user);
        synchronizeAdminRole(user, form.isAdmin(), actorId);
        return get(userId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void suspend(Long userId, Long actorId) {
        User user = get(userId);
        ensureNotSelf(userId, actorId);
        if (user.getStatus() == UserStatus.DELETED) {
            throw new IllegalArgumentException("Deleted accounts cannot be suspended");
        }
        if (user.isSystemAdmin() && user.getStatus() == UserStatus.ACTIVE) {
            lockAdminRole();
            protectLastActiveAdmin(user);
        }
        user.suspend();
        userRepository.save(user);
    }

    @Transactional
    public void activate(Long userId) {
        User user = get(userId);
        if (user.getStatus() == UserStatus.DELETED) {
            throw new IllegalArgumentException("Deleted accounts cannot be reactivated");
        }
        user.activate();
        userRepository.save(user);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Long userId, Long actorId) {
        User user = get(userId);
        ensureNotSelf(userId, actorId);
        if (user.isSystemAdmin() && user.getStatus() == UserStatus.ACTIVE) {
            lockAdminRole();
            protectLastActiveAdmin(user);
        }
        user.markDeleted();
        userRepository.save(user);
    }

    private void synchronizeAdminRole(User user, boolean shouldBeAdmin, Long actorId) {
        Role adminRole = adminRole();
        boolean hasAdminRole = user.isSystemAdmin();
        if (shouldBeAdmin && !hasAdminRole) {
            SystemRoleAssignment assignment = SystemRoleAssignment.bind(user, adminRole);
            assignmentRepository.save(assignment);
            user.attachSystemRole(assignment);
        } else if (!shouldBeAdmin && hasAdminRole) {
            ensureNotSelf(user.getId(), actorId);
            lockAdminRole();
            protectLastActiveAdmin(user);
            assignmentRepository.deleteById(new SystemRoleAssignmentId(user.getId(), adminRole.getId()));
            user.detachSystemRole(adminRole);
        }
    }

    private void addAdminRole(User user) {
        Role role = adminRole();
        SystemRoleAssignment assignment = SystemRoleAssignment.bind(user, role);
        assignmentRepository.save(assignment);
        user.attachSystemRole(assignment);
    }

    private Role adminRole() {
        Role role = roleRepository.findByCode(RoleCode.ADMIN.name())
                .orElseThrow(() -> new IllegalStateException("The system ADMIN role is not configured"));
        if (role.getScope() != RoleScope.SYSTEM) {
            throw new IllegalStateException("The ADMIN role must have SYSTEM scope");
        }
        return role;
    }

    private Role lockAdminRole() {
        Role role = roleRepository.lockSystemAdminRole()
                .orElseThrow(() -> new IllegalStateException("The system ADMIN role is not configured"));
        if (role.getScope() != RoleScope.SYSTEM) {
            throw new IllegalStateException("The ADMIN role must have SYSTEM scope");
        }
        return role;
    }

    private void ensureEmailAvailable(String email, Long currentUserId) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new DuplicateEmailException(email);
            }
        });
    }

    private void protectLastActiveAdmin(User user) {
        if (user.getStatus() == UserStatus.ACTIVE
                && user.isSystemAdmin()
                && userRepository.countActiveUsersWithSystemRole(RoleCode.ADMIN.name()) <= 1) {
            throw new IllegalArgumentException("The last active administrator cannot be suspended, deleted, or demoted");
        }
    }

    private static void ensureNotSelf(Long userId, Long actorId) {
        if (userId.equals(actorId)) {
            throw new IllegalArgumentException("You cannot suspend, delete, or remove administrator access from your own account");
        }
    }
}
