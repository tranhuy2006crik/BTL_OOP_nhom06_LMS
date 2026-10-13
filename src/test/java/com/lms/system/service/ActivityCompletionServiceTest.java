package com.lms.system.service;

import com.lms.system.repository.ActivityCompletionRepository;
import com.lms.system.repository.LmsWorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityCompletionServiceTest {

    @Mock
    private ActivityCompletionRepository completionRepository;

    @Mock
    private LmsWorkflowRepository workflowRepository;

    private ActivityCompletionService completionService;

    @BeforeEach
    void setUp() {
        completionService = new ActivityCompletionService(completionRepository, workflowRepository);
    }

    @Test
    void activeStudentCanMarkVisibleCourseActivityComplete() {
        when(workflowRepository.requireActiveUserId("student@example.com")).thenReturn(11L);
        when(workflowRepository.isActiveStudent(11L, 3L)).thenReturn(true);

        assertDoesNotThrow(() -> completionService.setCompleted(3L, 5L, 19L, true, "student@example.com"));

        verify(workflowRepository).requireCourseNotArchived(3L);
        verify(workflowRepository).requireSectionInCourse(5L, 3L);
        verify(completionRepository).setCompleted(3L, 5L, 19L, 11L, true);
    }

    @Test
    void teacherOrNonMemberCannotChangeStudentProgress() {
        when(workflowRepository.requireActiveUserId("teacher@example.com")).thenReturn(7L);
        when(workflowRepository.isActiveStudent(7L, 3L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> completionService.setCompleted(3L, 5L, 19L, true, "teacher@example.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(completionRepository, never()).setCompleted(3L, 5L, 19L, 7L, true);
    }

    @Test
    void archivedCourseCannotExposeProgressState() {
        when(workflowRepository.requireActiveUserId("student@example.com")).thenReturn(11L);
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(workflowRepository).requireCourseNotArchived(3L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> completionService.completedActivityIds(3L, 5L, "student@example.com")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(completionRepository, never()).findCompletedActivityIds(3L, 5L, 11L);
    }

    @Test
    void returnsOnlyCompletedActivityIds() {
        when(workflowRepository.requireActiveUserId("student@example.com")).thenReturn(11L);
        when(workflowRepository.isActiveStudent(11L, 3L)).thenReturn(true);
        when(completionRepository.findCompletedActivityIds(3L, 5L, 11L)).thenReturn(Set.of(19L, 21L));

        assertEquals(Set.of(19L, 21L),
                completionService.completedActivityIds(3L, 5L, "student@example.com"));
    }
}
