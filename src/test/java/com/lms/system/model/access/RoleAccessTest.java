package com.lms.system.model.access;

import com.lms.system.model.Permission;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleAccessTest {

    @Test
    void adminCanManageUsersWithoutCourse() {
        RoleAccess admin = new SystemAdminAccess();
        assertTrue(admin.grants(Permission.MANAGE_USERS, AccessContext.system()));
        assertTrue(admin.grants(Permission.GRADE_SUBMISSION, AccessContext.course(10L)));
    }

    @Test
    void teacherCannotActAtSystemScope() {
        RoleAccess teacher = new CourseTeacherAccess();
        assertFalse(teacher.grants(Permission.MANAGE_USERS, AccessContext.system()));
        assertFalse(teacher.grants(Permission.GRADE_SUBMISSION, AccessContext.system()));
        assertTrue(teacher.grants(Permission.GRADE_SUBMISSION, AccessContext.course(3L)));
        assertFalse(teacher.grants(Permission.SUBMIT_ASSIGNMENT, AccessContext.course(3L)));
    }

    @Test
    void studentCanSubmitInCourseOnly() {
        RoleAccess student = new CourseStudentAccess();
        assertTrue(student.grants(Permission.SUBMIT_ASSIGNMENT, AccessContext.course(8L)));
        assertFalse(student.grants(Permission.GRADE_SUBMISSION, AccessContext.course(8L)));
        assertFalse(student.grants(Permission.SUBMIT_ASSIGNMENT, AccessContext.system()));
    }
}
