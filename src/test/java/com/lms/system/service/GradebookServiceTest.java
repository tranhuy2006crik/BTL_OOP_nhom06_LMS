package com.lms.system.service;

import com.lms.system.dto.GradeForm;
import com.lms.system.dto.StudentCourseProgress;
import com.lms.system.dto.StudentGradeRow;
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
import static org.mockito.Mockito.doThrow;
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

    @Test
    void archivedCourseCannotBeGraded() {
        when(workflowRepository.requireActiveUserId("teacher@example.com")).thenReturn(7L);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(workflowRepository).requireCourseNotArchived(3L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gradebookService.saveGrade(3L, 19L, gradeForm("80", null), "teacher@example.com")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(gradebookRepository, never()).saveGrade(19L, 7L, new BigDecimal("80"), null);
    }

    @Test
    void studentOverviewSummarizesOnlyPublishedScoresAndProgress() {
        when(workflowRepository.requireActiveUserId("student@example.com")).thenReturn(11L);
        when(gradebookRepository.findStudentGrades(11L)).thenReturn(java.util.List.of(
                gradeRow(1L, new BigDecimal("8.00"), new BigDecimal("10.00")),
                gradeRow(2L, new BigDecimal("15.00"), new BigDecimal("20.00")),
                gradeRow(3L, null, new BigDecimal("100.00"))
        ));
        when(gradebookRepository.findStudentProgress(11L)).thenReturn(java.util.List.of(
                new StudentCourseProgress(3L, "Java", 4L, 2L),
                new StudentCourseProgress(4L, "OOP", 0L, 0L)
        ));

        GradebookService.StudentOverview overview = gradebookService.studentOverview("student@example.com");

        assertEquals(2L, overview.publishedGradeCount());
        assertEquals(new BigDecimal("77.5"), overview.averagePublishedPercentage());
        assertEquals(2L, overview.completedActivityCount());
        assertEquals(4L, overview.totalActivityCount());
        assertEquals(50, overview.overallProgressPercent());
    }

    private static StudentGradeRow gradeRow(Long assignmentId, BigDecimal score, BigDecimal maxGrade) {
        return new StudentGradeRow(
                3L, "Java", assignmentId, "Assignment " + assignmentId, maxGrade, null,
                assignmentId, 1, "SUBMITTED", null, score, null, null);
    }

    private static GradeForm gradeForm(String score, String feedback) {
        GradeForm form = new GradeForm();
        form.setScore(new BigDecimal(score));
        form.setFeedback(feedback);
        return form;
    }
}
