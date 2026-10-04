package com.lms.system.controller;

import com.lms.system.model.User;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.ProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ProfileService profileService;

    public HomeController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal AuthenticatedUser currentUser, Model model) {
        User user = profileService.getProfile(currentUser.getId());
        model.addAttribute("user", user);
        return "home";
    }
}
