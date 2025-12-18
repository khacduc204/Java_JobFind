package com.example.JobFinder.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public String adminDashboard(Authentication authentication, Model model) {
        model.addAttribute("currentUser", authentication.getName());
        
        // Dashboard statistics - placeholder data
        model.addAttribute("totalJobs", 0);
        model.addAttribute("activeJobs", 0);
        model.addAttribute("totalApplications", 0);
        model.addAttribute("applicationsLast30", 0);
        model.addAttribute("interviewRate", 0.0);
        model.addAttribute("shortlistedCount", 0);
        model.addAttribute("hireRate", 0.0);
        model.addAttribute("hiredCount", 0);
        model.addAttribute("employerCount", 0);
        model.addAttribute("candidateCount", 0);
        
        return "admin/dashboard";
    }
}
