package com.lms.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileForm {

    @NotBlank(message = "Họ tên không được trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    public ProfileForm() {
    }

    public ProfileForm(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
