package com.lms.system.controller;

import com.lms.system.dto.GradeForm;
import com.lms.system.model.User;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.CourseworkService;
import com.lms.system.service.GradebookService;
import com.lms.system.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GradebookController {

    private final GradebookService gradebookService;
    private final ProfileService profileService;
    private final CourseworkService courseworkService;

    public GradebookController(
            GradebookService gradebookService,
            ProfileService profileService,
            CourseworkService courseworkService
    ) {
        this.gradebookService = gradebookService;
        this.profileService = profileService;
        this.courseworkService = courseworkService;
    }

    // Hiển thị điểm đã công bố cùng tiến độ học tập của tài khoản đang đăng nhập.
    @GetMapping("/grades")
    public String studentGrades(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        User user = profileService.getProfile(currentUser.getId());
        GradebookService.StudentOverview overview = gradebookService.studentOverview(currentUser.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("pagePath", "/grades");
        model.addAttribute("theme", theme);
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("courseOverviewEnabled", false);
        model.addAttribute("courseSearchEnabled", false);
        model.addAttribute("grades", overview.grades());
        model.addAttribute("courseProgress", overview.courseProgress());
        model.addAttribute("publishedGradeCount", overview.publishedGradeCount());
        model.addAttribute("averagePublishedPercentage", overview.averagePublishedPercentage());
        model.addAttribute("completedActivityCount", overview.completedActivityCount());
        model.addAttribute("totalActivityCount", overview.totalActivityCount());
        model.addAttribute("overallProgressPercent", overview.overallProgressPercent());
        return "grades/student-gradebook";
    }

    // Liệt kê các khóa mà giảng viên/quản trị viên có quyền vào chấm bài.
    @GetMapping("/grading")
    public String gradingCourses(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        User user = profileService.getProfile(currentUser.getId());
        model.addAttribute("user", user);
        model.addAttribute("pagePath", "/grading");
        model.addAttribute("theme", theme);
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("courseOverviewEnabled", false);
        model.addAttribute("courseSearchEnabled", false);
        model.addAttribute("courses", gradebookService.coursesForGrading(currentUser.getUsername()));
        return "grades/grading-courses";
    }

    // Hiển thị bài nộp và trạng thái điểm của khóa học được chọn.
    @GetMapping("/courses/{courseId}/grading")
    public String gradingPage(
            @PathVariable Long courseId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        return renderGradingPage(courseId, currentUser, model, new GradeForm(), theme);
    }

    // Kiểm tra dữ liệu đầu vào, lưu điểm ở trạng thái chưa công bố rồi quay lại danh sách.
    @PostMapping("/courses/{courseId}/grading/{submissionId}/save")
    public String saveGrade(
            @PathVariable Long courseId,
            @PathVariable Long submissionId,
            @RequestParam(defaultValue = "light") String theme,
            @Valid @ModelAttribute("gradeForm") GradeForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (bindingResult.hasErrors()) {
            model.addAttribute("gradeError", "Vui lòng kiểm tra lại thông tin điểm.");
            return renderGradingPage(courseId, currentUser, model, form, theme);
        }
        gradebookService.saveGrade(courseId, submissionId, form, currentUser.getUsername());
        redirectAttributes.addFlashAttribute("gradeNotice", "Đã lưu điểm ở trạng thái chưa công bố.");
        redirectAttributes.addAttribute("theme", theme);
        return "redirect:/courses/" + courseId + "/grading";
    }

    // Chỉ gọi thao tác công bố riêng sau khi giảng viên đã lưu điểm.
    @PostMapping("/courses/{courseId}/grading/{submissionId}/release")
    public String releaseGrade(
            @PathVariable Long courseId,
            @PathVariable Long submissionId,
            @RequestParam(defaultValue = "light") String theme,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        gradebookService.releaseGrade(courseId, submissionId, currentUser.getUsername());
        redirectAttributes.addFlashAttribute("gradeNotice", "Đã công bố điểm cho học viên.");
        redirectAttributes.addAttribute("theme", theme);
        return "redirect:/courses/" + courseId + "/grading";
    }

    private String renderGradingPage(
            Long courseId,
            AuthenticatedUser currentUser,
            Model model,
            GradeForm gradeForm,
            String theme
    ) {
        User user = profileService.getProfile(currentUser.getId());
        model.addAttribute("user", user);
        model.addAttribute("pagePath", "/courses/" + courseId + "/grading");
        model.addAttribute("theme", theme);
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("courseOverviewEnabled", false);
        model.addAttribute("courseSearchEnabled", false);
        model.addAttribute("courseId", courseId);
        model.addAttribute("courseTitle", gradebookService.courseTitleForGrading(
                courseId, currentUser.getUsername()));
        var submissions = gradebookService.submissionsForGrading(courseId, currentUser.getUsername());
        model.addAttribute("submissions", submissions);
        model.addAttribute("submissionFiles", courseworkService.submissionFilesForGrading(submissions));
        model.addAttribute("ungradedCount", submissions.stream().filter(row -> row.score() == null).count());
        model.addAttribute("awaitingReleaseCount", submissions.stream()
                .filter(row -> row.score() != null && row.releasedAt() == null).count());
        model.addAttribute("publishedCount", submissions.stream()
                .filter(row -> row.releasedAt() != null).count());
        model.addAttribute("gradeForm", gradeForm);
        return "grades/course-grading";
    }

    private static void requireTheme(String theme) {
        if (!"light".equals(theme) && !"dark".equals(theme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported theme");
        }
    }
}
