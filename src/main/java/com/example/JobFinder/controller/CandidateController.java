package com.example.JobFinder.controller;

import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/candidate")
@RequiredArgsConstructor
public class CandidateController {

    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String candidateDashboard(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User currentUser = userRepository.findByEmailIgnoreCase(email).orElse(null);
            model.addAttribute("currentUser", currentUser);
        }
        return "frontend/candidate/dashboard";
    }
}
