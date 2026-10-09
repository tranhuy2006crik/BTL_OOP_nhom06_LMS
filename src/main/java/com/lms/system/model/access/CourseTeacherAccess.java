package com.lms.system.model.access;

import com.lms.system.model.Permission;
import com.lms.system.model.RoleCode;

import java.util.EnumSet;
import java.util.Set;

public final class CourseTeacherAccess extends RoleAccess {

    private static final Set<Permission> TEACHER_PERMISSIONS = EnumSet.of(
            Permission.VIEW_COURSE,
            Permission.MANAGE_COURSE_CONTENT,
            Permission.PUBLISH_ANNOUNCEMENT,
            Permission.GRADE_SUBMISSION,
            Permission.VIEW_ROSTER,
            Permission.VIEW_OWN_GRADES
    );

    @Override
    public RoleCode roleCode() {
        return RoleCode.TEACHER;
    }

    @Override
    protected boolean appliesTo(AccessContext context) {
        return context.isCourseScoped();
    }

    @Override
    protected Set<Permission> allowedPermissions() {
        return TEACHER_PERMISSIONS;
    }
}
