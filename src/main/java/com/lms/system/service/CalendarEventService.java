package com.lms.system.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CalendarEventService {

    private static final List<String> WEEKDAYS = List.of("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat");

    private static final List<CalendarEvent> SAMPLE_EVENTS = List.of(
            new CalendarEvent(LocalDate.of(2026, 10, 5), "09:00", "Welcome to your learning calendar",
                    "Select dates to see events scheduled for that day.", EventCategory.SITE, null),
            new CalendarEvent(LocalDate.of(2026, 10, 5), "11:00", "Course overview and orientation",
                    "Introduction to the learning space.", EventCategory.COURSE, "Foundations of Learning"),
            new CalendarEvent(LocalDate.of(2026, 10, 12), "15:00", "Study group: Getting started",
                    "An optional group study session.", EventCategory.GROUP, "Foundations of Learning"),
            new CalendarEvent(LocalDate.of(2026, 10, 23), "23:59", "Submit your first assignment",
                    "Sample due date for the course.", EventCategory.COURSE, "Foundations of Learning"),
            new CalendarEvent(LocalDate.of(2026, 10, 28), "10:00", "Personal study reminder",
                    "A sample event in your personal calendar.", EventCategory.USER, null)
    );

    public List<String> weekdays() {
        return WEEKDAYS;
    }

    public List<CalendarDay> calendar(YearMonth month, int selectedDay, Set<EventCategory> visibleCategories) {
        LocalDate firstDay = month.atDay(1);
        int leadingBlankCells = firstDay.getDayOfWeek().getValue() % DayOfWeek.SUNDAY.getValue();
        int cellCount = ((leadingBlankCells + month.lengthOfMonth() + 6) / 7) * 7;
        List<CalendarDay> cells = new ArrayList<>(cellCount);

        for (int index = 0; index < cellCount; index++) {
            int dayNumber = index - leadingBlankCells + 1;
            if (dayNumber < 1 || dayNumber > month.lengthOfMonth()) {
                cells.add(new CalendarDay("", false, false, false));
                continue;
            }
            LocalDate date = month.atDay(dayNumber);
            boolean hasEvents = SAMPLE_EVENTS.stream()
                    .anyMatch(event -> event.date().equals(date) && visibleCategories.contains(event.category()));
            cells.add(new CalendarDay(
                    Integer.toString(dayNumber),
                    dayNumber == selectedDay,
                    date.equals(LocalDate.now()),
                    hasEvents
            ));
        }
        return List.copyOf(cells);
    }

    public List<CalendarEvent> eventsFor(LocalDate date, Set<EventCategory> visibleCategories) {
        return SAMPLE_EVENTS.stream()
                .filter(event -> event.date().equals(date))
                .filter(event -> visibleCategories.contains(event.category()))
                .sorted(Comparator.comparing(CalendarEvent::time))
                .toList();
    }

    public String monthLabel(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
    }

    public String dateLabel(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + ", "
                + date.getDayOfMonth()
                + " "
                + date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " "
                + date.getYear();
    }

    public record CalendarDay(String day, boolean selected, boolean today, boolean hasEvents) {
    }

    public record CalendarEvent(
            LocalDate date,
            String time,
            String title,
            String description,
            EventCategory category,
            String courseName
    ) {
    }

    public enum EventCategory {
        SITE("site", "Site events", "event-site"),
        CATEGORY("category", "Category events", "event-category"),
        COURSE("course", "Course events", "event-course"),
        GROUP("group", "Group events", "event-group"),
        USER("user", "User events", "event-user"),
        OTHER("other", "Other events", "event-other");

        private final String code;
        private final String label;
        private final String cssClass;

        EventCategory(String code, String label, String cssClass) {
            this.code = code;
            this.label = label;
            this.cssClass = cssClass;
        }

        public String code() {
            return code;
        }

        public String label() {
            return label;
        }

        public String cssClass() {
            return cssClass;
        }

        public static EventCategory fromCode(String code) {
            for (EventCategory category : values()) {
                if (category.code.equals(code)) {
                    return category;
                }
            }
            return null;
        }
    }
}
