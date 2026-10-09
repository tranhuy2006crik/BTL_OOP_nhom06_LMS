package com.lms.system.model;

public enum Permission {
    VIEW_OWN_PROFILE(true),
    EDIT_OWN_PROFILE(true),
    CHANGE_OWN_PASSWORD(true),

    MANAGE_USERS(false),
    CREATE_COURSE(false),

    VIEW_COURSE(false),
    MANAGE_COURSE_CONTENT(false),
    PUBLISH_ANNOUNCEMENT(false),
    GRADE_SUBMISSION(false),
    VIEW_ROSTER(false),

    SUBMIT_ASSIGNMENT(false),
    VIEW_OWN_GRADES(false);

    private final boolean selfService;

    Permission(boolean selfService) {
        this.selfService = selfService;
    }

    public boolean isSelfService() {
        return selfService;
    }
}
