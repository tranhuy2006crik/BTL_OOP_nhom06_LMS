package com.lms.system.controller;

import com.lms.system.model.User;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.service.CalendarEventService;
import com.lms.system.service.CalendarEventService.EventCategory;
import com.lms.system.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Controller
public class CalendarController {

    private final ProfileService profileService;
    private final CalendarEventService calendarEventService;

    public CalendarController(ProfileService profileService, CalendarEventService calendarEventService) {
        this.profileService = profileService;
        this.calendarEventService = calendarEventService;
    }

    @GetMapping("/calendar")
    public String calendar(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false, name = "visible") List<String> visibleCodes,
            @RequestParam(defaultValue = "light") String theme,
            Model model
    ) {
        if (!Set.of("light", "dark").contains(theme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported theme");
        }

        YearMonth selectedMonth = parseMonth(month);
        int selectedDay;
        if (day == null) {
            if (selectedMonth.equals(YearMonth.now())) {
                selectedDay = LocalDate.now().getDayOfMonth();
            } else {
                selectedDay = 1;
            }
        } else {
            selectedDay = day;
        }
        if (selectedDay < 1 || selectedDay > selectedMonth.lengthOfMonth()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected day is outside the chosen month");
        }

        Set<EventCategory> visibleCategories = parseVisibleCategories(visibleCodes);
        LocalDate selectedDate = selectedMonth.atDay(selectedDay);
        User user = profileService.getProfile(currentUser.getId());

        model.addAttribute("user", user);
        model.addAttribute("theme", theme);
        model.addAttribute("pagePath", "/calendar");
        model.addAttribute("dashboardEnabled", false);
        model.addAttribute("courseOverviewEnabled", false);
        model.addAttribute("courseSearchEnabled", false);
        model.addAttribute("calendarEnabled", true);
        model.addAttribute("calendarMonth", selectedMonth.toString());
        model.addAttribute("calendarMonthLabel", calendarEventService.monthLabel(selectedMonth));
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1).toString());
        model.addAttribute("previousMonthLabel", calendarEventService.monthLabel(selectedMonth.minusMonths(1)));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1).toString());
        model.addAttribute("nextMonthLabel", calendarEventService.monthLabel(selectedMonth.plusMonths(1)));
        model.addAttribute("selectedDay", selectedDay);
        model.addAttribute("selectedDateLabel", calendarEventService.dateLabel(selectedDate));
        model.addAttribute("weekdays", calendarEventService.weekdays());
        model.addAttribute("calendarCells", calendarEventService.calendar(selectedMonth, selectedDay, visibleCategories));
        model.addAttribute("visibleCategories", visibleCategories);
        model.addAttribute("visibleCategoryCodes", visibleCategories.stream()
                .map(EventCategory::code)
                .toList());
        model.addAttribute("eventCategories", EventCategory.values());
        model.addAttribute("selectedEvents", calendarEventService.eventsFor(selectedDate, visibleCategories));
        return "calendar";
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

    private static Set<EventCategory> parseVisibleCategories(List<String> visibleCodes) {
        if (visibleCodes == null) {
            return EnumSet.allOf(EventCategory.class);
        }
        EnumSet<EventCategory> categories = EnumSet.noneOf(EventCategory.class);
        for (String code : visibleCodes) {
            EventCategory category = EventCategory.fromCode(code);
            if (category == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported event category");
            }
            categories.add(category);
        }
        return categories;
    }
}