package com.lms.system.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseTest {

    @Test
    void courseNormalizesFieldsAndCanBeArchived() {
        Course course = Course.create(
                "  CS101  ",
                "  Object Oriented Programming  ",
                "  Intro course ",
                CourseStatus.PUBLISHED,
                null,
                null,
                null,
                5L
        );

        assertEquals("CS101", course.getCourseCode());
        assertEquals("Object Oriented Programming", course.getTitle());
        assertEquals(CourseStatus.PUBLISHED, course.getStatus());

        course.archive();

        assertEquals(CourseStatus.ARCHIVED, course.getStatus());
    }

    @Test
    void courseRejectsEndDateBeforeStartDate() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 2, 9, 0);
        LocalDateTime end = start.minusDays(1);

        assertThrows(IllegalArgumentException.class, () -> Course.create(
                "CS101", "OOP", null, CourseStatus.DRAFT, start, end, null, 5L));
    }
}
