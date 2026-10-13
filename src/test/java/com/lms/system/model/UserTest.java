package com.lms.system.model;

import com.lms.system.exception.InvalidPasswordException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private final PasswordHasher hasher = new PasswordHasher() {
        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return passwordHash.equals("hashed:" + rawPassword);
        }
    };

    @Test
    void registerNormalizesEmailAndAllowsLogin() {
        User user = User.register("  A@Mail.com ", "Nguyen Van A", "hashed:secret12");
        assertEquals("a@mail.com", user.getEmail());
        assertTrue(user.canLogin());
        assertTrue(user.hasPermission(Permission.EDIT_OWN_PROFILE, null));
        assertFalse(user.hasPermission(Permission.MANAGE_USERS, null));
    }

    @Test
    void suspendedUserCannotLoginOrUsePermissions() {
        User user = User.register("a@mail.com", "A", "hashed:secret12");
        user.suspend();
        assertFalse(user.canLogin());
        assertFalse(user.hasPermission(Permission.VIEW_OWN_PROFILE, null));
    }

    @Test
    void teacherMembershipIsCourseScoped() {
        User user = User.register("gv@mail.com", "Giao Vien", "hashed:secret12");
        Role teacher = Role.catalog("TEACHER", "Teacher", RoleScope.COURSE);
        user.attachCourseMembership(CourseMembership.bind(user, 11L, teacher, CourseMembershipStatus.ACTIVE));

        assertTrue(user.hasPermission(Permission.GRADE_SUBMISSION, 11L));
        assertFalse(user.hasPermission(Permission.GRADE_SUBMISSION, 99L));
        assertFalse(user.hasPermission(Permission.MANAGE_USERS, 11L));
    }

    @Test
    void withdrawnMembershipDoesNotGrantAccess() {
        User user = User.register("sv@mail.com", "Sinh Vien", "hashed:secret12");
        Role student = Role.catalog("STUDENT", "Student", RoleScope.COURSE);
        user.attachCourseMembership(CourseMembership.bind(user, 5L, student, CourseMembershipStatus.WITHDRAWN));
        assertFalse(user.hasPermission(Permission.SUBMIT_ASSIGNMENT, 5L));
    }

    @Test
    void changePasswordChecksCurrentAndStrength() {
        User user = User.register("a@mail.com", "A", "hashed:secret12");
        assertThrows(InvalidPasswordException.class,
                () -> user.changePassword("wrong", "newpass99", hasher));
        user.changePassword("secret12", "newpass99", hasher);
        assertTrue(hasher.matches("newpass99", user.getPasswordHash()));
    }

    @Test
    void accountUpdateValidatesAllFieldsBeforeChangingState() {
        User user = User.register("old@example.com", "Old Name", "hashed:secret12");

        assertThrows(IllegalArgumentException.class,
                () -> user.updateAccount("new@example.com", " "));

        assertEquals("old@example.com", user.getEmail());
        assertEquals("Old Name", user.getFullName());
    }

}
