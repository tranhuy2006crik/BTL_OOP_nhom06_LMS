package com.lms.system.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.lms.system.dto.StudentCourseProgress;
import com.lms.system.model.Course;
import com.lms.system.repository.CourseRepository;
import com.lms.system.repository.GradebookRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDateTime;

@Service
public class CourseCatalogService {

    private static final List<CourseCard> SAMPLE_COURSES = List.of(
            new CourseCard("dementia-care", "Dementia care: Caring for the Carers", "Faculty of Health and Medicine",
                    "Dr. Amelia Carter", "Learn practical approaches to supporting people living with dementia and the carers around them.",
                    1, "in-progress", "dementia", "❋", false, null, null, false),
            new CourseCard("celebrating-cultures", "Celebrating Cultures", "Our Community",
                    "Professor Minh Nguyen", "Explore cultural identity, community traditions, and ways to build inclusive spaces.",
                    27, "in-progress", "cultures", "◎", false, null, null, false),
            new CourseCard("psychology-in-cinema", "Psychology in Cinema", "Communication and Media",
                    "Dr. Olivia Bennett", "Discover how films portray memory, emotion, motivation, and human behaviour.",
                    42, "in-progress", "cinema", "◉", true, null, null, false),
            new CourseCard("the-impressionists", "The Impressionists", "Communication and Media",
                    "Lecturer An Tran", "Study the artists, techniques, and historical context behind Impressionist painting.",
                    0, "in-progress", "impressionists", "✿", false, null, null, false),
            new CourseCard("chemical-nomenclature", "Chemical Nomenclature", "Faculty of Health and Medicine",
                    "Dr. Sofia Patel", "Learn the conventions used to name chemical compounds and write their formulas.",
                    18, "in-progress", "chemistry", "⚗", false, null, null, false),
            new CourseCard("russia-in-revolution", "History: Russia in Revolution", "History",
                    "Professor Daniel Brooks", "Examine the political and social forces that shaped the Russian Revolution.",
                    63, "in-progress", "history", "✥", false, null, null, false),
            new CourseCard("foundations-of-data-science", "Foundations of Data Science", "Science and Technology",
                    "Dr. Linh Pham", "Build foundations in data analysis, visualization, and responsible interpretation.",
                    0, "future", "data-science", "⌘", false, null, null, false),
            new CourseCard("academic-writing-essentials", "Academic Writing Essentials", "Languages",
                    "Lecturer Emma Wilson", "Practice planning, structuring, and revising clear academic writing.",
                    100, "past", "writing", "✎", false, null, null, false)
    );

    private final CourseRepository courseRepository;
    private final GradebookRepository gradebookRepository;

    public CourseCatalogService(CourseRepository courseRepository, GradebookRepository gradebookRepository) {
        this.courseRepository = courseRepository;
        this.gradebookRepository = gradebookRepository;
    }

