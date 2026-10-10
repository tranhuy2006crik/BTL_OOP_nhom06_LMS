package com.lms.system.controller;

import com.lms.system.model.User;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.CourseCatalogService;
import com.lms.system.service.DashboardService;
import com.lms.system.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Set;

@Controller
public class DashboardController {

    private final ProfileService profileService;
    private final CourseCatalogService courseCatalogService;
    private final DashboardService dashboardService;

    public DashboardController(
            ProfileService profileService,
            CourseCatalogService courseCatalogService,
            DashboardService dashboardService
    ) {
        this.profileService = profileService;
        this.courseCatalogService = courseCatalogService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "in-progress") String status,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "card") String view,
            @RequestParam(defaultValue = "light") String theme,
            @RequestParam(defaultValue = "all") String timelineFilter,
            @RequestParam(defaultValue = "dates") String timelineSort,
            @RequestParam(defaultValue = "") String timelineQuery,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) String notice,
            Model model
    ) {
        requireChoice("status", status, Set.of("in-progress", "future", "past", "all"));
        requireChoice("sort", sort, Set.of("recent", "name-asc", "name-desc", "progress"));
        requireChoice("view", view, Set.of("card", "list"));
        requireChoice("theme", theme, Set.of("light", "dark"));
        requireChoice("timelineFilter", timelineFilter, Set.of("all", "upcoming", "past"));
        requireChoice("timelineSort", timelineSort, Set.of("dates", "latest"));

        YearMonth selectedMonth = parseMonth(month);
        int selectedDay;
        if (day == null) {
            if (selectedMonth.equals(YearMonth.now())) {
                selectedDay = java.time.LocalDate.now().getDayOfMonth();
            } else {
                selectedDay = 1;
            }
        } else {
            selectedDay = day;
        }
        if (selectedDay < 1 || selectedDay > selectedMonth.lengthOfMonth()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected day is outside the chosen month");
        }

        User user = profileService.getProfile(currentUser.getId());
        String query = q.trim();
        String normalizedTimelineQuery = timelineQuery.trim();
        var courses = courseCatalogService.search(status, query, sort);

        model.addAttribute("user", user);
        model.addAttribute("todayLabel", dashboardService.todayLabel());
        model.addAttribute("courseSearchEnabled", false);
        model.addAttribute("courseOverviewEnabled", false);
        model.addAttribute("dashboardEnabled", true);
        model.addAttribute("pagePath", "/dashboard");
        model.addAttribute("courses", courses);
        model.addAttribute("courseCount", courses.size());
        model.addAttribute("status", status);
        model.addAttribute("query", query);
        model.addAttribute("sort", sort);
        model.addAttribute("view", view);
        model.addAttribute("theme", theme);
        model.addAttribute("timelineFilter", timelineFilter);
        model.addAttribute("timelineSort", timelineSort);
        model.addAttribute("timelineQuery", normalizedTimelineQuery);
        model.addAttribute("activities",
                dashboardService.timeline(timelineFilter, normalizedTimelineQuery, timelineSort));
        model.addAttribute("weekdays", dashboardService.weekdays());
        model.addAttribute("calendarCells", dashboardService.calendar(selectedMonth, selectedDay));
        model.addAttribute("calendarMonth", selectedMonth.toString());
        model.addAttribute("calendarMonthLabel", dashboardService.monthLabel(selectedMonth));
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1).toString());
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1).toString());
        model.addAttribute("selectedDay", selectedDay);
        model.addAttribute("noticeMessage", noticeMessage(notice));
        return "dashboard";
    }

    private static YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must use YYYY-MM format", exception);
        }
    }

    private static void requireChoice(String parameter, String value, Set<String> allowedValues) {
        if (!allowedValues.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported " + parameter + " value");
        }
    }

    private static String noticeMessage(String notice) {
        if (notice == null) {
            return null;
        }
        return switch (notice) {
            case "grades" -> "Grades will be available when course grading is enabled.";
            case "messages" -> "Use the Messages page to contact active users.";
            case "private-files" -> "Private files will be available in a future update.";
            case "reports" -> "Reports will be available in a future update.";
            case "preferences" -> "Preferences will be available in a future update.";
            case "language" -> "Language options will be available in a future update.";
            case "notifications" -> "You’re all caught up — no new notifications.";
            case "course-options" -> "Course options will be connected when live course data is added.";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported notice");
        };
    }

}
