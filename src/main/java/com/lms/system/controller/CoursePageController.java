package com.lms.system.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

import com.lms.system.dto.AnnouncementResponse;
import com.lms.system.dto.AnnouncementRequest;
import com.lms.system.dto.CourseSectionRequest;
import com.lms.system.dto.LessonRequest;
import com.lms.system.dto.LessonResponse;
import com.lms.system.model.Course;
import com.lms.system.repository.CourseRepository;
import com.lms.system.repository.LmsWorkflowRepository;
import com.lms.system.service.AnnouncementService;
import com.lms.system.service.LessonService;
import com.lms.system.service.ProfileService;

@Controller
@RequestMapping("/courses")
public class CoursePageController {

    private final LessonService lessonService;
    private final AnnouncementService announcementService;
    private final CourseRepository courseRepository;
    private final LmsWorkflowRepository workflowRepository;
    private final ProfileService profileService;

    public CoursePageController(
            LessonService lessonService,
            AnnouncementService announcementService,
            CourseRepository courseRepository,
            LmsWorkflowRepository workflowRepository,
            ProfileService profileService
    ) {
        this.lessonService = lessonService;
        this.announcementService = announcementService;
        this.courseRepository = courseRepository;
        this.workflowRepository = workflowRepository;
        this.profileService = profileService;
    }

    @GetMapping("/{courseId}")
    public String showCourse(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "light") String theme,
            Principal principal,
            Model model
    ) {
        if (principal == null) {
            return "redirect:/login";
        }
        if (!"light".equals(theme) && !"dark".equals(theme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported theme");
        }

        Course course = courseRepository.findWithCategoryById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học"));
        Long userId = workflowRepository.requireActiveUserId(principal.getName());
        boolean canManage = workflowRepository.canManageCourse(userId, courseId);
        if (!canManage && !workflowRepository.isActiveStudent(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập khóa học");
        }

        List<CourseRepository.CourseInstructorName> instructorRows =
                courseRepository.findActiveInstructorNamesByCourseIds(List.of(courseId));
        String instructors = instructorRows.stream()
                .map(CourseRepository.CourseInstructorName::getInstructorName)
                .reduce((first, second) -> first + ", " + second)
                .orElse("No instructor assigned");
        model.addAttribute("user", profileService.getProfile(userId));
        model.addAttribute("pagePath", "/courses/" + courseId);
        model.addAttribute("theme", theme);
        model.addAttribute("course", course);
        model.addAttribute("instructors", instructors);
        model.addAttribute("sections", workflowRepository.findCourseSections(courseId, canManage));
        model.addAttribute("canManage", canManage);
        model.addAttribute("announcements", announcementService.list(courseId, principal.getName()));
        return "courses/course-home";
    }

    @PostMapping("/{courseId}/announcements")
    public String publishAnnouncement(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "light") String theme,
            @Valid @ModelAttribute AnnouncementRequest request,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Enter an announcement title and message.");
        } else {
            try {
                announcementService.publish(courseId, request, principal.getName());
                redirectAttributes.addFlashAttribute("successMessage", "Announcement published.");
            } catch (ResponseStatusException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getReason());
            }
        }
        redirectAttributes.addAttribute("theme", theme);
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/{courseId}/sections")
    public String createSection(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "light") String theme,
            @Valid @ModelAttribute CourseSectionRequest request,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "A section title is required.");
        } else {
            try {
                lessonService.createSection(courseId, request, principal.getName());
                redirectAttributes.addFlashAttribute("successMessage", "Section created.");
            } catch (ResponseStatusException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getReason());
            }
        }
        redirectAttributes.addAttribute("theme", theme);
        return "redirect:/courses/" + courseId;
    }

    @GetMapping("/{courseId}/sections/{sectionId}")
    public String showSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @RequestParam(defaultValue = "light") String theme,
            Principal principal,
            Model model
    ) {
        if (principal == null) {
            return "redirect:/login";
        }
        requireTheme(theme);

        String email = principal.getName();
        List<LessonResponse> lessons = lessonService.list(courseId, sectionId, email);
        List<AnnouncementResponse> announcements = announcementService.list(courseId, email);
        Long userId = workflowRepository.requireActiveUserId(email);
        boolean canManage = workflowRepository.canManageCourse(userId, courseId);
        Course course = courseRepository.findWithCategoryById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học"));
        var section = workflowRepository.findCourseSections(courseId, canManage).stream()
                .filter(candidate -> candidate.id().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chương trong khóa học"));

        model.addAttribute("user", profileService.getProfile(userId));
        model.addAttribute("pagePath", "/courses/" + courseId + "/sections/" + sectionId);
        model.addAttribute("theme", theme);
        model.addAttribute("course", course);
        model.addAttribute("section", section);
        model.addAttribute("canManage", canManage);
        model.addAttribute("courseId", courseId);
        model.addAttribute("sectionId", sectionId);
        model.addAttribute("lessons", lessons);
        model.addAttribute("announcements", announcements);
        model.addAttribute("title", section.title());

        return "courses/course-detail";
    }

    @PostMapping("/{courseId}/sections/{sectionId}/lessons")
    public String createLesson(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @RequestParam(defaultValue = "light") String theme,
            @Valid @ModelAttribute LessonRequest request,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "A lesson title and content are required.");
        } else {
            try {
                lessonService.create(courseId, sectionId, request, principal.getName());
                redirectAttributes.addFlashAttribute("successMessage", "Lesson created.");
            } catch (ResponseStatusException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getReason());
            }
        }
        redirectAttributes.addAttribute("theme", theme);
        return "redirect:/courses/" + courseId + "/sections/" + sectionId;
    }

    @PostMapping("/{courseId}/sections/{sectionId}/lessons/{lessonId}")
    public String updateLesson(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long lessonId,
            @RequestParam(defaultValue = "light") String theme,
            @Valid @ModelAttribute LessonRequest request,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "A lesson title and content are required.");
        } else {
            try {
                lessonService.update(courseId, sectionId, lessonId, request, principal.getName());
                redirectAttributes.addFlashAttribute("successMessage", "Lesson updated.");
            } catch (ResponseStatusException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getReason());
            }
        }
        redirectAttributes.addAttribute("theme", theme);
        return "redirect:/courses/" + courseId + "/sections/" + sectionId;
    }

    @PostMapping("/{courseId}/sections/{sectionId}/lessons/{lessonId}/delete")
    public String deleteLesson(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long lessonId,
            @RequestParam(defaultValue = "light") String theme,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        try {
            lessonService.delete(courseId, sectionId, lessonId, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Lesson hidden from students.");
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getReason());
        }
        redirectAttributes.addAttribute("theme", theme);
        return "redirect:/courses/" + courseId + "/sections/" + sectionId;
    }

    private static void requireTheme(String theme) {
        if (!"light".equals(theme) && !"dark".equals(theme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported theme");
        }
    }
}
