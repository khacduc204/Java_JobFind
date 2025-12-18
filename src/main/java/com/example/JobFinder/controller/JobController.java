package com.example.JobFinder.controller;

import com.example.JobFinder.model.User;
import com.example.JobFinder.model.Candidate;
import com.example.JobFinder.repository.CategoryRepository;
import com.example.JobFinder.repository.CandidateRepository;
import com.example.JobFinder.repository.UserRepository;
import com.example.JobFinder.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {
    
    private final JobService jobService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    
    /**
     * Get candidate ID from authenticated user
     */
    private Integer getCandidateId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    Optional<Candidate> candidateOpt = candidateRepository.findByUserId(user.getId());
                    return candidateOpt.map(Candidate::getId).orElse(null);
                }
            }
        } catch (Exception e) {
            return null;
        }
        
        return null;
    }
    
    /**
     * Trang danh sách việc làm đã lưu (shortcut URL)
     */
    @GetMapping("/saved")
    public String savedJobs(
            @RequestParam(required = false, defaultValue = "1") int page,
            Authentication authentication,
            Model model) {
        
        // Redirect to main jobs page with saved=true parameter
        Integer candidateId = getCandidateId(authentication);
        
        if (candidateId == null) {
            // Not logged in or not a candidate - redirect to login
            return "redirect:/auth/login";
        }
        
        return listJobs(null, null, null, null, "newest", page, true, authentication, model);
    }
    
    /**
     * Trang danh sách việc làm
     */
    @GetMapping
    public String listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "false") boolean saved,
            Authentication authentication,
            Model model) {
        
        int perPage = 6;
        
        // Get current candidate ID
        Integer candidateId = getCandidateId(authentication);
        boolean canSaveJobs = candidateId != null;
        
        Map<String, Object> result;
        
        if (saved && canSaveJobs) {
            // Show saved jobs
            result = jobService.getSavedJobsByCandidate(candidateId, page, perPage);
            model.addAttribute("pageTitle", "Việc làm đã lưu | JobFind");
            model.addAttribute("headingTitle", "Việc làm đã lưu");
            model.addAttribute("showSaved", true);
        } else {
            // Show all jobs with filters
            result = jobService.getPublishedJobsWithFilters(
                keyword, location, employmentType, categoryId, sort, page, perPage
            );
            model.addAttribute("pageTitle", "Danh sách việc làm mới nhất | JobFind");
            model.addAttribute("headingTitle", "Việc làm mới nhất");
            model.addAttribute("showSaved", false);
        }
        
        // Add result data to model
        model.addAttribute("jobs", result.get("jobs"));
        model.addAttribute("total", result.get("total"));
        model.addAttribute("totalPages", result.get("totalPages"));
        model.addAttribute("currentPage", result.get("currentPage"));
        model.addAttribute("perPage", result.get("perPage"));
        
        // Add filters to model
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("location", location != null ? location : "");
        model.addAttribute("employmentType", employmentType != null ? employmentType : "");
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        
        // Get categories for filter dropdown
        model.addAttribute("categories", categoryRepository.findAllOrderByName());
        
        // Employment types
        model.addAttribute("employmentTypes", List.of(
            Map.of("value", "Full-time", "label", "Toàn thời gian"),
            Map.of("value", "Part-time", "label", "Bán thời gian"),
            Map.of("value", "Remote", "label", "Làm việc từ xa"),
            Map.of("value", "Intern", "label", "Thực tập")
        ));
        
        // User info
        model.addAttribute("canSaveJobs", canSaveJobs);
        if (canSaveJobs && candidateId != null) {
            List<Integer> savedJobIds = jobService.getSavedJobIds(candidateId);
            model.addAttribute("savedJobIds", savedJobIds);
        }
        
        // Calculate quick stats for current result
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jobsList = (List<Map<String, Object>>) result.get("jobs");
        long fullTimeCount = jobsList.stream()
            .filter(job -> "Full-time".equalsIgnoreCase(String.valueOf(job.get("employmentType"))))
            .count();
        long remoteCount = jobsList.stream()
            .filter(job -> "Remote".equalsIgnoreCase(String.valueOf(job.get("employmentType"))))
            .count();
        
        model.addAttribute("fullTimeCount", fullTimeCount);
        model.addAttribute("remoteCount", remoteCount);
        
        return "frontend/jobs/index";
    }

    
    /**
     * Trang việc làm hot (sắp xếp theo lượt xem)
     */
    @GetMapping("/hot")
    public String hotJobs(
            @RequestParam(required = false, defaultValue = "1") int page,
            Authentication authentication,
            Model model) {
        
        int perPage = 24;
        
        // Get hot jobs sorted by view count
        Map<String, Object> result = jobService.getHotJobs(page, perPage);
        
        model.addAttribute("jobs", result.get("jobs"));
        model.addAttribute("total", result.get("total"));
        model.addAttribute("totalPages", result.get("totalPages"));
        model.addAttribute("currentPage", result.get("currentPage"));
        model.addAttribute("perPage", result.get("perPage"));
        
        model.addAttribute("pageTitle", "Việc làm mới đăng | JobFind");
        
        // User info
        Integer candidateId = getCandidateId(authentication);
        boolean canSaveJobs = candidateId != null;
        model.addAttribute("canSaveJobs", canSaveJobs);
        
        if (canSaveJobs) {
            List<Integer> savedJobIds = jobService.getSavedJobIds(candidateId);
            model.addAttribute("savedJobIds", savedJobIds);
        }
        
        return "frontend/jobs/hot";
    }    
    /**
     * Chi tiết công việc (công khai)
     */
    @GetMapping("/{id}")
    public String showJobDetail(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        
        try {
            Integer candidateId = getCandidateId(authentication);
            
            // Get job detail with employer info
            Map<String, Object> jobDetail = jobService.getJobDetailById(id, candidateId);
            
            if (jobDetail == null || jobDetail.isEmpty()) {
                redirectAttributes.addFlashAttribute("flashType", "danger");
                redirectAttributes.addFlashAttribute("flashMessage", "Không tìm thấy tin tuyển dụng");
                return "redirect:/jobs";
            }
            
            model.addAttribute("job", jobDetail);
            model.addAttribute("isLoggedIn", authentication != null && authentication.isAuthenticated());
            model.addAttribute("candidateId", candidateId);
            model.addAttribute("pageTitle", jobDetail.get("title") + " - JobFind");
            
            // Check if user has saved this job
            boolean canSaveJobs = candidateId != null;
            boolean isSaved = false;
            
            if (canSaveJobs) {
                isSaved = jobService.isJobSavedByCandidate(candidateId, id);
            }
            
            model.addAttribute("canSaveJobs", canSaveJobs);
            model.addAttribute("isSaved", isSaved);
            
            // Add candidate info for apply widget
            if (candidateId != null) {
                com.example.JobFinder.model.Candidate candidate = candidateRepository.findById(candidateId).orElse(null);
                if (candidate != null) {
                    String candidateName = (candidate.getUser() != null && candidate.getUser().getName() != null) 
                        ? candidate.getUser().getName() 
                        : (candidate.getUser() != null ? candidate.getUser().getEmail() : "Ứng viên");
                    model.addAttribute("candidateName", candidateName);
                    model.addAttribute("candidateEmail", candidate.getUser() != null ? candidate.getUser().getEmail() : "");
                    model.addAttribute("candidateLocation", candidate.getLocation() != null ? candidate.getLocation() : "");
                    model.addAttribute("hasCv", candidate.getCvPath() != null && !candidate.getCvPath().isEmpty());
                    model.addAttribute("cvFileName", candidate.getCvPath() != null ? new java.io.File(candidate.getCvPath()).getName() : "");
                } else {
                    model.addAttribute("candidateName", authentication.getName());
                    model.addAttribute("candidateEmail", authentication.getName());
                    model.addAttribute("candidateLocation", "");
                    model.addAttribute("hasCv", false);
                }
            }
            
            // Flash message
            if (session.getAttribute("flashMessage") != null) {
                model.addAttribute("flashMessage", session.getAttribute("flashMessage"));
                model.addAttribute("flashType", session.getAttribute("flashType"));
                session.removeAttribute("flashMessage");
                session.removeAttribute("flashType");
            }
            
            return "frontend/jobs/detail";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Có lỗi xảy ra");
            return "redirect:/jobs";
        }
    }    
    /**
     * Save/unsave job (AJAX endpoint)
     */
    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> toggleSaveJob(
            @RequestParam Integer jobId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Must be logged in as candidate
        Integer candidateId = getCandidateId(authentication);
        
        if (candidateId == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để lưu việc làm");
            return response;
        }
        
        try {
            boolean saved = jobService.toggleSaveJob(candidateId, jobId);
            response.put("success", true);
            response.put("saved", saved);
            response.put("message", saved ? "Đã lưu việc làm" : "Đã bỏ lưu việc làm");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return response;
    }
}
