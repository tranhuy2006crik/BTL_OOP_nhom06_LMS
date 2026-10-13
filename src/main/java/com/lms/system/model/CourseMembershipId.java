package com.lms.system.model;

import java.io.Serializable;
import java.util.Objects;

public class CourseMembershipId implements Serializable {

    private Long courseId;
    private Long userId;

    public CourseMembershipId() {
    }

    public CourseMembershipId(Long courseId, Long userId) {
        this.courseId = courseId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CourseMembershipId that)) {
            return false;
        }
        return Objects.equals(courseId, that.courseId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseId, userId);
    }
}
