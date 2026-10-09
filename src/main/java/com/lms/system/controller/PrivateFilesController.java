package com.lms.system.controller;

import com.lms.system.model.User;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.PrivateFileService;
import com.lms.system.service.PrivateFileService.PrivateFilesOverview;
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
public class PrivateFilesController {

    private final ProfileService profileService;
    private final PrivateFileService privateFileService;

    public PrivateFilesController(ProfileService profileService, PrivateFileService privateFileService) {
        this.profileService = profileService;
        this.privateFileService = privateFileService;
    }

    @GetMapping("/private-files")
    public String privateFiles(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "grid") String view,
            @RequestParam(defaultValue = "light") String theme,
            @RequestParam(required = false) String notice,
            Model model
    ) {
        if (!Set.of("grid", "list").contains(view)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file view");
        }
        if (!Set.of("light", "dark").contains(theme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported theme");
        }

        User user = profileService.getProfile(currentUser.getId());
        PrivateFilesOverview overview = privateFileService.overview();
        model.addAttribute("user", user);
        model.addAttribute("pagePath", "/private-files");
        model.addAttribute("theme", theme);
        model.addAttribute("view", view);
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("courseOverviewEnabled", false);
        model.addAttribute("courseSearchEnabled", false);
        model.addAttribute("privateFilesEnabled", true);
        model.addAttribute("files", overview.files());
        model.addAttribute("usedBytes", overview.usedBytes());
        model.addAttribute("usedStorage", String.format("%.1f KB", overview.usedBytes() / 1024.0));
        model.addAttribute("limitMegabytes", String.format("%.0f", overview.limitMegabytes()));
        model.addAttribute("usedPercentage", overview.usedPercentage());
        model.addAttribute("noticeMessage", noticeMessage(notice));
        return "private-files";
    }

    private static String noticeMessage(String notice) {
        if (notice == null) {
            return null;
        }
        return switch (notice) {
            case "grades" -> "Grades will be available when course grading is enabled.";
            case "messages" -> "Messaging will be available in a future update.";
            case "private-files" -> "Private files will be available when private storage is connected.";
            case "reports" -> "Reports will be available in a future update.";
            case "preferences" -> "Preferences will be available in a future update.";
            case "language" -> "Language options will be available in a future update.";
            case "notifications" -> "You’re all caught up — no new notifications.";
            case "upload" -> "File uploads will be available when private storage is connected.";
            case "folder" -> "Creating folders will be available when private storage is connected.";
            case "download" -> "Downloading sample files is not enabled.";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported notice");
        };
    }
}
