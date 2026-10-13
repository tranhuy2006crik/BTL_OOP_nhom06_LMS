package com.lms.system.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lms.system.dto.LessonRequest;
import com.lms.system.dto.LessonResponse;
import com.lms.system.repository.LmsWorkflowRepository;

@Service
public class LessonService {

    private final LmsWorkflowRepository repository;

    public LessonService(LmsWorkflowRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> list(Long courseId, Long sectionId, String email) {
        Long userId = repository.requireActiveUserId(email);
        boolean canManage = repository.canManageCourse(userId, courseId);
        // Người xem phải là quản lý khóa hoặc học viên đang ghi danh; visibility được lọc trong repository.
        if (!canManage && !repository.isActiveStudent(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem nội dung khóa học");
        }
        repository.requireSectionInCourse(sectionId, courseId);
        return repository.findLessons(courseId, sectionId, canManage);
    }

    // Ghi activity và phần nội dung lesson trong cùng transaction để không tạo dữ liệu thiếu một nửa.
    @Transactional
    public LessonResponse create(Long courseId, Long sectionId, LessonRequest request, String email) {
        Long userId = requireManager(courseId, email);
        repository.requireSectionInCourse(sectionId, courseId);
        return repository.createLesson(
                courseId, sectionId, userId, request.title(), request.description(), request.content());
    }

    @Transactional
    public LessonResponse update(
            Long courseId,
            Long sectionId,
            Long lessonId,
            LessonRequest request,
            String email
    ) {
        requireManager(courseId, email);
        return repository.updateLesson(
                courseId, sectionId, lessonId, request.title(), request.description(), request.content());
    }

    @Transactional
    public void delete(Long courseId, Long sectionId, Long lessonId, String email) {
        requireManager(courseId, email);
        repository.hideLesson(courseId, sectionId, lessonId);
    }

    private Long requireManager(Long courseId, String email) {
        Long userId = repository.requireActiveUserId(email);
        // Kiểm tra quyền ở service để controller không phải chứa quy tắc nghiệp vụ.
        if (!repository.canManageCourse(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ giảng viên của khóa học mới được quản lý bài học");
        }
        return userId;
    }
}