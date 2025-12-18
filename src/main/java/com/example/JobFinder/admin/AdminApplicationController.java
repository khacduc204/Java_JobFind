package com.example.JobFinder.admin;

import com.example.JobFinder.model.Application;
import com.example.JobFinder.repository.ApplicationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/applications")
public class AdminApplicationController {

    private final ApplicationRepository applicationRepository;

    public AdminApplicationController(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String listApplications(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false) Integer jobId,
            @RequestParam(required = false) Integer employerId,
            @RequestParam(required = false, defaultValue = "1") int page,
            Model model,
            HttpSession session) {

        // Get flash message if any
        if (session.getAttribute("admin_application_flash") != null) {
            @SuppressWarnings("unchecked")
            Map<String, String> flash = (Map<String, String>) session.getAttribute("admin_application_flash");
            model.addAttribute("flashType", flash.get("type"));
            model.addAttribute("flashMessage", flash.get("message"));
            session.removeAttribute("admin_application_flash");
        }

        // Pagination
        int pageSize = 20;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "appliedAt"));

        // Get applications with filters
        Page<Application> applicationPage = applicationRepository.findAllWithFilters(
            keyword, status, jobId, employerId, pageable
        );

        List<Application> applications = applicationPage.getContent();
        int totalPages = applicationPage.getTotalPages();

        // Calculate statistics
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("applied", applicationRepository.countByStatus("applied"));
        statusCounts.put("viewed", applicationRepository.countByStatus("viewed"));
        statusCounts.put("shortlisted", applicationRepository.countByStatus("shortlisted"));
        statusCounts.put("rejected", applicationRepository.countByStatus("rejected"));
        statusCounts.put("hired", applicationRepository.countByStatus("hired"));
        
        long totalCount = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        
        // Last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long last7Days = applicationRepository.countSince(sevenDaysAgo);
        
        // Last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long last30Days = applicationRepository.countSince(thirtyDaysAgo);
        
        // Awaiting review (applied + viewed)
        long awaitingReview = statusCounts.get("applied") + statusCounts.get("viewed");

        // Prepare application data
        List<Map<String, Object>> applicationList = applications.stream().map(app -> {
            Map<String, Object> appData = new HashMap<>();
            appData.put("id", app.getId());
            appData.put("status", app.getStatus());
            appData.put("appliedAt", app.getAppliedAt());
            appData.put("coverLetter", app.getCoverLetter());
            
            // Candidate info
            if (app.getCandidate() != null) {
                appData.put("candidateName", app.getCandidate().getUser() != null ? 
                    app.getCandidate().getUser().getName() : "N/A");
                appData.put("candidateEmail", app.getCandidate().getUser() != null ? 
                    app.getCandidate().getUser().getEmail() : "");
                appData.put("candidateId", app.getCandidate().getId());
            } else {
                appData.put("candidateName", "N/A");
                appData.put("candidateEmail", "");
                appData.put("candidateId", null);
            }
            
            // Job info
            if (app.getJob() != null) {
                appData.put("jobTitle", app.getJob().getTitle());
                appData.put("jobId", app.getJob().getId());
                
                // Employer info
                if (app.getJob().getEmployer() != null) {
                    appData.put("employerName", app.getJob().getEmployer().getCompanyName());
                    appData.put("employerId", app.getJob().getEmployer().getId());
                } else {
                    appData.put("employerName", "N/A");
                    appData.put("employerId", null);
                }
            } else {
                appData.put("jobTitle", "N/A");
                appData.put("jobId", null);
                appData.put("employerName", "N/A");
                appData.put("employerId", null);
            }
            
            return appData;
        }).collect(Collectors.toList());

        // Status breakdown for chart
        Map<String, Object> statusBreakdown = new HashMap<>();
        for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
            long count = entry.getValue();
            double percentage = totalCount > 0 ? (count * 100.0 / totalCount) : 0;
            
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("count", count);
            statusData.put("percentage", Math.round(percentage * 10) / 10.0);
            
            statusBreakdown.put(entry.getKey(), statusData);
        }

        model.addAttribute("applications", applicationList);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalApplications", totalCount);
        model.addAttribute("last7Days", last7Days);
        model.addAttribute("last30Days", last30Days);
        model.addAttribute("awaitingReview", awaitingReview);
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("statusBreakdown", statusBreakdown);
        model.addAttribute("filterKeyword", keyword);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterJobId", jobId);
        model.addAttribute("filterEmployerId", employerId);

        return "admin/applications/index";
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String showApplication(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Application> applicationOpt = applicationRepository.findByIdWithDetails(id);
        
        if (applicationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Không tìm thấy hồ sơ ứng tuyển");
            return "redirect:/admin/applications";
        }

        Application application = applicationOpt.get();
        
        // Prepare application data
        Map<String, Object> appData = new HashMap<>();
        appData.put("id", application.getId());
        appData.put("status", application.getStatus());
        appData.put("appliedAt", application.getAppliedAt());
        appData.put("coverLetter", application.getCoverLetter());
        appData.put("resumeSnapshot", application.getResumeSnapshot());
        
        // Candidate info
        if (application.getCandidate() != null) {
            Map<String, Object> candidateData = new HashMap<>();
            candidateData.put("id", application.getCandidate().getId());
            candidateData.put("headline", application.getCandidate().getHeadline());
            candidateData.put("summary", application.getCandidate().getSummary());
            candidateData.put("location", application.getCandidate().getLocation());
            candidateData.put("skills", application.getCandidate().getSkills());
            candidateData.put("experience", application.getCandidate().getExperience());
            
            if (application.getCandidate().getUser() != null) {
                candidateData.put("name", application.getCandidate().getUser().getName());
                candidateData.put("email", application.getCandidate().getUser().getEmail());
                candidateData.put("phone", application.getCandidate().getUser().getPhone());
            }
            
            appData.put("candidate", candidateData);
        }
        
        // Job info
        if (application.getJob() != null) {
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("id", application.getJob().getId());
            jobData.put("title", application.getJob().getTitle());
            jobData.put("description", application.getJob().getDescription());
            jobData.put("location", application.getJob().getLocation());
            jobData.put("salary", application.getJob().getSalary());
            jobData.put("employmentType", application.getJob().getEmploymentType());
            jobData.put("status", application.getJob().getStatus());
            
            if (application.getJob().getEmployer() != null) {
                jobData.put("employerName", application.getJob().getEmployer().getCompanyName());
                jobData.put("employerId", application.getJob().getEmployer().getId());
                jobData.put("employerEmail", application.getJob().getEmployer().getUser() != null ? 
                    application.getJob().getEmployer().getUser().getEmail() : "");
            }
            
            appData.put("job", jobData);
        }

        model.addAttribute("application", appData);
        
        return "admin/applications/show";
    }

    @PostMapping("/{id}/update-status")
    @Transactional
    public String updateStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            HttpSession session) {

        try {
            Optional<Application> applicationOpt = applicationRepository.findById(id);
            
            if (applicationOpt.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy hồ sơ ứng tuyển");
            }

            Application application = applicationOpt.get();

            // Validate status
            List<String> allowedStatuses = Arrays.asList("applied", "viewed", "shortlisted", "rejected", "hired");
            if (!allowedStatuses.contains(status)) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ");
            }

            application.setStatus(status);
            applicationRepository.save(application);

            Map<String, String> flash = new HashMap<>();
            flash.put("type", "success");
            flash.put("message", "Cập nhật trạng thái thành công");
            session.setAttribute("admin_application_flash", flash);

        } catch (Exception e) {
            Map<String, String> flash = new HashMap<>();
            flash.put("type", "danger");
            flash.put("message", e.getMessage());
            session.setAttribute("admin_application_flash", flash);
        }

        return "redirect:/admin/applications/" + id;
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteApplication(
            @PathVariable Integer id,
            HttpSession session) {

        try {
            Optional<Application> applicationOpt = applicationRepository.findById(id);
            
            if (applicationOpt.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy hồ sơ ứng tuyển");
            }

            applicationRepository.deleteById(id);

            Map<String, String> flash = new HashMap<>();
            flash.put("type", "success");
            flash.put("message", "Xóa hồ sơ ứng tuyển thành công");
            session.setAttribute("admin_application_flash", flash);

        } catch (Exception e) {
            Map<String, String> flash = new HashMap<>();
            flash.put("type", "danger");
            flash.put("message", "Không thể xóa hồ sơ: " + e.getMessage());
            session.setAttribute("admin_application_flash", flash);
        }

        return "redirect:/admin/applications";
    }
}
