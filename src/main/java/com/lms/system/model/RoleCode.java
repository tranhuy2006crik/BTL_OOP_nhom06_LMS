package com.lms.system.model;

public enum RoleCode {
    ADMIN,
    TEACHER,
    STUDENT;

    public static RoleCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Role code không được trống");
        }
        try {
            return RoleCode.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Role không hỗ trợ: " + code);
        }
    }
}
