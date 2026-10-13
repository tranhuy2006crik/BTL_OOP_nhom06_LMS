package com.lms.system.exception;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String email) {
        super("Email đã được sử dụng: " + email);
    }
}
