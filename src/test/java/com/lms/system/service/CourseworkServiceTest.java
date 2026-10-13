package com.lms.system.service;

import com.lms.system.dto.AssignmentForm;
import com.lms.system.repository.CourseworkRepository;
import com.lms.system.repository.CourseworkRepository.FileRecord;
import com.lms.system.repository.LmsWorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseworkServiceTest {

    @Mock
    private CourseworkRepository courseworkRepository;

    @Mock
    private LmsWorkflowRepository workflowRepository;

    @Mock
    private CourseFileStorageService fileStorage;

    private CourseworkService courseworkService;

    @BeforeEach
    void setUp() {
        courseworkService = new CourseworkService(courseworkRepository, workflowRepository, fileStorage);
    }

    @Test
    void studentCanSubmitTextToTheirActiveCourseAssignment() throws Exception {
        when(workflowRepository.requireActiveUserId("student@example.com")).thenReturn(11L);
        when(workflowRepository.canManageCourse(11L, 3L)).thenReturn(false);
        when(workflowRepository.isActiveStudent(11L, 3L)).thenReturn(true);
        when(fileStorage.storeAll(null)).thenReturn(List.of());
        when(courseworkRepository.submit(
                eq(3L), eq(19L), eq(11L), eq("My answer"), eq(List.of()), any(LocalDateTime.class)))
                .thenReturn(42L);

        Long submissionId = courseworkService.submit(
                3L, 19L, "  My answer  ", null, "student@example.com");

        assertEquals(42L, submissionId);
        verify(workflowRepository).requireCourseNotArchived(3L);
        verify(courseworkRepository).submit(
                eq(3L), eq(19L), eq(11L), eq("My answer"), eq(List.of()), any(LocalDateTime.class));
    }

    @Test
    void nonMemberCannotSubmitOrReadAssignmentData() throws Exception {
        when(workflowRepository.requireActiveUserId("visitor@example.com")).thenReturn(13L);
        when(workflowRepository.canManageCourse(13L, 3L)).thenReturn(false);
        when(workflowRepository.isActiveStudent(13L, 3L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseworkService.submit(3L, 19L, "Answer", null, "visitor@example.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(fileStorage, never()).storeAll(any());
        verify(courseworkRepository, never()).submit(
                eq(3L), eq(19L), eq(13L), any(), any(), any());
    }

    @Test
    void teacherCannotSubmitAsAStudent() throws Exception {
        when(workflowRepository.requireActiveUserId("teacher@example.com")).thenReturn(7L);
        when(workflowRepository.canManageCourse(7L, 3L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseworkService.submit(3L, 19L, "Answer", null, "teacher@example.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(fileStorage, never()).storeAll(any());
        verify(courseworkRepository, never()).submit(
                eq(3L), eq(19L), eq(7L), any(), any(), any());
    }

    @Test
    void studentCannotDownloadAnotherStudentsSubmissionFile() throws Exception {
        when(workflowRepository.requireActiveUserId("student@example.com")).thenReturn(11L);
        when(workflowRepository.canManageCourse(11L, 3L)).thenReturn(false);
        when(workflowRepository.isActiveStudent(11L, 3L)).thenReturn(true);
        when(courseworkRepository.findSubmissionFile(3L, 42L, 6L)).thenReturn(
                new FileRecord(6L, "key", "answer.pdf", "application/pdf", 10L,
                        "PUBLISHED", "VISIBLE", "VISIBLE", 22L));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseworkService.downloadSubmissionFile(
                        3L, 42L, 6L, "student@example.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(fileStorage, never()).read("key");
    }

    @Test
    void assignmentCreationRequiresCourseManager() throws Exception {
        when(workflowRepository.requireActiveUserId("student@example.com")).thenReturn(11L);
        when(workflowRepository.canManageCourse(11L, 3L)).thenReturn(false);
        when(workflowRepository.isActiveStudent(11L, 3L)).thenReturn(true);

        AssignmentForm form = new AssignmentForm();
        form.setTitle("Essay");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseworkService.createAssignment(3L, 5L, form, null, "student@example.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(fileStorage, never()).storeAll(any());
        verify(courseworkRepository, never()).createAssignment(
                eq(3L), eq(5L), eq(11L), any(), any(), any(), any(), anyBoolean());
    }
}
