package com.example.JobFinder.controller;

import com.example.JobFinder.model.Candidate;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.ApplicationRepository;
import com.example.JobFinder.repository.CandidateRepository;
import com.example.JobFinder.repository.UserRepository;
import com.example.JobFinder.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;

@Controller
@RequestMapping("/candidate")
@RequiredArgsConstructor
public class CandidateController {

    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;
    private final JobService jobService;

    @PreAuthorize("hasRole('CANDIDATE')")
    @GetMapping("/dashboard")
    public String candidateDashboard(Authentication authentication, Model model) {
        Candidate candidate = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User currentUser = userRepository.findByEmailIgnoreCase(email).orElse(null);
            model.addAttribute("currentUser", currentUser);

            if (currentUser != null) {
                candidate = candidateRepository.findByUserId(currentUser.getId()).orElse(null);
                model.addAttribute("candidate", candidate);
                model.addAttribute("headline", candidate != null ? candidate.getHeadline() : null);
                model.addAttribute("location", candidate != null ? candidate.getLocation() : null);
                model.addAttribute("fullName", currentUser.getName() != null ? currentUser.getName() : currentUser.getEmail());

                int applicationsCount = candidate != null ? (int) applicationRepository.countByCandidateId(candidate.getId()) : 0;
                int savedJobsCount = candidate != null ? (int) jobService.countSavedJobsByCandidate(candidate.getId()) : 0;

                int profileCompletion = 40;
                if (candidate != null) {
                    if (candidate.getHeadline() != null && !candidate.getHeadline().isBlank()) {
                        profileCompletion += 15;
                    }
                    if (candidate.getLocation() != null && !candidate.getLocation().isBlank()) {
                        profileCompletion += 10;
                    }
                    if (candidate.getSkills() != null && !candidate.getSkills().isBlank()) {
                        profileCompletion += 20;
                    }
                    if (candidate.getSummary() != null && !candidate.getSummary().isBlank()) {
                        profileCompletion += 10;
                    }
                    if (candidate.getExperience() != null && !candidate.getExperience().isBlank()) {
                        profileCompletion += 10;
                    }
                    if (candidate.getCvPath() != null && !candidate.getCvPath().isBlank()) {
                        profileCompletion += 10;
                    }
                }
                profileCompletion = Math.min(100, profileCompletion);

                model.addAttribute("applicationsCount", applicationsCount);
                model.addAttribute("savedJobsCount", savedJobsCount);
                model.addAttribute("profileViews", 0); // Placeholder until tracking implemented
                model.addAttribute("profileCompletion", profileCompletion);

                if (candidate != null) {
                    model.addAttribute("recommendedJobs", jobService.getRecommendedJobsForCandidate(candidate.getId(), 4));
                } else {
                    model.addAttribute("recommendedJobs", Collections.emptyList());
                }
            }
        }
        return "frontend/candidate/dashboard";
    }
}
