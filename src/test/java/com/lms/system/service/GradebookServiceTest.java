package com.lms.system.service;

import com.lms.system.dto.GradeForm;
import com.lms.system.repository.GradebookRepository;
import com.lms.system.repository.LmsWorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradebookServiceTest {

    @Mock
    private GradebookRepository gradebookRepository;

    @Mock
    private LmsWorkflowRepository workflowRepository;

    private GradebookService gradebookService;

    @BeforeEach
    void setUp() {
        gradebookService = new GradebookService(gradebookRepository, workflowRepository);
    }

    @Test
    void teacherCanSaveGradeForSubmittedWorkWithinMaximum() {
        GradeForm form = gradeForm("85.50", "Good work");
        when(workflowRepository.requireActiveUserId("teacher@example.com")).thenReturn(7L);
        when(workflowRepository.canManageCourse(7L, 3L)).thenReturn(true);
        when(gradebookRepository.lockSubmissionMaxGrade(3L, 19L)).thenReturn(new BigDecimal("100.00"));

        gradebookService.saveGrade(3L, 19L, form, "teacher@example.com");

        verify(gradebookRepository).saveGrade(19L, 7L, new BigDecimal("85.50"), "Good work");
    }

    @Test
    void scoreAboveAssignmentMaximumIsRejected() {
        GradeForm form = gradeForm("101", null);
        when(workflowRepository.requireActiveUserId("teacher@example.com")).thenReturn(7L);
        when(workflowRepository.canManageCourse(7L, 3L)).thenReturn(true);
        when(gradebookRepository.lockSubmissionMaxGrade(3L, 19L)).thenReturn(new BigDecimal("100.00"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gradebookService.saveGrade(3L, 19L, form, "teacher@example.com")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(gradebookRepository, never()).saveGrade(19L, 7L, form.getScore(), null);
    }

    @Test
    void studentCannotGradeCourseSubmission() {
        when(workflowRepository.requireActiveUserId("student@example.com")).thenReturn(11L);
        when(workflowRepository.canManageCourse(11L, 3L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gradebookService.saveGrade(
                        3L, 19L, gradeForm("80", null), "student@example.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(gradebookRepository, never()).lockSubmissionMaxGrade(3L, 19L);
    }

    @Test
    void releaseRequiresCourseManager() {
        when(workflowRepository.requireActiveUserId("student@example.com")).thenReturn(11L);
        when(workflowRepository.canManageCourse(11L, 3L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gradebookService.releaseGrade(3L, 19L, "student@example.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(gradebookRepository, never()).releaseGrade(3L, 19L);
    }

    private static GradeForm gradeForm(String score, String feedback) {
        GradeForm form = new GradeForm();
        form.setScore(new BigDecimal(score));
        form.setFeedback(feedback);
        return form;
    }
}
