package com.lms.system.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DashboardService {

    private static final List<String> WEEKDAYS = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
    private static final Set<LocalDate> CALENDAR_EVENTS = Set.of(
            LocalDate.of(2026, 10, 3),
            LocalDate.of(2026, 10, 5)
    );
    private static final List<TimelineActivity> SAMPLE_ACTIVITIES = List.of(
            new TimelineActivity(
                    LocalDate.of(2026, 12, 23),
                    "06:00",
                    "(Mobile assignment) View from your window",
                    "Assignment is due · Celebrating Cultures",
                    "assignment"
            ),
            new TimelineActivity(
                    LocalDate.of(2027, 7, 23),
                    "06:00",
                    "What do you already know?",
                    "Assignment is due · Cross-cultural Communication",
                    "assignment"
            )
    );

    public List<String> weekdays() {
        return WEEKDAYS;
    }

    public List<CalendarCell> calendar(YearMonth month, int selectedDay) {
        LocalDate firstDay = month.atDay(1);
        int leadingBlankCells = firstDay.getDayOfWeek().getValue() - 1;
        int cellCount = ((leadingBlankCells + month.lengthOfMonth() + 6) / 7) * 7;
        List<CalendarCell> cells = new ArrayList<>(cellCount);

        for (int index = 0; index < cellCount; index++) {
            int dayNumber = index - leadingBlankCells + 1;
            if (dayNumber < 1 || dayNumber > month.lengthOfMonth()) {
                cells.add(new CalendarCell("", false, false, false));
                continue;
            }
            LocalDate date = month.atDay(dayNumber);
            cells.add(new CalendarCell(
                    Integer.toString(dayNumber),
                    dayNumber == selectedDay,
                    date.equals(LocalDate.now()),
                    CALENDAR_EVENTS.contains(date)
            ));
        }
        return List.copyOf(cells);
    }

    public List<TimelineActivity> timeline(String filter, String query, String sort) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now();
        Comparator<TimelineActivity> timelineComparator;
        if ("latest".equals(sort)) {
            timelineComparator = Comparator.comparing(TimelineActivity::dueDate).reversed();
        } else {
            timelineComparator = Comparator.comparing(TimelineActivity::dueDate);
        }
        return SAMPLE_ACTIVITIES.stream()
                .filter(activity -> switch (filter) {
                    case "upcoming" -> !activity.dueDate().isBefore(today);
                    case "past" -> activity.dueDate().isBefore(today);
                    default -> true;
                })
                .filter(activity -> normalizedQuery.isEmpty()
                        || activity.title().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || activity.courseLabel().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(timelineComparator)
                .toList();
    }

    public String monthLabel(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " "
                + month.getYear();
    }

    public String todayLabel() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH));
    }

    public record CalendarCell(String day, boolean selected, boolean today, boolean hasEvent) {
    }

    public record TimelineActivity(
            LocalDate dueDate,
            String dueTime,
            String title,
            String courseLabel,
            String type
    ) {
        public String formattedDate() {
            return dueDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + ", "
                    + dueDate.getDayOfMonth()
                    + " "
                    + dueDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + " "
                    + dueDate.getYear();
        }
    }
}
