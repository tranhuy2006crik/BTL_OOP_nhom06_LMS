package com.lms.system.service;

import com.lms.system.dto.admin.AdminCategoryForm;
import com.lms.system.dto.admin.AdminCourseForm;
import com.lms.system.exception.ResourceNotFoundException;
import com.lms.system.model.Course;
import com.lms.system.model.CourseCategory;
import com.lms.system.model.CourseStatus;
import com.lms.system.repository.CourseCategoryRepository;
import com.lms.system.repository.CourseRepository;
import com.lms.system.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseService {

    private final CourseRepository courseRepository;
    private final CourseCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public AdminCourseService(
            CourseRepository courseRepository,
            CourseCategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<Course> searchCourses(String query, CourseStatus status, Pageable pageable) {
        return courseRepository.searchAdminCourses(query.trim(), status, pageable);
    }

    @Transactional(readOnly = true)
    public Course getCourse(Long id) {
        return courseRepository.findWithCategoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course was not found"));
    }

    @Transactional
    public Course createCourse(AdminCourseForm form, Long creatorId) {
        ensureCourseCodeAvailable(form.getCourseCode(), null);
        if (!userRepository.existsById(creatorId)) {
            throw new ResourceNotFoundException("Course creator was not found");
        }
        Course course = Course.create(
                form.getCourseCode(), form.getTitle(), form.getDescription(), form.getStatus(),
                form.getStartAt(), form.getEndAt(), category(form.getCategoryId()), creatorId
        );
        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long id, AdminCourseForm form) {
        Course course = getCourse(id);
        ensureCourseCodeAvailable(form.getCourseCode(), id);
        course.update(
                form.getCourseCode(), form.getTitle(), form.getDescription(), form.getStatus(),
                form.getStartAt(), form.getEndAt(), category(form.getCategoryId())
        );
        return courseRepository.save(course);
    }

    @Transactional
    public void archiveCourse(Long id) {
        Course course = getCourse(id);
        course.archive();
        courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public List<CourseCategory> categories() {
        return categoryRepository.findAllWithParent();
    }

    @Transactional(readOnly = true)
    public long countCategories() {
        return categoryRepository.count();
    }

    @Transactional(readOnly = true)
    public long countCourses(CourseStatus status) {
        return courseRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countCourses() {
        return courseRepository.count();
    }

    @Transactional(readOnly = true)
    public CourseCategory getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course category was not found"));
    }

    @Transactional
    public CourseCategory createCategory(AdminCategoryForm form) {
        CourseCategory parent = category(form.getParentId());
        CourseCategory category = CourseCategory.create(form.getName(), form.getDescription(), parent);
        return categoryRepository.save(category);
    }

    @Transactional
    public CourseCategory updateCategory(Long id, AdminCategoryForm form) {
        CourseCategory category = getCategory(id);
        CourseCategory parent = category(form.getParentId());
        ensureParentIsNotDescendant(category, parent);
        category.update(form.getName(), form.getDescription(), parent);
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        CourseCategory category = getCategory(id);
        if (categoryRepository.existsByParentId(id) || courseRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("Move or remove this category's child categories and courses first");
        }
        categoryRepository.delete(category);
    }

    private CourseCategory category(Long id) {
        if (id == null) {
            return null;
        }
        return getCategory(id);
    }

    private void ensureCourseCodeAvailable(String code, Long currentId) {
        boolean alreadyUsed = courseRepository.existsByCourseCodeIgnoreCase(code.trim());
        if (alreadyUsed && (currentId == null
                || !courseRepository.findById(currentId)
                .map(course -> course.getCourseCode().equalsIgnoreCase(code.trim()))
                .orElse(false))) {
            throw new IllegalArgumentException("Course code is already in use");
        }
    }

    private static void ensureParentIsNotDescendant(CourseCategory category, CourseCategory newParent) {
        CourseCategory candidate = newParent;
        while (candidate != null) {
            if (category.getId().equals(candidate.getId())) {
                throw new IllegalArgumentException("A category cannot be its own parent or a descendant's parent");
            }
            candidate = candidate.getParent();
        }
    }
}
