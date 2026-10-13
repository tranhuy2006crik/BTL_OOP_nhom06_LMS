package com.lms.system.controller;

import com.lms.system.model.User;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.MessageInboxService;
import com.lms.system.service.ProfileService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
public class MessagesController {

    private final ProfileService profileService;
    private final MessageInboxService messageInboxService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessagesController(
            ProfileService profileService,
            MessageInboxService messageInboxService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.profileService = profileService;
        this.messageInboxService = messageInboxService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/messages")
    public String messages(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "light") String theme,
            @RequestParam(defaultValue = "messages") String view,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) Long conversationId,
            Model model
    ) {
        if (!Set.of("light", "dark").contains(theme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported theme");
        }
        if (!Set.of("messages", "contacts").contains(view)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported messages view");
        }

        User user = profileService.getProfile(currentUser.getId());
        String normalizedQuery = query.trim();
        var conversation = conversationId == null || !view.equals("messages")
                ? null
                : messageInboxService.openConversation(currentUser.getId(), conversationId);
        var conversations = messageInboxService.conversations(currentUser.getId(), normalizedQuery);
        var starredConversations = messageInboxService.starredConversations(currentUser.getId(), normalizedQuery);
        model.addAttribute("user", user);
        model.addAttribute("pagePath", "/messages");
        model.addAttribute("theme", theme);
        model.addAttribute("view", view);
        model.addAttribute("query", normalizedQuery);
        model.addAttribute("conversationId", conversationId);
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("courseOverviewEnabled", false);
        model.addAttribute("courseSearchEnabled", false);
        model.addAttribute("messageInboxEnabled", true);
        model.addAttribute("starredCount", messageInboxService.starredCount(currentUser.getId()));
        model.addAttribute("groupCount", 0);
        long unstarredCount = conversations.stream().filter(item -> !item.starred()).count();
        model.addAttribute("privateCount", unstarredCount);
        model.addAttribute("unstarredCount", unstarredCount);
        model.addAttribute("conversations", conversations);
        model.addAttribute("starredConversations", starredConversations);
        model.addAttribute("contacts", view.equals("contacts")
                ? messageInboxService.contacts(currentUser.getId(), normalizedQuery)
                : java.util.List.of());
        model.addAttribute("conversation", conversation);
        return "messages";
    }

    @PostMapping("/messages/conversations")
    public String startConversation(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam Long recipientId,
            @RequestParam(defaultValue = "light") String theme,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        Long conversationId = messageInboxService.startConversation(currentUser.getId(), recipientId);
        redirectAttributes.addAttribute("conversationId", conversationId);
        redirectAttributes.addAttribute("theme", theme);
        return "redirect:/messages";
    }

    @PostMapping("/messages/{conversationId}/star")
    public String setConversationStarred(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long conversationId,
            @RequestParam boolean starred,
            @RequestParam(defaultValue = "light") String theme
    ) {
        requireTheme(theme);
        messageInboxService.setConversationStarred(currentUser.getId(), conversationId, starred);
        return "redirect:/messages?conversationId=" + conversationId + "&theme=" + theme;
    }

    @PostMapping("/messages/{conversationId}/messages")
    public String sendMessage(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "light") String theme,
            @RequestParam(defaultValue = "") String body,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        try {
            var message = messageInboxService.sendMessage(currentUser.getId(), conversationId, body);
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, message);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("messageError", exception.getMessage());
        }
        redirectAttributes.addAttribute("conversationId", conversationId);
        redirectAttributes.addAttribute("theme", theme);
        return "redirect:/messages";
    }

    private static void requireTheme(String theme) {
        if (!Set.of("light", "dark").contains(theme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported theme");
        }
    }
}
