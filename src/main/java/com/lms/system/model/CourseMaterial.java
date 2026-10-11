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
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_materials")
public class CourseMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "course_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long courseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private Course course;

    @Column(name = "uploaded_by", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", insertable = false, updatable = false)
    private User uploader;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "uploaded_at", nullable = false, updatable = false, columnDefinition = "datetime")
    private LocalDateTime uploadedAt;

    protected CourseMaterial() {
    }

    public static CourseMaterial create(
            Long courseId,
            Long uploadedBy,
            String title,
            String description,
            String fileName,
            String originalFileName,
            String mimeType,
            long fileSize,
            String storagePath
    ) {
        if (courseId == null || uploadedBy == null) {
            throw new IllegalArgumentException("Course and uploader are required");
        }
        if (title == null || title.isBlank() || title.trim().length() > 200) {
            throw new IllegalArgumentException("Material title is required and must be 200 characters or fewer");
        }
        if (fileName == null || fileName.isBlank() || storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Material file information is required");
        }
        CourseMaterial material = new CourseMaterial();
        material.courseId = courseId;
        material.uploadedBy = uploadedBy;
        material.title = title.trim();
        material.description = description == null || description.isBlank() ? null : description.trim();
        material.fileName = fileName;
        material.originalFileName = originalFileName == null || originalFileName.isBlank() ? fileName : originalFileName;
        material.mimeType = mimeType;
        material.fileSize = fileSize;
        material.storagePath = storagePath;
        return material;
    }

    @PrePersist
    void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}
