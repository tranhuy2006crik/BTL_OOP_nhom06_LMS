package com.lms.system.service;

import com.lms.system.service.CalendarEventService.EventCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarEventServiceTest {

    private final CalendarEventService calendarEventService = new CalendarEventService();

    @Test
    void buildsSundayFirstMonthGridAndMarksSelectedDay() {
        var cells = calendarEventService.calendar(
                YearMonth.of(2026, 10),
                5,
                EnumSet.allOf(EventCategory.class)
        );

        assertEquals(35, cells.size());
        assertEquals("1", cells.get(4).day());
        assertTrue(cells.get(8).selected());
        assertTrue(cells.get(8).hasEvents());
        assertEquals("Sun", calendarEventService.weekdays().get(0));
    }

    @Test
    void returnsOnlySelectedDayEventsInSelectedCategories() {
        var allEvents = calendarEventService.eventsFor(
                LocalDate.of(2026, 10, 5),
                EnumSet.allOf(EventCategory.class)
        );
        var courseEvents = calendarEventService.eventsFor(
                LocalDate.of(2026, 10, 5),
                EnumSet.of(EventCategory.COURSE)
        );

        assertEquals(2, allEvents.size());
        assertEquals(1, courseEvents.size());
        assertEquals("Course overview and orientation", courseEvents.get(0).title());
        assertFalse(courseEvents.get(0).category().equals(EventCategory.SITE));
    }
}
