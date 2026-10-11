package com.lms.system.controller;

import com.lms.system.model.User;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.CourseCatalogService;
import com.lms.system.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Controller
public class HomeController {

    private final ProfileService profileService;
    private final CourseCatalogService courseCatalogService;

    public HomeController(ProfileService profileService, CourseCatalogService courseCatalogService) {
        this.profileService = profileService;
        this.courseCatalogService = courseCatalogService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "in-progress") String status,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "card") String view,
            @RequestParam(defaultValue = "light") String theme,
            @RequestParam(required = false) String notice,
            Model model
    ) {
        requireChoice("status", status, Set.of("in-progress", "future", "past", "all"));
        requireChoice("sort", sort, Set.of("recent", "name-asc", "name-desc", "progress"));
        requireChoice("view", view, Set.of("card", "list"));
        requireChoice("theme", theme, Set.of("light", "dark"));

        User user = profileService.getProfile(currentUser.getId());
        String query = q.trim();
        var displayedCourses = courseCatalogService.search(status, query, sort);

        model.addAttribute("user", user);
        model.addAttribute("courses", displayedCourses);
        model.addAttribute("courseSearchEnabled", true);
        model.addAttribute("courseOverviewEnabled", false);
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("pagePath", "/home");
        model.addAttribute("courseCount", displayedCourses.size());
        model.addAttribute("status", status);
        model.addAttribute("query", query);
        model.addAttribute("sort", sort);
        model.addAttribute("view", view);
        model.addAttribute("theme", theme);
        model.addAttribute("noticeMessage", noticeMessage(notice));
        return "home";
    }

    private static void requireChoice(String parameter, String value, Set<String> allowedValues) {
        if (!allowedValues.contains(value)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported " + parameter + " value"
            );
        }
    }

    private static String noticeMessage(String notice) {
        if (notice == null) {
            return null;
        }
        return switch (notice) {
            case "grades" -> "Grades will be available when course grading is enabled.";
            case "messages" -> "Use the Messages page to contact active users.";
            case "private-files" -> "Private files will be available in a future update.";
            case "reports" -> "Reports will be available in a future update.";
            case "preferences" -> "Preferences will be available in a future update.";
            case "language" -> "Language options will be available in a future update.";
            case "notifications" -> "You’re all caught up — no new notifications.";
            case "course-options" -> "Course options will be connected when live course data is added.";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported notice");
        };
    }

}
