package com.lms.system.model;

public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    DELETED;

    public boolean allowsLogin() {
        return this == ACTIVE;
    }
}
