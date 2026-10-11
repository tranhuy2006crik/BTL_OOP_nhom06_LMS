package com.lms.system.controller;

import com.lms.system.model.AssignmentSubmission;
import com.lms.system.model.Course;
import com.lms.system.model.CourseAssignment;
import com.lms.system.model.CourseMaterial;
import com.lms.system.model.Permission;
import com.lms.system.model.User;
import com.lms.system.repository.CourseRepository;
import com.lms.system.repository.UserRepository;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.CourseAssignmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class CourseAssignmentController {

    private final CourseAssignmentService assignmentService;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseAssignmentController(
            CourseAssignmentService assignmentService,
            CourseRepository courseRepository,
            UserRepository userRepository
    ) {
        this.assignmentService = assignmentService;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/courses/{courseId}/assignments")
    public String assignments(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long courseId,
            Model model
    ) {
        User user = userRepository.findWithAccessById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course was not found"));
        if (!user.hasPermission(Permission.VIEW_COURSE, courseId)) {
            throw new AccessDeniedException("You do not have access to this course");
        }

        List<CourseAssignment> assignments = assignmentService.listAssignments(courseId);
        List<CourseMaterial> materials = assignmentService.listMaterials(courseId);

        model.addAttribute("user", user);
        model.addAttribute("course", course);
        model.addAttribute("theme", "light");
        model.addAttribute("assignments", assignments);
        model.addAttribute("materials", materials);
        model.addAttribute("mySubmission", null);
        model.addAttribute("pagePath", "/courses/" + courseId + "/assignments");
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("courseOverviewEnabled", true);
        model.addAttribute("courseSearchEnabled", false);
        return "courses/assignments/list";
    }

    @GetMapping("/courses/{courseId}/assignments/{assignmentId}")
    public String assignmentDetail(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            Model model
    ) {
        User user = userRepository.findWithAccessById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course was not found"));
        if (!user.hasPermission(Permission.VIEW_COURSE, courseId)) {
            throw new AccessDeniedException("You do not have access to this course");
        }

        CourseAssignment assignment = assignmentService.getAssignment(courseId, assignmentId);
        AssignmentSubmission mySubmission = null;
        try {
            mySubmission = assignmentService.getStudentSubmission(courseId, assignmentId, user.getId());
        } catch (IllegalArgumentException ignored) {
            // no submission for this user yet
        }

        model.addAttribute("user", user);
        model.addAttribute("course", course);
        model.addAttribute("assignment", assignment);
        model.addAttribute("mySubmission", mySubmission);
        model.addAttribute("theme", "light");
        model.addAttribute("pagePath", "/courses/" + courseId + "/assignments/" + assignmentId);
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("courseOverviewEnabled", true);
        model.addAttribute("courseSearchEnabled", false);
        model.addAttribute("formattedDueAt", assignment.getDueAt() == null ? "No deadline" : assignment.getDueAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        return "courses/assignments/detail";
    }

    @PostMapping("/courses/{courseId}/assignments")
    public String createAssignment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String instructions,
            @RequestParam String dueAt,
            @RequestParam(defaultValue = "100") Integer maxScore,
            @RequestParam(defaultValue = "false") boolean allowLate,
            @RequestParam(defaultValue = "true") boolean published
    ) {
        CourseAssignment assignment = assignmentService.createAssignment(
                courseId,
                currentUser.getId(),
                title,
                description,
                instructions,
                LocalDateTime.parse(dueAt),
                maxScore,
                allowLate,
                published
        );
        return "redirect:/courses/" + courseId + "/assignments/" + assignment.getId();
    }

    @PostMapping("/courses/{courseId}/assignments/{assignmentId}/submit")
    public String submitAssignment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            @RequestParam(required = false) String textAnswer,
            @RequestParam(required = false) MultipartFile attachment
    ) {
        assignmentService.submitAssignment(courseId, assignmentId, currentUser.getId(), textAnswer, attachment);
        return "redirect:/courses/" + courseId + "/assignments/" + assignmentId;
    }

    @PostMapping("/courses/{courseId}/assignments/{assignmentId}/grade")
    public String gradeAssignment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            @RequestParam Long submissionId,
            @RequestParam(required = false) Integer grade,
            @RequestParam(required = false) String feedback
    ) {
        assignmentService.gradeSubmission(courseId, assignmentId, submissionId, currentUser.getId(), grade, feedback);
        return "redirect:/courses/" + courseId + "/assignments/" + assignmentId;
    }

    @PostMapping("/courses/{courseId}/materials")
    public String uploadMaterial(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile file
    ) {
        assignmentService.uploadMaterial(courseId, currentUser.getId(), title, description, file);
        return "redirect:/courses/" + courseId + "/assignments";
    }

    @GetMapping("/courses/{courseId}/materials/{materialId}/download")
    public ResponseEntity<Resource> downloadMaterial(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long courseId,
            @PathVariable Long materialId
    ) {
        User user = userRepository.findWithAccessById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        if (!user.hasPermission(Permission.VIEW_COURSE, courseId)) {
            throw new AccessDeniedException("You do not have access to this course");
        }
        CourseMaterial material = assignmentService.listMaterials(courseId).stream()
                .filter(item -> item.getId().equals(materialId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Material was not found"));

        Resource resource = assignmentService.downloadMaterial(courseId, materialId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getOriginalFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
