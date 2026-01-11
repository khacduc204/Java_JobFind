package com.example.JobFinder.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorPageController {

    @GetMapping("/403")
    public String accessDenied(Authentication authentication, Model model) {
        model.addAttribute("currentUser", authentication != null ? authentication.getName() : null);
        model.addAttribute("pageTitle", "403 - Truy cập bị từ chối");
        return "error/403";
    }
}
