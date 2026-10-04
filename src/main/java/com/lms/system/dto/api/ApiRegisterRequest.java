package com.lms.system.dto.api;

import com.lms.system.dto.RegisterForm;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ApiRegisterRequest {

    @NotBlank(message = "Email không được trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 254, message = "Email tối đa 254 ký tự")
    private String email;

    @NotBlank(message = "Họ tên không được trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    @NotBlank(message = "Mật khẩu không được trống")
    @Size(min = 8, max = 64, message = "Mật khẩu từ 8 đến 64 ký tự")
    private String password;

    @NotBlank(message = "Hãy nhập lại mật khẩu")
    private String confirmPassword;

    public RegisterForm toRegisterForm() {
        RegisterForm form = new RegisterForm();
        form.setEmail(email);
        form.setFullName(fullName);
        form.setPassword(password);
        form.setConfirmPassword(confirmPassword);
        return form;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
