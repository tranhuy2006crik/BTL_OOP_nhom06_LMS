package com.lms.system.dto;

public record StudentCourseProgress(
        Long courseId,
        String courseTitle,
        long totalActivities,
        long completedActivities
) {
    // Tính phần trăm hoàn thành; khóa học chưa có hoạt động được xem là 0% để tránh chia cho 0.
    public int percent() {
        if (totalActivities == 0) {
            return 0;
        }
        return (int) Math.round(100.0 * completedActivities / totalActivities);
    }
}
