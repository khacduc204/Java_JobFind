package com.example.JobFinder.controller;

import com.example.JobFinder.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            Model model) {
        
        // Page metadata
        model.addAttribute("pageTitle", "JobFind - Nền tảng việc làm chuẩn TopCV");
        model.addAttribute("pageStyles", List.of("home.css"));
        model.addAttribute("pageScripts", List.of("homepage.js"));
        
        // Get popular keywords
        model.addAttribute("searchKeywords", homeService.getPopularKeywords(6));
        
        // Prefilled search values
        model.addAttribute("prefilledKeyword", keyword != null ? keyword : "");
        model.addAttribute("prefilledLocation", location != null ? location : "");
        
        // Hero metrics (statistics)
        Map<String, Object> stats = homeService.getStatistics();
        model.addAttribute("heroMetrics", homeService.formatHeroMetrics(stats));
        
        // Hero categories (top 3 for sidebar)
        model.addAttribute("heroCategories", homeService.getTopCategories(3));
        
        // Highlight cards
        model.addAttribute("highlightCards", homeService.getHighlightCards());
        
        // Top categories (6 for main section)
        model.addAttribute("topCategories", homeService.getTopCategories(6));
        
        // Hot jobs
        model.addAttribute("hotJobs", homeService.getHotJobs(4));
        
        // Top employers
        model.addAttribute("topEmployers", homeService.getTopEmployers(6));
        
        // Blog articles
        model.addAttribute("blogArticles", homeService.getBlogArticles());
        
        return "frontend/home";
    }
}
