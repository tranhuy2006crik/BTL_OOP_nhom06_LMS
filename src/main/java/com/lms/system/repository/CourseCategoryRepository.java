package com.lms.system.repository;

import com.lms.system.model.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {

    boolean existsByParentId(Long parentId);

    @Query("SELECT c FROM CourseCategory c LEFT JOIN FETCH c.parent ORDER BY LOWER(c.name), c.id")
    List<CourseCategory> findAllWithParent();
}
