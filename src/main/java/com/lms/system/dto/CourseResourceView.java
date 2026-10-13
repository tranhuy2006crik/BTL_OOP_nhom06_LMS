package com.lms.system.dto;

import java.util.List;

public record CourseResourceView(
        Long id,
        String title,
        String description,
        Integer position,
        String status,
        List<CourseFileView> files
) {
    public CourseResourceView {
        files = List.copyOf(files);
    }
}
