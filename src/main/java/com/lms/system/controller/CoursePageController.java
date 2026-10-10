package com.lms.system.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.lms.system.dto.AnnouncementResponse;
import com.lms.system.dto.LessonResponse;
import com.lms.system.service.AnnouncementService;
import com.lms.system.service.LessonService;

@Controller
@RequestMapping("/courses")
public class CoursePageController {

    private final LessonService lessonService;
    private final AnnouncementService announcementService;

    public CoursePageController(LessonService lessonService, AnnouncementService announcementService) {
        this.lessonService = lessonService;
        this.announcementService = announcementService;
    }

    @GetMapping("/{courseId}/sections/{sectionId}")
    public String showSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            Principal principal,
            Model model
    ) {
        if (principal == null) {
            return "redirect:/login";
        }

        String email = principal.getName();
        List<LessonResponse> lessons = lessonService.list(courseId, sectionId, email);
        List<AnnouncementResponse> announcements = announcementService.list(courseId, email);

        model.addAttribute("courseId", courseId);
        model.addAttribute("sectionId", sectionId);
        model.addAttribute("lessons", lessons);
        model.addAttribute("announcements", announcements);
        model.addAttribute("title", "Course content");
        model.addAttribute("pageTitle", "Course content");

        return "courses/course-detail";
    }
}
