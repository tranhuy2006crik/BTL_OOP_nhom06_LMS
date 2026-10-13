package com.lms.system.dto.admin;

import com.lms.system.model.Course;
import com.lms.system.model.CourseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminCourseForm {

    @NotBlank(message = "Course code is required")
    @Size(max = 50)
    private String courseCode;

    @NotBlank(message = "Course title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 10000)
    private String description;

    @NotNull(message = "Choose a course status")
    private CourseStatus status = CourseStatus.DRAFT;

    private Long categoryId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endAt;

    private List<Long> instructorIds = new ArrayList<>();

    public AdminCourseForm() {
    }

    public AdminCourseForm(Course course) {
        this.courseCode = course.getCourseCode();
        this.title = course.getTitle();
        this.description = course.getDescription();
        this.status = course.getStatus();
        this.categoryId = course.getCategoryId();
        this.startAt = course.getStartAt();
        this.endAt = course.getEndAt();
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CourseStatus getStatus() {
        return status;
    }

    public void setStatus(CourseStatus status) {
        this.status = status;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public List<Long> getInstructorIds() {
        return instructorIds;
    }

    public void setInstructorIds(List<Long> instructorIds) {
        this.instructorIds = instructorIds == null ? new ArrayList<>() : instructorIds;
    }
}
