package com.example.JobFinder.controller;

import com.example.JobFinder.service.AdminDashboardService;
import com.example.JobFinder.service.AdminDashboardService.DashboardData;
import com.example.JobFinder.service.AdminDashboardService.MonthlyActivityPoint;
import com.example.JobFinder.service.AdminDashboardService.PipelineStatus;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public String adminDashboard(Authentication authentication, Model model) {
        DashboardData stats = adminDashboardService.buildDashboardData();
        String currentUser = authentication != null ? authentication.getName() : "Admin";

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("totalJobs", stats.totalJobs());
        model.addAttribute("activeJobs", stats.activeJobs());
        model.addAttribute("totalApplications", stats.totalApplications());
        model.addAttribute("applicationsLast30", stats.applicationsLast30());
        model.addAttribute("interviewRate", stats.interviewRate());
        model.addAttribute("shortlistedCount", stats.shortlistedCount());
        model.addAttribute("hireRate", stats.hireRate());
        model.addAttribute("hiredCount", stats.hiredCount());
        model.addAttribute("employerCount", stats.employerCount());
        model.addAttribute("candidateCount", stats.candidateCount());

        List<PipelineStatus> pipelineStatuses = stats.pipelineStatuses();
        List<MonthlyActivityPoint> monthlyActivity = stats.monthlyActivity();

        boolean hasPipelineData = pipelineStatuses.stream().anyMatch(status -> status.count() > 0);
        boolean hasActivityData = monthlyActivity.stream().anyMatch(MonthlyActivityPoint::hasData);
        boolean hasFeaturedEmployers = !stats.featuredEmployers().isEmpty();

        model.addAttribute("pipelineStatuses", pipelineStatuses);
        model.addAttribute("monthlyActivity", monthlyActivity);
        model.addAttribute("featuredEmployers", stats.featuredEmployers());
        model.addAttribute("hasPipelineData", hasPipelineData);
        model.addAttribute("hasActivityData", hasActivityData);
        model.addAttribute("hasFeaturedEmployers", hasFeaturedEmployers);

        model.addAttribute("pipelineChartData", pipelineStatuses.stream()
            .map(status -> Map.of(
                "label", status.label(),
                "count", status.count(),
                "color", status.color()
            ))
            .toList());

        model.addAttribute("activityChartData", monthlyActivity.stream()
            .map(point -> Map.of(
                "label", point.label(),
                "jobs", point.jobs(),
                "applications", point.applications(),
                "interviews", point.interviews(),
                "hires", point.hires()
            ))
            .toList());

        return "admin/dashboard";
    }
}
