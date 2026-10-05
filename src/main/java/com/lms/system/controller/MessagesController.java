package com.lms.system.controller;

import com.lms.system.model.User;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.MessageInboxService;
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
public class MessagesController {

    private final ProfileService profileService;
    private final MessageInboxService messageInboxService;

    public MessagesController(ProfileService profileService, MessageInboxService messageInboxService) {
        this.profileService = profileService;
        this.messageInboxService = messageInboxService;
    }

    @GetMapping("/messages")
    public String messages(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "light") String theme,
            @RequestParam(defaultValue = "messages") String view,
            @RequestParam(defaultValue = "") String query,
            Model model
    ) {
        if (!Set.of("light", "dark").contains(theme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported theme");
        }
        if (!Set.of("messages", "contacts").contains(view)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported messages view");
        }

        User user = profileService.getProfile(currentUser.getId());
        var summary = messageInboxService.summary();
        model.addAttribute("user", user);
        model.addAttribute("pagePath", "/messages");
        model.addAttribute("theme", theme);
        model.addAttribute("view", view);
        model.addAttribute("query", query);
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("courseOverviewEnabled", false);
        model.addAttribute("courseSearchEnabled", false);
        model.addAttribute("messageInboxEnabled", true);
        model.addAttribute("starredCount", summary.starredCount());
        model.addAttribute("groupCount", summary.groupCount());
        model.addAttribute("privateCount", summary.privateCount());
        return "messages";
    }
}
