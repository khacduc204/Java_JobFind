package com.example.JobFinder.controller;

import com.example.JobFinder.service.EmployerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/employers")
@RequiredArgsConstructor
public class EmployerFrontendController {

    private final EmployerService employerService;

    @GetMapping
    public String listEmployers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, defaultValue = "featured") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            Model model) {
        
        int perPage = 6;
        
        // Page metadata
        model.addAttribute("pageTitle", "Danh sách nhà tuyển dụng uy tín | JobFind");
        model.addAttribute("pageStyles", java.util.List.of("employers.css"));
        
        // Get employers with pagination
        var result = employerService.getDirectoryPaginated(q, location, sort, page, perPage);
        
        model.addAttribute("companies", result.get("rows"));
        model.addAttribute("totalEmployers", result.get("total"));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.get("totalPages"));
        model.addAttribute("searchTerm", q != null ? q : "");
        model.addAttribute("locationFilter", location != null ? location : "");
        model.addAttribute("sortOrder", sort);
        
        // Directory stats
        model.addAttribute("directoryStats", employerService.getDirectoryStats());
        
        return "frontend/employers/index";
    }

    @GetMapping("/{id}")
    public String showEmployer(@PathVariable Integer id, Model model) {
        // Convert Integer to Long for service call
        Long employerId = id.longValue();
        
        // Page metadata
        model.addAttribute("pageStyles", java.util.List.of("employer-profile.css"));
        
        // Get employer details
        var employer = employerService.getEmployerProfile(employerId);
        
        if (employer == null) {
            return "redirect:/employers";
        }
        
        model.addAttribute("employer", employer);
        model.addAttribute("pageTitle", employer.get("company_name") + " | Hồ sơ nhà tuyển dụng JobFind");
        
        // Get employer jobs
        model.addAttribute("jobs", employerService.getEmployerJobs(employerId));
        
        // Get employer statistics
        model.addAttribute("stats", employerService.getEmployerStats(employerId));
        
        // Get benefits and culture
        model.addAttribute("benefits", employerService.getEmployerBenefits(employerId));
        model.addAttribute("cultureHighlights", employerService.getCultureHighlights(employer));
        
        // Get hiring timeline
        model.addAttribute("hiringTimeline", employerService.getHiringTimeline(employerId));
        
        // Related jobs (from other employers)
        model.addAttribute("relatedJobs", employerService.getRelatedJobs(employerId, 3));
        
        return "frontend/employers/show";
    }
}
