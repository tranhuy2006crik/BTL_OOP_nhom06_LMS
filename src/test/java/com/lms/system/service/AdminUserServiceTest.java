package com.lms.system.service;

import com.lms.system.model.PasswordHasher;
import com.lms.system.model.Role;
import com.lms.system.model.RoleCode;
import com.lms.system.model.SystemRoleAssignment;
import com.lms.system.model.User;
import com.lms.system.model.UserStatus;
import com.lms.system.repository.RoleRepository;
import com.lms.system.repository.SystemRoleAssignmentRepository;
import com.lms.system.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final SystemRoleAssignmentRepository assignmentRepository = mock(SystemRoleAssignmentRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final AdminUserService service = new AdminUserService(
            userRepository, roleRepository, assignmentRepository, passwordHasher);

    @Test
    void cannotSuspendOwnAdministratorAccount() {
        User admin = administrator();
        when(userRepository.findWithAccessById(7L)).thenReturn(Optional.of(admin));

        assertThrows(IllegalArgumentException.class, () -> service.suspend(7L, 7L));

        verify(userRepository, never()).save(admin);
    }

    @Test
    void cannotSuspendTheOnlyActiveAdministrator() {
        User admin = administrator();
        when(userRepository.findWithAccessById(7L)).thenReturn(Optional.of(admin));
        when(userRepository.countActiveUsersWithSystemRole("ADMIN")).thenReturn(1L);
        Role adminRole = mock(Role.class);
        when(adminRole.getScope()).thenReturn(com.lms.system.model.RoleScope.SYSTEM);
        when(roleRepository.lockSystemAdminRole()).thenReturn(Optional.of(adminRole));

        assertThrows(IllegalArgumentException.class, () -> service.suspend(7L, 9L));

        assertEquals(UserStatus.ACTIVE, admin.getStatus());
        verify(userRepository, never()).save(admin);
    }

    @Test
    void deletedAccountsCannotBeReactivated() {
        User deleted = User.register("deleted@example.com", "Deleted User", "hash");
        deleted.markDeleted();
        when(userRepository.findWithAccessById(12L)).thenReturn(Optional.of(deleted));

        assertThrows(IllegalArgumentException.class, () -> service.activate(12L));

        assertEquals(UserStatus.DELETED, deleted.getStatus());
        verify(userRepository, never()).save(deleted);
    }

    private static User administrator() {
        User user = User.register("admin@example.com", "Admin User", "hash");
        Role adminRole = mock(Role.class);
        when(adminRole.roleCode()).thenReturn(RoleCode.ADMIN);
        SystemRoleAssignment assignment = mock(SystemRoleAssignment.class);
        when(assignment.getRole()).thenReturn(adminRole);
        user.attachSystemRole(assignment);
        return user;
    }
}
