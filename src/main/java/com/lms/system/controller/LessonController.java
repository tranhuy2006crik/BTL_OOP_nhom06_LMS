package com.lms.system.controller;

import com.lms.system.dto.LessonRequest;
import com.lms.system.dto.LessonResponse;
import com.lms.system.service.LessonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/sections/{sectionId}/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping
    public List<LessonResponse> list(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @AuthenticationPrincipal UserDetails user
    ) {
        return lessonService.list(courseId, sectionId, user.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResponse create(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody LessonRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        return lessonService.create(courseId, sectionId, request, user.getUsername());
    }

    @PutMapping("/{lessonId}")
    public LessonResponse update(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        return lessonService.update(courseId, sectionId, lessonId, request, user.getUsername());
    }

    @DeleteMapping("/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetails user
    ) {
        lessonService.delete(courseId, sectionId, lessonId, user.getUsername());
    }
}