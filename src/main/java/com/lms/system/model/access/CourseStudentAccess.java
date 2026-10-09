package com.lms.system.model.access;

import com.lms.system.model.Permission;
import com.lms.system.model.RoleCode;

import java.util.EnumSet;
import java.util.Set;

public final class CourseStudentAccess extends RoleAccess {

    private static final Set<Permission> STUDENT_PERMISSIONS = EnumSet.of(
            Permission.VIEW_COURSE,
            Permission.SUBMIT_ASSIGNMENT,
            Permission.VIEW_OWN_GRADES
    );

    @Override
    public RoleCode roleCode() {
        return RoleCode.STUDENT;
    }

    @Override
    protected boolean appliesTo(AccessContext context) {
        return context.isCourseScoped();
    }

    @Override
    protected Set<Permission> allowedPermissions() {
        return STUDENT_PERMISSIONS;
    }
}
