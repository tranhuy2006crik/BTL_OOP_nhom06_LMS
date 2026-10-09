package com.lms.system.controller;

import com.lms.system.dto.ChangePasswordForm;
import com.lms.system.dto.ProfileForm;
import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.model.User;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.CourseCatalogService;
import com.lms.system.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final CourseCatalogService courseCatalogService;

    public ProfileController(ProfileService profileService, CourseCatalogService courseCatalogService) {
        this.profileService = profileService;
        this.courseCatalogService = courseCatalogService;
    }

    @GetMapping
    public String profile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        User user = profileService.getProfile(currentUser.getId());
        addProfileOverviewModel(model, user, theme);
        return "profile/profile";
    }

    @GetMapping("/edit")
    public String editProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        User user = profileService.getProfile(currentUser.getId());
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", new ProfileForm(user));
        }
        if (!model.containsAttribute("changePasswordForm")) {
            model.addAttribute("changePasswordForm", new ChangePasswordForm());
        }
        addProfileEditModel(model, user, theme);
        return "profile/edit-profile";
    }

    @GetMapping("/picture")
    public ResponseEntity<byte[]> profilePicture(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        User user = profileService.getProfilePicture(currentUser.getId());
        byte[] picture = user.getProfilePicture();
        if (picture == null || user.getProfilePictureContentType() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(user.getProfilePictureContentType()))
                .body(picture);
    }

    @PostMapping
    public String updateProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @ModelAttribute("profileForm") ProfileForm profileForm,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile profilePicture,
            @RequestParam(defaultValue = "false") boolean deleteProfilePicture,
            @RequestParam(defaultValue = "light") String theme,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (bindingResult.hasErrors()) {
            User user = profileService.getProfile(currentUser.getId());
            addProfileEditModel(model, user, theme);
            model.addAttribute("changePasswordForm", new ChangePasswordForm());
            return "profile/edit-profile";
        }
        try {
            byte[] pictureBytes;
            String pictureContentType;
            if (profilePicture == null || profilePicture.isEmpty()) {
                pictureBytes = null;
                pictureContentType = null;
            } else {
                pictureBytes = profilePicture.getBytes();
                pictureContentType = profilePicture.getContentType();
            }
            profileService.updateProfile(
                    currentUser.getId(),
                    profileForm,
                    pictureBytes,
                    pictureContentType,
                    deleteProfilePicture
            );
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Unable to read uploaded profile picture", ex);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("profileFailed", ex.getMessage());
            User user = profileService.getProfile(currentUser.getId());
            addProfileEditModel(model, user, theme);
            model.addAttribute("changePasswordForm", new ChangePasswordForm());
            return "profile/edit-profile";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật hồ sơ.");
        return "redirect:/profile?theme=" + theme;
    }

    @PostMapping("/password")
    public String changePassword(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @ModelAttribute("changePasswordForm") ChangePasswordForm changePasswordForm,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "light") String theme,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        User user = profileService.getProfile(currentUser.getId());
        if (bindingResult.hasErrors()) {
            addProfileEditModel(model, user, theme);
            model.addAttribute("profileForm", new ProfileForm(user));
            return "profile/edit-profile";
        }
        try {
            profileService.changePassword(currentUser.getId(), changePasswordForm);
        } catch (InvalidPasswordException | IllegalArgumentException ex) {
            bindingResult.reject("passwordFailed", ex.getMessage());
            addProfileEditModel(model, user, theme);
            model.addAttribute("profileForm", new ProfileForm(user));
            return "profile/edit-profile";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã đổi mật khẩu.");
        return "redirect:/profile/edit?theme=" + theme;
    }

    private void addProfileOverviewModel(Model model, User user, String theme) {
        model.addAttribute("user", user);
        model.addAttribute("pagePath", "/profile");
        model.addAttribute("theme", theme);
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("courseOverviewEnabled", false);
        model.addAttribute("courseSearchEnabled", false);
        model.addAttribute("profileCourses", courseCatalogService.search("in-progress", "", "recent"));
    }

    private static void addProfileEditModel(Model model, User user, String theme) {
        model.addAttribute("user", user);
        model.addAttribute("pagePath", "/profile/edit");
        model.addAttribute("theme", theme);
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("courseOverviewEnabled", false);
        model.addAttribute("courseSearchEnabled", false);
    }

    private static void requireTheme(String theme) {
        if (!Set.of("light", "dark").contains(theme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported theme");
        }
    }
}
