package com.lms.system.controller;

import com.lms.system.dto.ChangePasswordForm;
import com.lms.system.dto.ProfileForm;
import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.model.User;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String profile(@AuthenticationPrincipal AuthenticatedUser currentUser, Model model) {
        User user = profileService.getProfile(currentUser.getId());
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", new ProfileForm(user.getFullName()));
        }
        if (!model.containsAttribute("changePasswordForm")) {
            model.addAttribute("changePasswordForm", new ChangePasswordForm());
        }
        model.addAttribute("user", user);
        return "profile/profile";
    }

    @PostMapping
    public String updateProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @ModelAttribute("profileForm") ProfileForm profileForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            User user = profileService.getProfile(currentUser.getId());
            model.addAttribute("user", user);
            model.addAttribute("changePasswordForm", new ChangePasswordForm());
            return "profile/profile";
        }
        try {
            profileService.updateProfile(currentUser.getId(), profileForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("profileFailed", ex.getMessage());
            User user = profileService.getProfile(currentUser.getId());
            model.addAttribute("user", user);
            model.addAttribute("changePasswordForm", new ChangePasswordForm());
            return "profile/profile";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật hồ sơ.");
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @ModelAttribute("changePasswordForm") ChangePasswordForm changePasswordForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User user = profileService.getProfile(currentUser.getId());
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("profileForm", new ProfileForm(user.getFullName()));
            return "profile/profile";
        }
        try {
            profileService.changePassword(currentUser.getId(), changePasswordForm);
        } catch (InvalidPasswordException | IllegalArgumentException ex) {
            bindingResult.reject("passwordFailed", ex.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("profileForm", new ProfileForm(user.getFullName()));
            return "profile/profile";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã đổi mật khẩu.");
        return "redirect:/profile";
    }
}
