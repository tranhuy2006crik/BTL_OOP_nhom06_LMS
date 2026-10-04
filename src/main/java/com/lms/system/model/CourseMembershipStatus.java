package com.lms.system.model;

public enum CourseMembershipStatus {
    ACTIVE,
    SUSPENDED,
    WITHDRAWN;

    public boolean grantsCourseAccess() {
        return this == ACTIVE;
    }
}
