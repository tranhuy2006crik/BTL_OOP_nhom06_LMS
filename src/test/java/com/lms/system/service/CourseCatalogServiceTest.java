package com.lms.system.service;

import org.junit.jupiter.api.Test;

import com.lms.system.model.Course;
import com.lms.system.model.CourseStatus;
import com.lms.system.repository.CourseRepository;
import com.lms.system.repository.GradebookRepository;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseCatalogServiceTest {

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final GradebookRepository gradebookRepository = mock(GradebookRepository.class);
    private final CourseCatalogService courseCatalogService =
            new CourseCatalogService(courseRepository, gradebookRepository);

    @Test
    void filtersSampleCoursesByStatusAndSearchesCourseAndCategory() {
        var courses = courseCatalogService.search("all", "health", "recent");

        assertEquals(2, courses.size());
        assertEquals("Dementia care: Caring for the Carers", courses.get(0).title());
        assertEquals("Chemical Nomenclature", courses.get(1).title());
    }

    @Test
    void sortsCoursesByCompletionInDescendingOrder() {
        var courses = courseCatalogService.search("in-progress", "", "progress");

        assertEquals("History: Russia in Revolution", courses.get(0).title());
        assertEquals(63, courses.get(0).completion());
    }

    @Test
    void searchesSampleCoursesByInstructorName() {
        var courses = courseCatalogService.search("all", "olivia bennett", "recent");

        assertEquals(1, courses.size());
        assertEquals("Psychology in Cinema", courses.get(0).title());
    }

    @Test
    void resolvesSampleCourseDetailsBySlug() {
        var course = courseCatalogService.findSampleCourse("foundations-of-data-science");

        assertEquals("Foundations of Data Science", course.title());
        assertEquals("Dr. Linh Pham", course.instructor());
    }

    @Test
    void searchesOnlyCoursesAssignedToCurrentUserAndIncludesInstructor() {
        Course course = mock(Course.class);
        when(course.getId()).thenReturn(24L);
        when(course.getTitle()).thenReturn("Real database course");
        when(course.getCourseCode()).thenReturn("REAL-24");
        when(course.getDescription()).thenReturn("Saved course");
        when(course.getCategory()).thenReturn(null);
        when(course.getStatus()).thenReturn(CourseStatus.PUBLISHED);
        when(course.getStartAt()).thenReturn(null);
        when(course.getEndAt()).thenReturn(null);
        when(courseRepository.findCoursesForUser(9L)).thenReturn(List.of(course));
        when(courseRepository.findActiveStudentCourseIds(9L)).thenReturn(List.of(24L));
        CourseRepository.CourseInstructorName instructor = mock(CourseRepository.CourseInstructorName.class);
        when(instructor.getCourseId()).thenReturn(24L);
        when(instructor.getInstructorName()).thenReturn("Alex Nguyen");
        when(courseRepository.findActiveInstructorNamesByCourseIds(List.of(24L))).thenReturn(List.of(instructor));
        when(gradebookRepository.findStudentProgress(9L)).thenReturn(List.of());

        var courses = courseCatalogService.searchForUser(9L, "all", "alex", "recent");

        assertEquals(1, courses.size());
        assertEquals(24L, courses.get(0).courseId());
        assertEquals("REAL-24", courses.get(0).courseCode());
        assertEquals("Alex Nguyen", courses.get(0).instructor());
        assertEquals("Real database course", courses.get(0).title());
    }
}
