package com.lms.system.controller;

import com.lms.system.dto.AnnouncementRequest;
import com.lms.system.dto.AnnouncementResponse;
import com.lms.system.service.AnnouncementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse publish(
            @PathVariable Long courseId,
            @Valid @RequestBody AnnouncementRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        return announcementService.publish(courseId, request, user.getUsername());
    }

    @GetMapping
    public List<AnnouncementResponse> list(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails user
    ) {
        return announcementService.list(courseId, user.getUsername());
    }
}