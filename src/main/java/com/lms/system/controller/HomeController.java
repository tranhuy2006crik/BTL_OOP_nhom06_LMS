package com.lms.system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/courses/open")
    public String openCourse(
            @RequestParam Long courseId,
            @RequestParam Long sectionId
    ) {
        return "redirect:/courses/" + courseId + "/sections/" + sectionId;
    }
}
