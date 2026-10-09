package com.lms.system.service;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CourseCatalogService {

    private static final List<CourseCard> SAMPLE_COURSES = List.of(
            new CourseCard("Dementia care: Caring for the Carers", "Faculty of Health and Medicine",
                    1, "in-progress", "dementia", "❋", false),
            new CourseCard("Celebrating Cultures", "Our Community",
                    27, "in-progress", "cultures", "◎", false),
            new CourseCard("Psychology in Cinema", "Communication and Media",
                    42, "in-progress", "cinema", "◉", true),
            new CourseCard("The Impressionists", "Communication and Media",
                    0, "in-progress", "impressionists", "✿", false),
            new CourseCard("Chemical Nomenclature", "Faculty of Health and Medicine",
                    18, "in-progress", "chemistry", "⚗", false),
            new CourseCard("History: Russia in Revolution", "History",
                    63, "in-progress", "history", "✥", false),
            new CourseCard("Foundations of Data Science", "Science and Technology",
                    0, "future", "data-science", "⌘", false),
            new CourseCard("Academic Writing Essentials", "Languages",
                    100, "past", "writing", "✎", false)
    );

    public List<CourseCard> search(String status, String query, String sort) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        return SAMPLE_COURSES.stream()
                .filter(course -> status.equals("all") || status.equals(course.status()))
                .filter(course -> normalizedQuery.isEmpty()
                        || course.title().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || course.category().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(comparatorFor(sort))
                .toList();
    }

    private static Comparator<CourseCard> comparatorFor(String sort) {
        return switch (sort) {
            case "name-asc" -> Comparator.comparing(CourseCard::title, String.CASE_INSENSITIVE_ORDER);
            case "name-desc" -> Comparator.comparing(CourseCard::title, String.CASE_INSENSITIVE_ORDER).reversed();
            case "progress" -> Comparator.comparingInt(CourseCard::completion).reversed();
            default -> Comparator.comparingInt(SAMPLE_COURSES::indexOf);
        };
    }

    public record CourseCard(
            String title,
            String category,
            int completion,
            String status,
            String artwork,
            String symbol,
            boolean starred
    ) {
    }
}
