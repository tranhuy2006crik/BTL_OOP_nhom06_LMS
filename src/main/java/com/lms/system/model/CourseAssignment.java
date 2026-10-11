package com.lms.system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "course_assignments")
public class CourseAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "course_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long courseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private Course course;

    @Column(name = "created_by", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User creator;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "due_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime dueAt;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore = 100;

    @Column(name = "allow_late", nullable = false)
    private boolean allowLate = false;

    @Column(name = "published", nullable = false)
    private boolean published = true;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime updatedAt;

    protected CourseAssignment() {
    }

    public static CourseAssignment create(
            Long courseId,
            Long createdBy,
            String title,
            String description,
            String instructions,
            LocalDateTime dueAt,
            Integer maxScore,
            boolean allowLate,
            boolean published
    ) {
        CourseAssignment assignment = new CourseAssignment();
        assignment.courseId = courseId;
        assignment.createdBy = createdBy;
        assignment.update(title, description, instructions, dueAt, maxScore, allowLate, published);
        return assignment;
    }

    public void update(
            String title,
            String description,
            String instructions,
            LocalDateTime dueAt,
            Integer maxScore,
            boolean allowLate,
            boolean published
    ) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course is required");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("Assignment creator is required");
        }
        if (title == null || title.isBlank() || title.trim().length() > 200) {
            throw new IllegalArgumentException("Assignment title is required and must be 200 characters or fewer");
        }
        if (dueAt == null) {
            throw new IllegalArgumentException("Submission deadline is required");
        }
        if (maxScore == null || maxScore <= 0) {
            throw new IllegalArgumentException("Maximum score must be greater than zero");
        }
        this.title = title.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.instructions = instructions == null || instructions.isBlank() ? null : instructions.trim();
        this.dueAt = dueAt;
        this.maxScore = maxScore;
        this.allowLate = allowLate;
        this.published = published;
    }

    public boolean isPastDue(LocalDateTime now) {
        return now != null && dueAt != null && now.isAfter(dueAt);
    }

    public boolean isAvailableForSubmission(LocalDateTime now) {
        return published && (dueAt == null || !isPastDue(now) || allowLate);
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

    public Long getCourseId() {
        return courseId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getInstructions() {
        return instructions;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public boolean isAllowLate() {
        return allowLate;
    }

    public boolean isPublished() {
        return published;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
