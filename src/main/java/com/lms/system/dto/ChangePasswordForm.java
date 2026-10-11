package com.lms.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordForm {

    @NotBlank(message = "Mật khẩu hiện tại không được trống")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được trống")
    @Size(min = 8, max = 64, message = "Mật khẩu từ 8 đến 64 ký tự")
    private String newPassword;

    @NotBlank(message = "Hãy nhập lại mật khẩu mới")
    private String confirmNewPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }
}
