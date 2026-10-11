package com.lms.system.controller;

import com.lms.system.dto.RegisterForm;
import com.lms.system.exception.DuplicateEmailException;
import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
