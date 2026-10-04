package com.lms.system.model;

import java.io.Serializable;
import java.util.Objects;

public class SystemRoleAssignmentId implements Serializable {

    private Long userId;
    private Long roleId;

    public SystemRoleAssignmentId() {
    }

    public SystemRoleAssignmentId(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SystemRoleAssignmentId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}
