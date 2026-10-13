package com.lms.system.model.access;

import com.lms.system.model.Role;
import com.lms.system.model.RoleCode;

public final class RoleAccessFactory {

    private RoleAccessFactory() {
    }

    public static RoleAccess from(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        return fromCode(role.roleCode());
    }

    public static RoleAccess fromCode(RoleCode roleCode) {
        return switch (roleCode) {
            case ADMIN -> new SystemAdminAccess();
            case TEACHER -> new CourseTeacherAccess();
            case STUDENT -> new CourseStudentAccess();
        };
    }
}
