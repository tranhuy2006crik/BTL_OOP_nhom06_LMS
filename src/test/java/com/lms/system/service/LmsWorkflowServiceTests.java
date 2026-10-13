package com.lms.system.service;

import com.lms.system.dto.AnnouncementRequest;
import com.lms.system.dto.AnnouncementResponse;
import com.lms.system.dto.EnrollmentResponse;
import com.lms.system.dto.LessonRequest;
import com.lms.system.dto.LessonResponse;
import com.lms.system.repository.LmsWorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.lms.system.dto.CourseSectionRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LmsWorkflowServiceTests {

    @Mock
    private LmsWorkflowRepository repository;

    private LessonService lessonService;
    private EnrollmentService enrollmentService;
    private AnnouncementService announcementService;

    @BeforeEach
    void setUp() {
        lessonService = new LessonService(repository);
        enrollmentService = new EnrollmentService(repository);
        announcementService = new AnnouncementService(repository);
    }

    @Test
    void teacherCanCreateLesson() {
        LessonRequest request = new LessonRequest("Introduction", "Basics", "Lesson text");
        LessonResponse expected = new LessonResponse(
                31L, "Introduction", "Basics", "Lesson text", 1, "VISIBLE", null, null);
        when(repository.requireActiveUserId("teacher@example.com")).thenReturn(8L);
        when(repository.canManageCourse(8L, 4L)).thenReturn(true);
        when(repository.createLesson(4L, 9L, 8L, "Introduction", "Basics", "Lesson text"))
                .thenReturn(expected);

        LessonResponse actual = lessonService.create(4L, 9L, request, "teacher@example.com");

        assertEquals(expected, actual);
        verify(repository).requireSectionInCourse(9L, 4L);
    }

    @Test
    void nonMemberCannotViewLessons() {
        when(repository.requireActiveUserId("outsider@example.com")).thenReturn(12L);
        when(repository.canManageCourse(12L, 4L)).thenReturn(false);
        when(repository.isActiveStudent(12L, 4L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lessonService.list(4L, 9L, "outsider@example.com"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(repository, never()).findLessons(4L, 9L, false);
    }

    @Test
    void enrollmentUsesCurrentAuthenticatedEmailAndPublishedCourse() {
        EnrollmentResponse expected = new EnrollmentResponse(4L, 12L, "ACTIVE", LocalDateTime.now());
        when(repository.requireActiveUserId("student@example.com")).thenReturn(12L);
        when(repository.enroll(4L, 12L)).thenReturn(expected);

        EnrollmentResponse actual = enrollmentService.enroll(4L, "student@example.com");

        assertEquals(expected, actual);
        verify(repository).requirePublishedCourse(4L);
        verify(repository).enroll(4L, 12L);
    }

    @Test
    void nonTeacherCannotPublishAnnouncement() {
        when(repository.requireActiveUserId("student@example.com")).thenReturn(12L);
        when(repository.canManageCourse(12L, 4L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> announcementService.publish(
                        4L, new AnnouncementRequest("Exam", "Exam date"), "student@example.com"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(repository, never()).createAnnouncement(4L, 12L, "Exam", "Exam date");
    }

    @Test
    void studentAnnouncementListHidesDraftsAtRepositoryBoundary() {
        when(repository.requireActiveUserId("student@example.com")).thenReturn(12L);
        when(repository.canManageCourse(12L, 4L)).thenReturn(false);
        when(repository.isActiveStudent(12L, 4L)).thenReturn(true);
        when(repository.findAnnouncements(4L, false)).thenReturn(List.of());

        List<AnnouncementResponse> announcements = announcementService.list(4L, "student@example.com");

        assertEquals(List.of(), announcements);
        verify(repository).findAnnouncements(4L, false);
    }

    @Test
    void archivedCourseBlocksLessonAndAnnouncementFlows() {
        when(repository.requireActiveUserId("teacher@example.com")).thenReturn(8L);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học"))
                .when(repository).requireCourseNotArchived(4L);

        assertThrows(ResponseStatusException.class,
                () -> lessonService.list(4L, 9L, "teacher@example.com"));
        assertThrows(ResponseStatusException.class,
                () -> lessonService.createSection(4L, new CourseSectionRequest("Section", null), "teacher@example.com"));
        assertThrows(ResponseStatusException.class,
                () -> lessonService.create(4L, 9L,
                        new LessonRequest("Lesson", null, "Content"), "teacher@example.com"));
        assertThrows(ResponseStatusException.class,
                () -> lessonService.update(4L, 9L, 31L,
                        new LessonRequest("Lesson", null, "Content"), "teacher@example.com"));
        assertThrows(ResponseStatusException.class,
                () -> lessonService.delete(4L, 9L, 31L, "teacher@example.com"));
        assertThrows(ResponseStatusException.class,
                () -> announcementService.publish(4L,
                        new AnnouncementRequest("Title", "Content"), "teacher@example.com"));
        assertThrows(ResponseStatusException.class,
                () -> announcementService.list(4L, "teacher@example.com"));

        verify(repository, times(7)).requireCourseNotArchived(4L);
        verify(repository, never()).canManageCourse(8L, 4L);
        verify(repository, never()).findLessons(4L, 9L, true);
        verify(repository, never()).findAnnouncements(4L, true);
    }
}
