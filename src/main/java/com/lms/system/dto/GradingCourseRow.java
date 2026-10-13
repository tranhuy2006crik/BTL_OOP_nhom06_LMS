package com.lms.system.dto;

public record GradingCourseRow(
        Long courseId,
        String courseTitle,
        long submissionCount,
        long ungradedCount,
        long awaitingReleaseCount
) {
}
