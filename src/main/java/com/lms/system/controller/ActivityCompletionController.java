package com.lms.system.controller;

import com.lms.system.service.ActivityCompletionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/courses/{courseId}/sections/{sectionId}/activities/{activityId}/completion")
public class ActivityCompletionController {

    private final ActivityCompletionService completionService;

    public ActivityCompletionController(ActivityCompletionService completionService) {
        this.completionService = completionService;
    }

    @PostMapping
    public String updateCompletion(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long activityId,
            @RequestParam(defaultValue = "true") boolean completed,
            @RequestParam(defaultValue = "light") String theme,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        if (!"light".equals(theme) && !"dark".equals(theme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported theme");
        }
        completionService.setCompleted(
                courseId, sectionId, activityId, completed, principal.getName());
        redirectAttributes.addFlashAttribute(
                "successMessage", completed ? "Đã cập nhật tiến độ học tập." : "Đã bỏ đánh dấu hoàn thành.");
        redirectAttributes.addAttribute("theme", theme);
        return "redirect:/courses/" + courseId + "/sections/" + sectionId;
    }
}
