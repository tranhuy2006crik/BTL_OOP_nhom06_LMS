package com.lms.system.service;

import com.lms.system.dto.AnnouncementRequest;
import com.lms.system.dto.AnnouncementResponse;
import com.lms.system.repository.LmsWorkflowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AnnouncementService {

    private final LmsWorkflowRepository repository;

    public AnnouncementService(LmsWorkflowRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AnnouncementResponse publish(Long courseId, AnnouncementRequest request, String email) {
        Long userId = repository.requireActiveUserId(email);
        requireManager(userId, courseId);
        return repository.createAnnouncement(courseId, userId, request.title(), request.content());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> list(Long courseId, String email) {
        Long userId = repository.requireActiveUserId(email);
        boolean canManage = repository.canManageCourse(userId, courseId);
        if (!canManage && !repository.isActiveStudent(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem thông báo khóa học");
        }
        return repository.findAnnouncements(courseId, canManage);
    }

    private void requireManager(Long userId, Long courseId) {
        if (!repository.canManageCourse(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ giảng viên của khóa học mới được đăng thông báo");
        }
    }
}