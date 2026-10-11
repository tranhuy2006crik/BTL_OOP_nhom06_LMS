package com.lms.system.repository;

import com.lms.system.model.CourseMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Long> {

    List<CourseMaterial> findByCourseIdOrderByUploadedAtDesc(Long courseId);

    Optional<CourseMaterial> findByIdAndCourseId(Long id, Long courseId);
}
