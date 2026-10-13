package com.lms.system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "category_id", columnDefinition = "BIGINT UNSIGNED")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private CourseCategory category;

    @Column(name = "created_by", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User creator;

    @Column(name = "course_code", nullable = false, unique = true, length = 50)
    private String courseCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "start_at", columnDefinition = "datetime")
    private LocalDateTime startAt;

    @Column(name = "end_at", columnDefinition = "datetime")
    private LocalDateTime endAt;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime updatedAt;

    protected Course() {
    }

    public static Course create(
            String courseCode,
            String title,
            String description,
            CourseStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            CourseCategory category,
            Long creatorId
    ) {
        Course course = new Course();
        course.createdBy = creatorId;
        course.update(courseCode, title, description, status, startAt, endAt, category);
        return course;
    }

    public void update(
            String courseCode,
            String title,
            String description,
            CourseStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            CourseCategory category
    ) {
        if (courseCode == null || courseCode.isBlank() || courseCode.trim().length() > 50) {
            throw new IllegalArgumentException("Course code is required and must be 50 characters or fewer");
        }
        if (title == null || title.isBlank() || title.trim().length() > 200) {
            throw new IllegalArgumentException("Course title is required and must be 200 characters or fewer");
        }
        if (status == null) {
            throw new IllegalArgumentException("Course status is required");
        }
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("Course end date must be on or after its start date");
        }
        this.courseCode = courseCode.trim();
        this.title = title.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.status = status;
        this.startAt = startAt;
        this.endAt = endAt;
        this.category = category;
        this.categoryId = category == null ? null : category.getId();
    }

    public void archive() {
        this.status = CourseStatus.ARCHIVED;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public CourseCategory getCategory() {
        return category;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public User getCreator() {
        return creator;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public CourseStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
