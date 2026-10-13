package com.lms.system.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseCatalogServiceTest {

    private final CourseCatalogService courseCatalogService = new CourseCatalogService();

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
}
