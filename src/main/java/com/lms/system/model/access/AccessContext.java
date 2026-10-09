package com.lms.system.model.access;

public record AccessContext(Long courseId) {

    public static AccessContext system() {
        return new AccessContext(null);
    }

    public static AccessContext course(Long courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("courseId is required for course context");
        }
        return new AccessContext(courseId);
    }

    public boolean isCourseScoped() {
        return courseId != null;
    }
}
