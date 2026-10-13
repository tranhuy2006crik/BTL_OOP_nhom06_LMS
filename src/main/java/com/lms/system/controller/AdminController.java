package com.lms.system.controller;

import com.lms.system.dto.admin.AdminCategoryForm;
import com.lms.system.dto.admin.AdminCourseForm;
import com.lms.system.dto.admin.AdminUserForm;
import com.lms.system.exception.DuplicateEmailException;
import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.exception.ResourceNotFoundException;
import com.lms.system.model.CourseStatus;
import com.lms.system.model.User;
import com.lms.system.model.UserStatus;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.AdminCourseService;
import com.lms.system.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int PAGE_SIZE = 10;

    private final AdminUserService adminUserService;
    private final AdminCourseService adminCourseService;

    public AdminController(AdminUserService adminUserService, AdminCourseService adminCourseService) {
        this.adminUserService = adminUserService;
        this.adminCourseService = adminCourseService;
    }

    @ModelAttribute
    public void requireCurrentAdministrator(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        User user = adminUserService.get(currentUser.getId());
        if (!user.canLogin() || !user.isSystemAdmin()) {
            throw new AccessDeniedException("An active system administrator account is required");
        }
    }

    @GetMapping
    public String dashboard(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        User user = adminUserService.get(currentUser.getId());
        addBaseModel(model, user, "/admin", theme);
        model.addAttribute("userCount", adminUserService.countUsers());
        model.addAttribute("activeUserCount", adminUserService.countUsers(UserStatus.ACTIVE));
        model.addAttribute("suspendedUserCount", adminUserService.countUsers(UserStatus.SUSPENDED));
        model.addAttribute("courseCount", adminCourseService.countCourses());
        model.addAttribute("draftCourseCount", adminCourseService.countCourses(CourseStatus.DRAFT));
        model.addAttribute("publishedCourseCount", adminCourseService.countCourses(CourseStatus.PUBLISHED));
        model.addAttribute("categoryCount", adminCourseService.countCategories());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        UserStatus selectedStatus = parseUserStatus(status);
        String query = q.trim();
        User user = adminUserService.get(currentUser.getId());
        addBaseModel(model, user, "/admin/users", theme);
        model.addAttribute("users", adminUserService.search(
                query, selectedStatus, PageRequest.of(Math.max(0, page), PAGE_SIZE)));
        model.addAttribute("query", query);
        model.addAttribute("status", status);
        return "admin/users/list";
    }

    @GetMapping("/users/new")
    public String newUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        User user = adminUserService.get(currentUser.getId());
        if (!model.containsAttribute("userForm")) {
            model.addAttribute("userForm", new AdminUserForm());
        }
        addBaseModel(model, user, "/admin/users/new", theme);
        model.addAttribute("editing", false);
        return "admin/users/form";
    }

    @PostMapping("/users")
    public String createUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @ModelAttribute("userForm") AdminUserForm form,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "light") String theme,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (!bindingResult.hasErrors()) {
            try {
                adminUserService.create(form);
                redirectAttributes.addFlashAttribute("successMessage", "User account created.");
                return "redirect:/admin/users";
            } catch (DuplicateEmailException | InvalidPasswordException | IllegalArgumentException exception) {
                bindingResult.reject("userFormFailed", exception.getMessage());
            }
        }
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/users/new", theme);
        model.addAttribute("editing", false);
        return "admin/users/form";
    }

    @GetMapping("/users/{id}")
    public String userDetail(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        User user = adminUserService.get(id);
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/users/" + id, theme);
        model.addAttribute("managedUser", user);
        return "admin/users/detail";
    }

    @GetMapping("/users/{id}/edit")
    public String editUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        User managedUser = adminUserService.get(id);
        if (!model.containsAttribute("userForm")) {
            model.addAttribute("userForm", new AdminUserForm(managedUser));
        }
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/users/" + id + "/edit", theme);
        model.addAttribute("managedUser", managedUser);
        model.addAttribute("editing", true);
        return "admin/users/form";
    }

    @PostMapping("/users/{id}")
    public String updateUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @ModelAttribute("userForm") AdminUserForm form,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "light") String theme,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (!bindingResult.hasErrors()) {
            try {
                adminUserService.update(id, form, currentUser.getId());
                redirectAttributes.addFlashAttribute("successMessage", "User account updated.");
                return "redirect:/admin/users/" + id;
            } catch (DuplicateEmailException | ResourceNotFoundException | IllegalArgumentException exception) {
                bindingResult.reject("userFormFailed", exception.getMessage());
            }
        }
        User managedUser = adminUserService.get(id);
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/users/" + id + "/edit", theme);
        model.addAttribute("managedUser", managedUser);
        model.addAttribute("editing", true);
        return "admin/users/form";
    }

    @PostMapping("/users/{id}/suspend")
    public String suspendUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUserService.suspend(id, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "User account suspended.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/activate")
    public String activateUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUserService.activate(id);
            redirectAttributes.addFlashAttribute("successMessage", "User account activated.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUserService.delete(id, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "User account deleted and retained for learning history.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/courses")
    public String courses(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        CourseStatus selectedStatus = parseCourseStatus(status);
        String query = q.trim();
        User user = adminUserService.get(currentUser.getId());
        addBaseModel(model, user, "/admin/courses", theme);
        var courses = adminCourseService.searchCourses(
                query, selectedStatus, PageRequest.of(Math.max(0, page), PAGE_SIZE));
        model.addAttribute("courses", courses);
        model.addAttribute("courseInstructors", adminCourseService.instructorNamesByCourse(courses.getContent()));
        model.addAttribute("query", query);
        model.addAttribute("status", status);
        return "admin/courses/list";
    }

    @GetMapping("/courses/new")
    public String newCourse(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        if (!model.containsAttribute("courseForm")) {
            model.addAttribute("courseForm", new AdminCourseForm());
        }
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/courses/new", theme);
        addCourseFormOptions(model);
        model.addAttribute("instructors", adminCourseService.availableInstructors());
        model.addAttribute("editing", false);
        return "admin/courses/form";
    }

    @PostMapping("/courses")
    public String createCourse(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @ModelAttribute("courseForm") AdminCourseForm form,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "light") String theme,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (!bindingResult.hasErrors()) {
            try {
                var course = adminCourseService.createCourse(form, currentUser.getId());
                redirectAttributes.addFlashAttribute("successMessage", "Course created.");
                return "redirect:/admin/courses/" + course.getId();
            } catch (ResourceNotFoundException | IllegalArgumentException exception) {
                bindingResult.reject("courseFormFailed", exception.getMessage());
            }
        }
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/courses/new", theme);
        addCourseFormOptions(model);
        model.addAttribute("instructors", adminCourseService.availableInstructors());
        model.addAttribute("editing", false);
        return "admin/courses/form";
    }

    @GetMapping("/courses/{id}")
    public String courseDetail(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/courses/" + id, theme);
        model.addAttribute("course", adminCourseService.getCourse(id));
        return "admin/courses/detail";
    }

    @GetMapping("/courses/{id}/edit")
    public String editCourse(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        var course = adminCourseService.getCourse(id);
        if (!model.containsAttribute("courseForm")) {
            AdminCourseForm form = new AdminCourseForm(course);
            form.setInstructorIds(adminCourseService.instructorIdsForCourse(id));
            model.addAttribute("courseForm", form);
        }
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/courses/" + id + "/edit", theme);
        addCourseFormOptions(model);
        model.addAttribute("instructors", adminCourseService.availableInstructors());
        model.addAttribute("editing", true);
        model.addAttribute("course", course);
        return "admin/courses/form";
    }

    @PostMapping("/courses/{id}")
    public String updateCourse(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @ModelAttribute("courseForm") AdminCourseForm form,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "light") String theme,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (!bindingResult.hasErrors()) {
            try {
                adminCourseService.updateCourse(id, form);
                redirectAttributes.addFlashAttribute("successMessage", "Course updated.");
                return "redirect:/admin/courses/" + id;
            } catch (ResourceNotFoundException | IllegalArgumentException exception) {
                bindingResult.reject("courseFormFailed", exception.getMessage());
            }
        }
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/courses/" + id + "/edit", theme);
        addCourseFormOptions(model);
        model.addAttribute("instructors", adminCourseService.availableInstructors());
        model.addAttribute("editing", true);
        model.addAttribute("course", adminCourseService.getCourse(id));
        return "admin/courses/form";
    }

    @PostMapping("/courses/{id}/archive")
    public String archiveCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminCourseService.archiveCourse(id);
        redirectAttributes.addFlashAttribute("successMessage", "Course archived. Learning data has been retained.");
        return "redirect:/admin/courses";
    }

    @GetMapping("/categories")
    public String categories(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/categories", theme);
        model.addAttribute("categories", adminCourseService.categories());
        return "admin/categories/list";
    }

    @GetMapping("/categories/new")
    public String newCategory(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", new AdminCategoryForm());
        }
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/categories/new", theme);
        addCategoryFormOptions(model, null);
        model.addAttribute("editing", false);
        return "admin/categories/form";
    }

    @PostMapping("/categories")
    public String createCategory(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @ModelAttribute("categoryForm") AdminCategoryForm form,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "light") String theme,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (!bindingResult.hasErrors()) {
            try {
                adminCourseService.createCategory(form);
                redirectAttributes.addFlashAttribute("successMessage", "Category created.");
                return "redirect:/admin/categories";
            } catch (ResourceNotFoundException | IllegalArgumentException exception) {
                bindingResult.reject("categoryFormFailed", exception.getMessage());
            }
        }
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/categories/new", theme);
        addCategoryFormOptions(model, null);
        model.addAttribute("editing", false);
        return "admin/categories/form";
    }

    @GetMapping("/categories/{id}/edit")
    public String editCategory(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        requireTheme(theme);
        var category = adminCourseService.getCategory(id);
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", new AdminCategoryForm(category));
        }
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/categories/" + id + "/edit", theme);
        addCategoryFormOptions(model, id);
        model.addAttribute("category", category);
        model.addAttribute("editing", true);
        return "admin/categories/form";
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @ModelAttribute("categoryForm") AdminCategoryForm form,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "light") String theme,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        requireTheme(theme);
        if (!bindingResult.hasErrors()) {
            try {
                adminCourseService.updateCategory(id, form);
                redirectAttributes.addFlashAttribute("successMessage", "Category updated.");
                return "redirect:/admin/categories";
            } catch (ResourceNotFoundException | IllegalArgumentException exception) {
                bindingResult.reject("categoryFormFailed", exception.getMessage());
            }
        }
        addBaseModel(model, adminUserService.get(currentUser.getId()), "/admin/categories/" + id + "/edit", theme);
        addCategoryFormOptions(model, id);
        model.addAttribute("category", adminCourseService.getCategory(id));
        model.addAttribute("editing", true);
        return "admin/categories/form";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminCourseService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/categories";
    }

    private void addCourseFormOptions(Model model) {
        model.addAttribute("categories", adminCourseService.categories());
        model.addAttribute("courseStatuses", List.of(CourseStatus.values()));
    }

    private void addCategoryFormOptions(Model model, Long categoryId) {
        model.addAttribute("parentCategories", adminCourseService.categories().stream()
                .filter(category -> !category.getId().equals(categoryId))
                .toList());
    }

    private static void addBaseModel(Model model, User user, String path, String theme) {
        model.addAttribute("user", user);
        model.addAttribute("pagePath", path);
        model.addAttribute("theme", theme);
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("courseOverviewEnabled", false);
        model.addAttribute("courseSearchEnabled", false);
        model.addAttribute("adminPageEnabled", true);
    }

    private static UserStatus parseUserStatus(String value) {
        if ("all".equals(value)) {
            return null;
        }
        try {
            return UserStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported user status", exception);
        }
    }

    private static CourseStatus parseCourseStatus(String value) {
        if ("all".equals(value)) {
            return null;
        }
        try {
            return CourseStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported course status", exception);
        }
    }

    private static void requireTheme(String theme) {
        if (!Set.of("light", "dark").contains(theme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported theme");
        }
    }
}
