package com.lms.system.service;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardServiceTest {

    private final DashboardService dashboardService = new DashboardService();

    @Test
    void buildsMondayFirstCalendarGridWithSelectedDayAndEvents() {
        var cells = dashboardService.calendar(YearMonth.of(2026, 10), 5);

        assertEquals(35, cells.size());
        assertEquals("", cells.get(0).day());
        assertEquals("1", cells.get(3).day());
        assertTrue(cells.get(7).selected());
        assertTrue(cells.get(5).hasEvent());
        assertEquals("October 2026", dashboardService.monthLabel(YearMonth.of(2026, 10)));
    }

    @Test
    void filtersTimelineByActivityTextAndSortsByDate() {
        var activities = dashboardService.timeline("all", "window", "latest");

        assertEquals(1, activities.size());
        assertEquals("(Mobile assignment) View from your window", activities.get(0).title());
        assertEquals("Wednesday, 23 December 2026", activities.get(0).formattedDate());
    }
}
