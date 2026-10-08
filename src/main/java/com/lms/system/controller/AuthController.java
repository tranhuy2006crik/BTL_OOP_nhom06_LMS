package com.lms.system.controller;

import com.lms.system.dto.ForgotPasswordForm;
import com.lms.system.dto.RegisterForm;
import com.lms.system.dto.ResetPasswordForm;
import com.lms.system.exception.DuplicateEmailException;
import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.exception.PasswordResetDeliveryException;
import com.lms.system.service.AuthService;
import com.lms.system.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        if (isLoggedIn(authentication)) {
            return "redirect:/home";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model, Authentication authentication) {
        if (isLoggedIn(authentication)) {
            return "redirect:/home";
        }
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model, Authentication authentication) {
        if (isLoggedIn(authentication)) {
            return "redirect:/home";
        }
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(
            @Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        try {
            passwordResetService.requestPasswordReset(form.getEmail());
        } catch (PasswordResetDeliveryException ex) {
            bindingResult.reject("deliveryFailed", "Không thể gửi email lúc này. Vui lòng thử lại sau.");
            return "auth/forgot-password";
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "Nếu email thuộc về một tài khoản, hướng dẫn đặt lại mật khẩu sẽ được gửi.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(
            @RequestParam(required = false) String token,
            Model model,
            Authentication authentication
    ) {
        if (isLoggedIn(authentication)) {
            return "redirect:/home";
        }
        if (!model.containsAttribute("resetPasswordForm")) {
            ResetPasswordForm form = new ResetPasswordForm();
            form.setToken(token);
            model.addAttribute("resetPasswordForm", form);
        }
        model.addAttribute("tokenValid", passwordResetService.isTokenValid(token));
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        boolean tokenValid = passwordResetService.isTokenValid(form.getToken());
        model.addAttribute("tokenValid", tokenValid);
        if (!tokenValid) {
            return "auth/reset-password";
        }
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }
        try {
            passwordResetService.resetPassword(form.getToken(), form.getPassword(), form.getConfirmPassword());
        } catch (InvalidPasswordException ex) {
            model.addAttribute("tokenValid", passwordResetService.isTokenValid(form.getToken()));
            bindingResult.reject("resetFailed", ex.getMessage());
            return "auth/reset-password";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Mật khẩu đã được cập nhật. Hãy đăng nhập.");
        return "redirect:/login";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerForm") RegisterForm registerForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.register(registerForm);
        } catch (DuplicateEmailException | InvalidPasswordException | IllegalArgumentException ex) {
            bindingResult.reject("registerFailed", ex.getMessage());
            return "auth/register";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công. Hãy đăng nhập.");
        return "redirect:/login";
    }

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