    public List<CourseCard> searchForUser(Long userId, String status, String query, String sort) {
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        List<Course> userCourses = courseRepository.findCoursesForUser(userId);
        List<Long> courseIds = userCourses.stream().map(Course::getId).toList();
        Map<Long, String> instructors = courseIds.isEmpty() ? Map.of()
                : courseRepository.findActiveInstructorNamesByCourseIds(courseIds).stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                CourseRepository.CourseInstructorName::getCourseId,
                                java.util.stream.Collectors.mapping(
                                        CourseRepository.CourseInstructorName::getInstructorName,
                                        java.util.stream.Collectors.joining(", ")
                                )
                        ));
        List<Long> studentCourseIds = courseRepository.findActiveStudentCourseIds(userId);
        Map<Long, Integer> completionByCourse = new HashMap<>();
        for (StudentCourseProgress progress : gradebookRepository.findStudentProgress(userId)) {
            completionByCourse.put(progress.courseId(), progress.percent());
        }

        return userCourses.stream()
                .map(course -> toCourseCard(course, instructors.get(course.getId()),
                        studentCourseIds.contains(course.getId()), completionByCourse.get(course.getId())))
                .filter(course -> status.equals("all") || status.equals(course.status()))
                .filter(course -> normalizedQuery.isEmpty()
                        || course.title().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || course.courseCode().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || course.category().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || course.instructor().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(comparatorFor(sort))
                .toList();
    }

    public List<CourseCard> publishedCatalog(Long userId, String query) {
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        List<Course> availableCourses = courseRepository.findPublishedCoursesAvailableToUser(userId);
        List<Long> courseIds = availableCourses.stream().map(Course::getId).toList();
        Map<Long, String> instructors = courseIds.isEmpty() ? Map.of()
                : courseRepository.findActiveInstructorNamesByCourseIds(courseIds).stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                CourseRepository.CourseInstructorName::getCourseId,
                                java.util.stream.Collectors.mapping(
                                        CourseRepository.CourseInstructorName::getInstructorName,
                                        java.util.stream.Collectors.joining(", ")
                                )
                        ));
        return availableCourses.stream()
                .map(course -> toCourseCard(course, instructors.get(course.getId()), false, null))
                .filter(course -> normalizedQuery.isEmpty()
                        || course.title().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || course.courseCode().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || course.category().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || course.instructor().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .toList();
    }

    private static CourseCard toCourseCard(
            Course course, String instructor, boolean trackProgress, Integer completion
    ) {
        String category = course.getCategory() == null ? "Uncategorized" : course.getCategory().getName();
        return new CourseCard(
                String.valueOf(course.getId()),
                course.getTitle(),
                category,
                instructor == null || instructor.isBlank() ? "No instructor assigned" : instructor,
                course.getDescription() == null ? "" : course.getDescription(),
                completion == null ? 0 : completion,
                courseFilterStatus(course),
                artworkFor(course.getId()),
                "✦",
                false,
                course.getId(),
                course.getCourseCode(),
                trackProgress
        );
    }

    private static String courseFilterStatus(Course course) {
        LocalDateTime now = LocalDateTime.now();
        if (course.getStartAt() != null && course.getStartAt().isAfter(now)) {
            return "future";
        }
        if (course.getEndAt() != null && course.getEndAt().isBefore(now)) {
            return "past";
        }
        return "in-progress";
    }

    private static String artworkFor(Long id) {
        String[] artwork = {"dementia", "cultures", "cinema", "impressionists", "chemistry",
                "history", "data-science", "writing"};
        return artwork[Math.floorMod(id.hashCode(), artwork.length)];
    }

    public List<CourseCard> search(String status, String query, String sort) {
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return SAMPLE_COURSES.stream()
                .filter(course -> status.equals("all") || status.equals(course.status()))
                .filter(course -> normalizedQuery.isEmpty()
                        || course.title().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || course.category().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || course.instructor().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(comparatorFor(sort))
                .toList();
    }

    public CourseCard findSampleCourse(String slug) {
        return SAMPLE_COURSES.stream()
                .filter(course -> course.slug().equals(slug))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy khóa học mẫu"));
    }

    private static Comparator<CourseCard> comparatorFor(String sort) {
        return switch (sort) {
            case "name-asc" -> Comparator.comparing(CourseCard::title, String.CASE_INSENSITIVE_ORDER);
            case "name-desc" -> Comparator.comparing(CourseCard::title, String.CASE_INSENSITIVE_ORDER).reversed();
            case "progress" -> Comparator.comparingInt(CourseCard::completion).reversed();
            default -> Comparator.comparing(CourseCard::courseId,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    public record CourseCard(
            String slug,
            String title,
            String category,
            String instructor,
            String description,
            int completion,
            String status,
            String artwork,
            String symbol,
            boolean starred,
            Long courseId,
            String courseCode,
            boolean trackProgress
    ) {
    }
}
