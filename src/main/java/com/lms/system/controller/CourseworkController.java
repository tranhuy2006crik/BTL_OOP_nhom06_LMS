package com.lms.system.controller;

import com.lms.system.dto.AssignmentForm;
import com.lms.system.service.CourseworkService;
import com.lms.system.service.CourseworkService.DownloadableFile;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/courses")
public class CourseworkController {

    private final CourseworkService courseworkService;

    public CourseworkController(CourseworkService courseworkService) {
        this.courseworkService = courseworkService;
    }

    @PostMapping("/{courseId}/sections/{sectionId}/assignments")
    public String createAssignment(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @RequestParam(defaultValue = "light") String theme,
            @Valid @ModelAttribute("assignmentForm") AssignmentForm form,
            BindingResult bindingResult,
            @RequestParam(name = "attachments", required = false) List<MultipartFile> attachments,
            java.security.Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng kiểm tra thông tin bài tập.");
        } else {
            try {
                courseworkService.createAssignment(
                        courseId, sectionId, form, attachments, principal.getName());
                redirectAttributes.addFlashAttribute("successMessage", "Đã tạo bài tập.");
            } catch (IllegalArgumentException | ResponseStatusException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", errorMessage(exception));
            }
        }
        return redirectToSection(courseId, sectionId, theme, redirectAttributes);
    }

    @PostMapping("/{courseId}/sections/{sectionId}/assignments/{assignmentId}")
    public String updateAssignment(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long assignmentId,
            @RequestParam(defaultValue = "light") String theme,
            @Valid @ModelAttribute("assignmentForm") AssignmentForm form,
            BindingResult bindingResult,
            @RequestParam(name = "attachments", required = false) List<MultipartFile> attachments,
            java.security.Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng kiểm tra thông tin bài tập.");
        } else {
            try {
                courseworkService.updateAssignment(
                        courseId, sectionId, assignmentId, form, attachments, principal.getName());
                redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật bài tập.");
            } catch (IllegalArgumentException | ResponseStatusException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", errorMessage(exception));
            }
        }
        return redirectToSection(courseId, sectionId, theme, redirectAttributes);
    }

    @PostMapping("/{courseId}/sections/{sectionId}/assignments/{assignmentId}/submissions")
    public String submitAssignment(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long assignmentId,
            @RequestParam(defaultValue = "light") String theme,
            @RequestParam(required = false) String textContent,
            @RequestParam(name = "submissionFiles", required = false) List<MultipartFile> files,
            java.security.Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        try {
            courseworkService.submit(courseId, assignmentId, textContent, files, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Bài tập đã được nộp.");
        } catch (IllegalArgumentException | ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage(exception));
        }
        return redirectToSection(courseId, sectionId, theme, redirectAttributes);
    }

    @PostMapping("/{courseId}/sections/{sectionId}/resources")
    public String createResource(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @RequestParam(defaultValue = "light") String theme,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(name = "attachments", required = false) List<MultipartFile> attachments,
            java.security.Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        try {
            courseworkService.createResource(
                    courseId, sectionId, title, description, attachments, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm tài liệu khóa học.");
        } catch (IllegalArgumentException | ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage(exception));
        }
        return redirectToSection(courseId, sectionId, theme, redirectAttributes);
    }

    @GetMapping("/{courseId}/files/{fileId}")
    public ResponseEntity<byte[]> downloadActivityFile(
            @PathVariable Long courseId,
            @PathVariable Long fileId,
            java.security.Principal principal
    ) {
        return fileResponse(courseworkService.downloadActivityFile(courseId, fileId, principal.getName()));
    }

    @GetMapping("/{courseId}/submissions/{submissionId}/files/{fileId}")
    public ResponseEntity<byte[]> downloadSubmissionFile(
            @PathVariable Long courseId,
            @PathVariable Long submissionId,
            @PathVariable Long fileId,
            java.security.Principal principal
    ) {
        return fileResponse(courseworkService.downloadSubmissionFile(
                courseId, submissionId, fileId, principal.getName()));
    }

    private static ResponseEntity<byte[]> fileResponse(DownloadableFile file) {
        MediaType mediaType = MediaType.parseMediaType(file.mimeType());
        byte[] content = file.content();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.originalName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(content);
    }

    private static String errorMessage(RuntimeException exception) {
        if (exception instanceof ResponseStatusException statusException) {
            return statusException.getReason() == null
                    ? "Không thể hoàn tất thao tác."
                    : statusException.getReason();
        }
        return exception.getMessage() == null ? "Dữ liệu không hợp lệ." : exception.getMessage();
    }

    private static String redirectToSection(
            Long courseId, Long sectionId, String theme, RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addAttribute("theme", theme);
        return "redirect:/courses/" + courseId + "/sections/" + sectionId;
    }

    private static void requireTheme(String theme) {
        if (!Set.of("light", "dark").contains(theme)) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported theme");
        }
    }
}
