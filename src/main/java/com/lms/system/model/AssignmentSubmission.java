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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "assignment_submissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_assignment_submission_student", columnNames = {"assignment_id", "student_id"})
)
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "assignment_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", insertable = false, updatable = false)
    private CourseAssignment assignment;

    @Column(name = "student_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @Column(name = "submitted_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime submittedAt;

    @Column(name = "text_answer", columnDefinition = "TEXT")
    private String textAnswer;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    protected AssignmentSubmission() {
    }

    public static AssignmentSubmission create(
            Long assignmentId,
            Long studentId,
            String textAnswer,
            String attachmentName,
            String attachmentPath,
            LocalDateTime now
    ) {
        if (assignmentId == null || studentId == null) {
            throw new IllegalArgumentException("Assignment and student are required");
        }
        AssignmentSubmission submission = new AssignmentSubmission();
        submission.assignmentId = assignmentId;
        submission.studentId = studentId;
        submission.textAnswer = textAnswer == null || textAnswer.isBlank() ? null : textAnswer.trim();
        submission.attachmentName = attachmentName;
        submission.attachmentPath = attachmentPath;
        submission.submittedAt = now == null ? LocalDateTime.now() : now;
        submission.status = SubmissionStatus.SUBMITTED;
        return submission;
    }

    public static SubmissionStatus resolveSubmissionStatus(CourseAssignment assignment, LocalDateTime now) {
        if (assignment == null || assignment.getDueAt() == null) {
            return SubmissionStatus.SUBMITTED;
        }
        return now != null && now.isAfter(assignment.getDueAt()) ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED;
    }

    public void markSubmitted(LocalDateTime now) {
        this.submittedAt = now == null ? LocalDateTime.now() : now;
        this.status = SubmissionStatus.SUBMITTED;
    }

    public void grade(Integer grade, String feedback) {
        if (grade != null && (grade < 0 || grade > 100)) {
            throw new IllegalArgumentException("Grade must be between 0 and 100");
        }
        this.grade = grade;
        this.feedback = feedback == null || feedback.isBlank() ? null : feedback.trim();
        this.status = grade == null ? SubmissionStatus.SUBMITTED : SubmissionStatus.GRADED;
    }

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getTextAnswer() {
        return textAnswer;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public Integer getGrade() {
        return grade;
    }

    public String getFeedback() {
        return feedback;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }
}
