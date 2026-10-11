package com.lms.system.service;

import com.lms.system.model.AssignmentSubmission;
import com.lms.system.model.CourseAssignment;
import com.lms.system.model.SubmissionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseAssignmentServiceTest {

    @Test
    void assignmentMarksLateWhenDueDatePassed() {
        LocalDateTime now = LocalDateTime.now();
        CourseAssignment assignment = CourseAssignment.create(
                10L,
                20L,
                "Assignment 1",
                "Complete exercise",
                "Read chapter 1",
                now.minusDays(1),
                10,
                true,
                true
        );

        assertTrue(assignment.isPastDue(now));
        assertEquals(SubmissionStatus.LATE, AssignmentSubmission.resolveSubmissionStatus(assignment, now));
    }

    @Test
    void assignmentIsOnTimeBeforeDueDate() {
        LocalDateTime now = LocalDateTime.now();
        CourseAssignment assignment = CourseAssignment.create(
                10L,
                20L,
                "Assignment 2",
                "Complete exercise",
                "Read chapter 2",
                now.plusDays(2),
                10,
                false,
                true
        );

        assertEquals(SubmissionStatus.SUBMITTED, AssignmentSubmission.resolveSubmissionStatus(assignment, now));
    }
}
